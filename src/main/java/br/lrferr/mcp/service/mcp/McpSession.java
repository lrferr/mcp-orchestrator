package br.lrferr.mcp.service.mcp;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import br.lrferr.mcp.model.McpServerConfig;

/**
 * Represents a session with a running MCP server using JSON-RPC over stdio
 * transport.
 */
public class McpSession {

    private static final Logger log = LoggerFactory.getLogger(McpSession.class);

    private final String serverName;
    private final Process process;
    private final BufferedReader reader;
    private final BufferedWriter writer;
    private final ObjectMapper objectMapper;
    private final McpMessageFrame messageFrame;

    private final Map<String, CompletableFuture<JsonNode>> pendingRequests = new ConcurrentHashMap<>();

    private volatile Instant lastInteraction = Instant.now();

    public McpSession(String serverName, Process process, ObjectMapper objectMapper) {
        this.serverName = serverName;
        this.process = process;
        this.objectMapper = objectMapper;
        this.reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8));
        this.writer = new BufferedWriter(new OutputStreamWriter(process.getOutputStream(), StandardCharsets.UTF_8));
        this.messageFrame = new McpMessageFrame(objectMapper);

        startListenerThread();
    }

    private void startListenerThread() {
        Thread listener = new Thread(() -> {
            try {
                while (process.isAlive()) {
                    JsonNode message = messageFrame.read(reader);
                    handleIncomingMessage(message);
                }
            }
            catch (IOException ex) {
                log.warn("MCP session listener stopped for {}: {}", serverName, ex.getMessage());
            }
        });
        listener.setName("mcp-session-" + serverName);
        listener.setDaemon(true);
        listener.start();
    }

    private void handleIncomingMessage(JsonNode message) {
        lastInteraction = Instant.now();
        if (message.has("id")) {
            String id = message.get("id").asText();
            CompletableFuture<JsonNode> future = pendingRequests.remove(id);
            if (future != null) {
                future.complete(message);
                return;
            }
        }
        log.debug("Received MCP notification from {}: {}", serverName, message);
    }

    public CompletableFuture<JsonNode> callMethod(String method, JsonNode params) {
        String requestId = UUID.randomUUID().toString();
        ObjectNode request = objectMapper.createObjectNode();
        request.put("jsonrpc", "2.0");
        request.put("id", requestId);
        request.put("method", method);
        if (params != null) {
            request.set("params", params);
        }

        CompletableFuture<JsonNode> future = new CompletableFuture<>();
        pendingRequests.put(requestId, future);

        try {
            messageFrame.write(writer, request);
        }
        catch (IOException ex) {
            pendingRequests.remove(requestId);
            throw new McpProtocolException("Failed to send MCP request", ex);
        }

        return future;
    }

    public JsonNode callMethodSync(String method, JsonNode params) {
        try {
            JsonNode response = callMethod(method, params).join();
            if (response != null && response.has("error")) {
                throw new McpProtocolException("MCP method returned error: " + response.get("error"));
            }
            return response;
        }
        catch (Exception ex) {
            throw new McpProtocolException("MCP method call failed: " + method, ex);
        }
    }

    public Instant getLastInteraction() {
        return lastInteraction;
    }

    public boolean isAlive() {
        return process.isAlive();
    }

    public void close() {
        try {
            writer.close();
        }
        catch (IOException ignore) {
        }
        try {
            reader.close();
        }
        catch (IOException ignore) {
        }
        process.destroyForcibly();
    }

    ObjectMapper getObjectMapper() {
        return objectMapper;
    }
}

