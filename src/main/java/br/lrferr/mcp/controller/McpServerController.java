package br.lrferr.mcp.controller;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import br.lrferr.mcp.model.McpServerConfig;
import br.lrferr.mcp.service.McpClientService;
import br.lrferr.mcp.service.McpConfigLoader;
import br.lrferr.mcp.service.McpProcessManagerService;
import br.lrferr.mcp.service.McpProcessManagerService.RunningServer;
import br.lrferr.mcp.service.OllamaService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "MCP & Ollama API", 
     description = "Manage MCP servers and query Ollama LLMs. Start servers → Connect → Send queries.")
@RestController
@RequestMapping("/api/mcp")
public class McpServerController {

	private final McpConfigLoader configLoader;
	private final McpProcessManagerService processManagerService;
	private final McpClientService clientService;
	private final OllamaService ollamaService;

	public McpServerController(McpConfigLoader configLoader, McpProcessManagerService processManagerService,
			McpClientService clientService, OllamaService ollamaService) {
		this.configLoader = configLoader;
		this.processManagerService = processManagerService;
		this.clientService = clientService;
		this.ollamaService = ollamaService;
	}

	@Operation(
		summary = "Start MCP Servers", 
		description = "Starts all configured MCP servers from JSON file. Optionally specify a custom config file path.")
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "Servers started successfully"),
		@ApiResponse(responseCode = "500", description = "Failed to start servers")
	})
	@PostMapping("/start")
	public ResponseEntity<Map<String, Object>> startServers(
			@Parameter(description = "Custom JSON config file path (optional)")
			@RequestParam(required = false) String file) {
		Path path = file != null ? Path.of(file) : configLoader.getDefaultPath();
		Map<String, McpServerConfig> servers = configLoader.loadConfiguration(path);
		servers.forEach((name, config) -> processManagerService.startServer(name, config, path));
		
		Map<String, Object> response = Map.of(
			"message", "Started " + servers.size() + " servers",
			"servers", servers.keySet(),
			"source", path.toAbsolutePath().toString()
		);
		return ResponseEntity.ok(response);
	}

	@Operation(
		summary = "List All Servers", 
		description = "Shows all configured MCP servers with their current status (RUNNING/STOPPED) and process IDs.")
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "Server list retrieved successfully")
	})
	@GetMapping("/list")
	public ResponseEntity<Map<String, Object>> listServers() {
		Map<String, McpServerConfig> loadedServers = configLoader.loadDefaultConfiguration();
		Map<String, RunningServer> runningServers = processManagerService.getRunningServers();

		List<Map<String, Object>> serverList = loadedServers.entrySet().stream()
			.map(entry -> {
				Map<String, Object> serverInfo = new java.util.LinkedHashMap<>();
				String name = entry.getKey();
				RunningServer running = runningServers.get(name);
				serverInfo.put("name", name);
				serverInfo.put("config", entry.getValue());
				serverInfo.put("status", running != null ? "RUNNING" : "STOPPED");
				serverInfo.put("pid", running != null ? running.process().pid() : null);
				return serverInfo;
			})
			.collect(Collectors.toList());

		return ResponseEntity.ok(Map.of(
			"servers", serverList,
			"total", serverList.size()
		));
	}

	@Operation(
		summary = "Stop Server", 
		description = "Stops a running MCP server and terminates its process.")
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "Server stopped successfully"),
		@ApiResponse(responseCode = "404", description = "Server not found")
	})
	@DeleteMapping("/{serverName}")
	public ResponseEntity<Map<String, Object>> stopServer(
			@Parameter(description = "Server name to stop")
			@PathVariable String serverName) {
		processManagerService.stopServer(serverName);
		return ResponseEntity.ok(Map.of(
			"message", "Stopped server " + serverName,
			"serverName", serverName
		));
	}

	@Operation(
		summary = "Connect to Server", 
		description = "Establishes connection to a running MCP server for sending queries. Required before using /query endpoint.")
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "Connected successfully"),
		@ApiResponse(responseCode = "400", description = "Server not running")
	})
	@PostMapping("/{serverName}/connect")
	public ResponseEntity<Map<String, Object>> connectToServer(
			@Parameter(description = "Server name to connect to")
			@PathVariable String serverName) {
		RunningServer server = processManagerService.getRunningServer(serverName);
		if (server == null) {
			return ResponseEntity.badRequest().body(Map.of(
				"error", "Server not running: " + serverName
			));
		}
		
		clientService.connect(serverName);
		return ResponseEntity.ok(Map.of(
			"message", "Connected to " + serverName,
			"serverName", serverName,
			"pid", server.process().pid()
		));
	}

	@Operation(
		summary = "Query Server", 
		description = "Send prompt to connected MCP server via Ollama LLM. Must connect to server first using /{serverName}/connect.")
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "Query executed successfully"),
		@ApiResponse(responseCode = "400", description = "No server connected")
	})
	@PostMapping("/query")
	public ResponseEntity<Map<String, Object>> queryServer(
			@Parameter(description = "Your question or prompt")
			@RequestParam String prompt) {
		if (!clientService.getCurrentServer().isPresent()) {
			return ResponseEntity.badRequest().body(Map.of(
				"error", "No server connected. Use /connect endpoint first."
			));
		}
		
		String response = clientService.query(prompt);
		return ResponseEntity.ok(Map.of(
			"response", response,
			"prompt", prompt
		));
	}

	// Ollama Endpoints
	@Operation(
		summary = "List Ollama Models", 
		description = "Shows all available language models in your local Ollama installation.")
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "Models list retrieved"),
		@ApiResponse(responseCode = "500", description = "Ollama not available")
	})
	@GetMapping("/ollama/models")
	public ResponseEntity<Map<String, Object>> listOllamaModels() {
		try {
			List<Map<String, Object>> models = ollamaService.listModels();
			return ResponseEntity.ok(Map.of(
				"models", models,
				"total", models.size()
			));
		} catch (Exception e) {
			return ResponseEntity.internalServerError().body(Map.of(
				"error", "Failed to fetch models: " + e.getMessage()
			));
		}
	}

	@Operation(
		summary = "Get Model Details", 
		description = "Retrieves detailed information about a specific Ollama model including size, format, and parameters.")
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "Model details retrieved"),
		@ApiResponse(responseCode = "500", description = "Model not found or Ollama error")
	})
	@GetMapping("/ollama/models/{modelName}")
	public ResponseEntity<Map<String, Object>> getOllamaModelInfo(
			@Parameter(description = "Model name (e.g. llama3:latest)")
			@PathVariable String modelName) {
		try {
			Map<String, Object> modelInfo = ollamaService.getModelInfo(modelName);
			return ResponseEntity.ok(Map.of(
				"model", modelInfo
			));
		} catch (Exception e) {
			return ResponseEntity.internalServerError().body(Map.of(
				"error", "Failed to get model info: " + e.getMessage()
			));
		}
	}

	@Operation(
		summary = "Test Ollama Connection", 
		description = "Verifies if Ollama server is running and accessible. Use this to check Ollama setup.")
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "Connection successful"),
		@ApiResponse(responseCode = "500", description = "Ollama not running or inaccessible")
	})
	@GetMapping("/ollama/test-connection")
	public ResponseEntity<Map<String, Object>> testOllamaConnection() {
		Map<String, Object> result = ollamaService.testConnection();
		boolean isSuccess = "SUCCESS".equals(result.get("status"));
		
		if (isSuccess) {
			return ResponseEntity.ok(result);
		} else {
			return ResponseEntity.internalServerError().body(result);
		}
	}
}
