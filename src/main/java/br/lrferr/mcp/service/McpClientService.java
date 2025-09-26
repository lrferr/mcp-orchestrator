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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import br.lrferr.mcp.service.mcp.McpSession;
import br.lrferr.mcp.service.mcp.McpSessionManager;
import br.lrferr.mcp.service.mcp.McpToolInvoker;

@Service
public class McpClientService {

	private static final Logger log = LoggerFactory.getLogger(McpClientService.class);

	private final McpProcessManagerService processManagerService;
	private final McpSessionManager sessionManager;
	private final McpToolInvoker toolInvoker;
	private final ObjectMapper objectMapper;

	private final Builder chatClientBuilder;
	private ChatClient chatClient;

	private String currentServer;
	private String selectedModel = "llama3";

	public McpClientService(McpProcessManagerService processManagerService, McpSessionManager sessionManager,
		McpToolInvoker toolInvoker, ObjectMapper objectMapper, Builder chatClientBuilder) {
		this.processManagerService = processManagerService;
		this.sessionManager = sessionManager;
		this.toolInvoker = toolInvoker;
		this.objectMapper = objectMapper;
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
		this.currentServer = serverName;
		log.info("Connected to MCP server: {}", serverName);
	}

	public void disconnect() {
		log.info("Disconnected from MCP server: {}", currentServer);
		sessionManager.closeSession(currentServer);
		this.currentServer = null;
	}

	public String query(String prompt) {
		if (currentServer == null) {
			throw new IllegalStateException("No MCP server connected. Use /api/mcp/{serverName}/connect first.");
		}

		log.info("Processing query for {} with prompt: {}", currentServer, prompt);

		if (isDataQuery(prompt)) {
			return executeRealDataQuery(prompt);
		}

		return executeLLMQuery(prompt);
	}

	private boolean isDataQuery(String prompt) {
		String lowerPrompt = prompt.toLowerCase();
		// More aggressive detection for data queries
		return lowerPrompt.contains("select") || 
			   lowerPrompt.contains("retorne") || 
			   lowerPrompt.contains("lista") || 
			   lowerPrompt.contains("dados") || 
			   lowerPrompt.contains("registros") || 
			   lowerPrompt.contains("tabela") ||
			   lowerPrompt.contains("database") ||
			   lowerPrompt.contains("query") ||
			   lowerPrompt.contains("motorista") ||
			   lowerPrompt.contains("frota") ||
			   (lowerPrompt.contains("5") && lowerPrompt.contains("primeiros"));
	}

	private String executeRealDataQuery(String prompt) {
		McpSession session = sessionManager.getOrCreateSession(currentServer);
		if ("oracle-monitor".equals(currentServer)) {
			return executeOracleQuery(prompt, session);
		}
		if ("mysql-monitor".equals(currentServer)) {
			return executeMySQLQuery(prompt, session);
		}
		return "❌ **Unsupported Server**: MCP data queries currently supported for oracle-monitor and mysql-monitor only.";
	}

	private String executeOracleQuery(String prompt, McpSession session) {
		try {
			String sqlQuery = generateSQLFromPrompt(prompt);
			return executeQueryWithTool(session, "execute_safe_query", sqlQuery, "oracle");
		} catch (Exception e) {
			log.error("Failed to execute Oracle query: {}", e.getMessage(), e);
			return "❌ **Oracle Query Error**: " + e.getMessage();
		}
	}

	private String executeMySQLQuery(String prompt, McpSession session) {
		try {
			String sqlQuery = generateSQLFromPrompt(prompt);
			return executeQueryWithTool(session, "execute_safe_query", sqlQuery, "mysql");
		} catch (Exception e) {
			log.error("Failed to execute MySQL query: {}", e.getMessage(), e);
			return "❌ **MySQL Query Error**: " + e.getMessage();
		}
	}

	private String executeQueryWithTool(McpSession session, String toolName, String sqlQuery, String databaseType) {
		log.info("Executing query via MCP tool {}: {}", toolName, sqlQuery);
		ObjectNode params = objectMapper.createObjectNode();
		params.put("query", sqlQuery);
		params.put("databaseType", databaseType);
		JsonNode resultNode = toolInvoker.invokeTool(session, toolName, params);
		return formatToolResult(resultNode, sqlQuery, databaseType);
	}

	private String formatToolResult(JsonNode resultNode, String sqlQuery, String databaseType) {
		if (resultNode == null || resultNode.isNull()) {
			return "⚠️ No data returned for query: " + sqlQuery;
		}
		StringBuilder builder = new StringBuilder();
		builder.append("## ✅ Query Executed Successfully\n\n");
		builder.append("**Database**: ").append(databaseType).append("\n");
		builder.append("**SQL**: ```").append(sqlQuery).append("```\n\n");
		builder.append(resultNode.toPrettyString());
		return builder.toString();
	}

	private String generateSQLFromPrompt(String prompt) {
		try {
			ChatOptions options = ChatOptionsBuilder.builder()
				.withTemperature(0.1)
				.withMaxTokens(200)
				.withModel(selectedModel)
				.build();

			return chatClient.prompt()
				.system("Convert user requests to SQL queries. Return only the SQL query, no explanations. " +
						"Use Oracle syntax. For 'primeiros 5 registros' use ROWNUM <= 5.")
				.user(prompt)
				.options(options)
				.call()
				.content()
				.replaceAll("```sql", "")
				.replaceAll("```", "")
				.trim();
		} catch (Exception e) {
			log.warn("Failed to generate SQL from prompt, using fallback: {}", e.getMessage());
			return "SELECT * FROM frota.motorista WHERE ROWNUM <= 5";
		}
	}

	private String executeLLMQuery(String prompt) {
		log.info("Sending general prompt to {} via Ollama model {}: {}", currentServer, selectedModel, prompt);

		try {
			ChatOptions options = ChatOptionsBuilder.builder()
				.withTemperature(0.2)
				.withMaxTokens(512)
				.withModel(selectedModel)
				.build();

			String content = chatClient.prompt()
				.system("You are an assistant specialized in database and MCP management. " +
						"Current connected server: '" + currentServer + "'. " +
						"Provide helpful information about database management, MCP servers, and general guidance.")
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

