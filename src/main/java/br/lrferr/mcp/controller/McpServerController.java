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

@RestController
@RequestMapping("/api/mcp")
public class McpServerController {

	private final McpConfigLoader configLoader;
	private final McpProcessManagerService processManagerService;
	private final McpClientService clientService;

	public McpServerController(McpConfigLoader configLoader, McpProcessManagerService processManagerService,
			McpClientService clientService) {
		this.configLoader = configLoader;
		this.processManagerService = processManagerService;
		this.clientService = clientService;
	}

	@PostMapping("/start")
	public ResponseEntity<Map<String, Object>> startServers(@RequestParam(required = false) String file) {
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

	@DeleteMapping("/{serverName}")
	public ResponseEntity<Map<String, Object>> stopServer(@PathVariable String serverName) {
		processManagerService.stopServer(serverName);
		return ResponseEntity.ok(Map.of(
			"message", "Stopped server " + serverName,
			"serverName", serverName
		));
	}

	@PostMapping("/{serverName}/connect")
	public ResponseEntity<Map<String, Object>> connectToServer(@PathVariable String serverName) {
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

	@PostMapping("/query")
	public ResponseEntity<Map<String, Object>> queryServer(@RequestParam String prompt) {
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
}
