package br.lrferr.mcp.service;

import java.io.IOException;
import java.lang.ProcessBuilder.Redirect;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import br.lrferr.mcp.model.McpServerConfig;

@Service
public class McpProcessManagerService {

	private static final Logger log = LoggerFactory.getLogger(McpProcessManagerService.class);

	public record RunningServer(Process process, Instant startedAt, McpServerConfig config, Path configSource) {
}

	private final Map<String, RunningServer> runningServers = new ConcurrentHashMap<>();

	public synchronized void startServer(String name, McpServerConfig config, Path sourcePath) {
		if (runningServers.containsKey(name)) {
			throw new IllegalStateException("Server already running: " + name);
		}
		ProcessBuilder builder = createProcessBuilder(config);
		if (config.getEnv() != null) {
			config.getEnv().forEach(builder.environment()::put);
		}
		if (config.getWorkingDirectory() != null) {
			builder.directory(Path.of(config.getWorkingDirectory()).toFile());
		}
		try {
			Process process = builder.start();
			runningServers.put(name, new RunningServer(process, Instant.now(), config, sourcePath));
			log.info("Started MCP server '{}' with PID {}", name, process.pid());
		}
		catch (IOException ex) {
			throw new IllegalStateException("Failed to start MCP server: " + name, ex);
		}
	}

	public synchronized void stopServer(String name) {
		RunningServer server = runningServers.remove(name);
		if (server == null) {
			throw new IllegalArgumentException("Server not running: " + name);
		}
		server.process().destroy();
		log.info("Stopped MCP server '{}'", name);
	}

	public Map<String, RunningServer> getRunningServers() {
		return Collections.unmodifiableMap(runningServers);
	}

	public RunningServer getRunningServer(String name) {
		return runningServers.get(name);
	}

	public ProcessBuilder createProcessBuilder(McpServerConfig config) {
		if (config.getCommand() == null || config.getCommand().isBlank()) {
			throw new IllegalArgumentException("Command is required to start an MCP server");
		}
		ProcessBuilder builder = new ProcessBuilder();
		builder.command(buildCommand(config));
		if (config.getEnv() != null) {
			builder.environment().putAll(config.getEnv());
		}
		return builder;
	}

	private java.util.List<String> buildCommand(McpServerConfig config) {
		java.util.List<String> command = new java.util.ArrayList<>();
		command.add(config.getCommand());
		if (config.getArgs() != null) {
			command.addAll(config.getArgs());
		}
		return command;
	}

	public ProcessBuilder sampleProcessBuilder(McpServerConfig config) {
		ProcessBuilder builder = createProcessBuilder(config);
		if (config.getEnv() != null) {
			builder.environment().putAll(config.getEnv());
		}
		return builder;
	}
}

