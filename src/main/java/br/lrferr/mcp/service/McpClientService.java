package br.lrferr.mcp.service;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class McpClientService {

	private static final Logger log = LoggerFactory.getLogger(McpClientService.class);

	private String currentServer;

	public Optional<String> getCurrentServer() {
		return Optional.ofNullable(currentServer);
	}

	public void connect(String serverName) {
		this.currentServer = serverName;
		log.info("Connected to MCP server: {}", serverName);
	}

	public void disconnect() {
		log.info("Disconnected from MCP server: {}", currentServer);
		this.currentServer = null;
	}

	public String query(String prompt) {
		if (currentServer == null) {
			throw new IllegalStateException("No MCP server connected. Use mcp-connect first.");
		}
		log.info("Sending prompt to {}: {}", currentServer, prompt);
		return "[stubbed-response]";
	}
}

