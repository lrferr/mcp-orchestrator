package br.lrferr.mcp.service;

import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ChatClient.Builder;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.ChatOptionsBuilder;
import org.springframework.stereotype.Service;

import br.lrferr.mcp.service.McpProcessManagerService.RunningServer;

@Service
public class McpClientService {

	private static final Logger log = LoggerFactory.getLogger(McpClientService.class);

	private final McpProcessManagerService processManagerService;

	private final ChatClient chatClient;

	private String currentServer;

	public McpClientService(McpProcessManagerService processManagerService, Builder chatClientBuilder) {
		this.processManagerService = processManagerService;
		this.chatClient = chatClientBuilder.build();
	}

	public Optional<String> getCurrentServer() {
		return Optional.ofNullable(currentServer);
	}

	public void connect(String serverName) {
		RunningServer server = processManagerService.getRunningServer(serverName);
		if (server == null) {
			throw new IllegalArgumentException("Server not running: " + serverName);
		}
		this.currentServer = serverName;
		log.info("Connected to MCP server: {}", serverName);
	}

	public void disconnect() {
		log.info("Disconnected from MCP server: {}", currentServer);
		this.currentServer = null;
	}

	public String query(String prompt) {
		if (currentServer == null) {
			throw new IllegalStateException("No MCP server connected. Use /api/mcp/{serverName}/connect first.");
		}

		log.info("Sending prompt to {} via Ollama: {}", currentServer, prompt);

		ChatOptions options = ChatOptionsBuilder.builder()
			.withTemperature(0.2)
			.withMaxTokens(512)
			.build();

		String content = chatClient.prompt()
			.system("You are an assistant specialized in database and MCP management. Keep answers brief.")
			.user(prompt)
			.options(options)
			.call()
			.content();

		log.debug("LLM response: {}", content);
		return content;
	}

	public Map<String, Object> getChatInfo() {
		return Map.of(
			"connectedServer", currentServer,
			"model", "ollama"
		);
	}
}

