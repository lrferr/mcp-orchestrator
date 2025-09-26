package br.lrferr.mcp.service.mcp;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import br.lrferr.mcp.model.McpServerConfig;
import br.lrferr.mcp.service.McpProcessManagerService;
import br.lrferr.mcp.service.McpProcessManagerService.RunningServer;

/**
 * Maintains active MCP sessions per server, reusing processes started by the
 * process manager.
 */
@Component
public class McpSessionManager {

    private static final Logger log = LoggerFactory.getLogger(McpSessionManager.class);

    private final McpProcessManagerService processManagerService;
    private final ObjectMapper objectMapper;

    private final Map<String, McpSession> activeSessions = new ConcurrentHashMap<>();

    public McpSessionManager(McpProcessManagerService processManagerService, ObjectMapper objectMapper) {
        this.processManagerService = processManagerService;
        this.objectMapper = objectMapper;
    }

    public McpSession getOrCreateSession(String serverName) {
        return activeSessions.compute(serverName, (name, existing) -> {
            if (existing != null && existing.isAlive()) {
                return existing;
            }
            return createSession(name);
        });
    }

    public void closeSession(String serverName) {
        McpSession session = activeSessions.remove(serverName);
        if (session != null) {
            log.info("Closing MCP session for {}", serverName);
            session.close();
        }
    }

    private McpSession createSession(String serverName) {
        RunningServer running = processManagerService.getRunningServer(serverName);
        if (running == null) {
            throw new IllegalStateException("MCP server not running: " + serverName);
        }
        Process process = running.process();
        log.info("Creating MCP session for {} (PID {})", serverName, process.pid());
        McpSession session = new McpSession(serverName, process, objectMapper);
        performHandshake(session, running.config());
        return session;
    }

    private void performHandshake(McpSession session, McpServerConfig config) {
        try {
            McpHandshake.perform(session, config.getCapabilities());
        }
        catch (IOException ex) {
            throw new McpProtocolException("Failed MCP handshake", ex);
        }
    }

    @Scheduled(fixedDelay = 30_000)
    public void cleanupInactiveSessions() {
        Instant threshold = Instant.now().minus(Duration.ofMinutes(5));
        activeSessions.entrySet().removeIf(entry -> {
            McpSession session = entry.getValue();
            if (!session.isAlive() || session.getLastInteraction().isBefore(threshold)) {
                log.info("Closing idle MCP session for {}", entry.getKey());
                session.close();
                return true;
            }
            return false;
        });
    }
}

