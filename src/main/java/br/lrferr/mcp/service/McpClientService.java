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

	private final Builder chatClientBuilder;
	private ChatClient chatClient;

	private String currentServer;
	private String selectedModel = "llama3";

	public McpClientService(McpProcessManagerService processManagerService, Builder chatClientBuilder) {
		this.processManagerService = processManagerService;
		this.chatClientBuilder = chatClientBuilder;
		this.chatClient = chatClientBuilder.build();
	}

	public Optional<String> getCurrentServer() {
		return Optional.ofNullable(currentServer);
	}

	public String getSelectedModel() {
		return selectedModel;
	}

	public void setSelectedModel(String modelName) {
		if (modelName == null || modelName.isBlank()) {
			throw new IllegalArgumentException("Model name cannot be null or empty");
		}
		this.selectedModel = modelName;
		
		// Rebuild ChatClient to support dynamic model switching
		// Note: The ChatClient will use the current selectedModel variable for prompt calls
		this.chatClient = chatClientBuilder.build();
		
		log.info("Selected Ollama model: {}", modelName);
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

		log.info("Sending prompt to {} via Ollama model {}: {}", currentServer, selectedModel, prompt);

		try {
			ChatOptions options = ChatOptionsBuilder.builder()
				.withTemperature(0.2)
				.withMaxTokens(512)
				.withModel(selectedModel)
				.build();

			String content = chatClient.prompt()
				.system("You are an assistant specialized in database and MCP management. Keep answers brief.")
				.user(prompt)
				.options(options)
				.call()
				.content();

			log.debug("LLM response: {}", content);
			return content;
		} catch (org.springframework.web.client.UnknownContentTypeException e) {
			log.error("Ollama communication failed - unexpected content type: {}", e.getMessage(), e);
			return "❌ **Ollama Server Communication Error**: The model '" + selectedModel + 
				   "' returned unexpected format (text/plain instead of JSON). Please check: " +
				   "1) Ollama server is running 2) Model '" + selectedModel + "' is downloaded 3) " +
				   "Correct model name. Use GET /api/mcp/ollama/test-connection to verify Ollama status.";
		} catch (Exception e) {
			// Check if it's the specific UnknownContentTypeException propagated
			if (e.getCause() != null && e.getCause() instanceof org.springframework.web.client.UnknownContentTypeException) {
				log.error("Ollama communication failed (propagated): {}", e.getCause().getMessage(), e);
				return "❌ **Ollama Communication Issue**: Could not parse response format " +
					   "from model '" + selectedModel + "'. Check that the Ollama server is " +
					   "running and the model is loaded/generation completed first: '" + e.getCause().getMessage() + "'";
			}
			
			log.error("Failed to communicate with Ollama LLM {}: {}", selectedModel, e.getMessage(), e);
			return "❌ **OLLAMA ERROR**: Failed to process request: " + e.getMessage() + 
				   "\n\nPlease verify Ollama server status with GET /api/mcp/ollama/test-connection";
		}
	}

	public Map<String, Object> getChatInfo() {
		Map<String, Object> info = new java.util.LinkedHashMap<>();
		info.put("connectedServer", currentServer);
		info.put("model", selectedModel);
		info.put("type", "ollama");
		return info;
	}
}

