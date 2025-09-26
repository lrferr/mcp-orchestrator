package br.lrferr.mcp.controller;

import java.nio.file.Path;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
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

	@GetMapping("/servers")
	public ResponseEntity<Map<String, McpServerConfig>> listServers(
		@RequestParam(value = "config", required = false) String configPath) {
		Path path = configPath != null ? Path.of(configPath) : configLoader.getDefaultPath();
		Map<String, McpServerConfig> servers = configLoader.loadConfiguration(path);
		return ResponseEntity.ok(servers);
	}

	@PostMapping("/servers/{name}/start")
	public ResponseEntity<Void> startServer(@PathVariable String name, @RequestBody McpServerConfig config,
		@RequestParam(value = "config", required = false) String configPath) {
		processManagerService.startServer(name, config,
				configPath != null ? Path.of(configPath) : configLoader.getDefaultPath());
		return ResponseEntity.accepted().build();
	}

	@PostMapping("/servers/{name}/stop")
	public ResponseEntity<Void> stopServer(@PathVariable String name) {
		processManagerService.stopServer(name);
		return ResponseEntity.accepted().build();
	}

	@GetMapping("/servers/running")
	public ResponseEntity<Map<String, RunningServer>> runningServers() {
		return ResponseEntity.ok(processManagerService.getRunningServers());
	}

	@PostMapping("/servers/{name}/connect")
	public ResponseEntity<Void> connect(@PathVariable String name) {
		RunningServer server = processManagerService.getRunningServer(name);
		if (server == null) {
			return ResponseEntity.notFound().build();
		}
		clientService.connect(name);
		return ResponseEntity.ok().build();
	}

	@PostMapping("/servers/query")
	public ResponseEntity<String> query(@RequestBody String prompt) {
		return ResponseEntity.ok(clientService.query(prompt));
	}
}

