package br.lrferr.mcp.shell;

import java.nio.file.Path;
import java.util.Map;

import org.springframework.shell.Availability;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellMethodAvailability;
import org.springframework.shell.standard.ShellOption;

import br.lrferr.mcp.model.McpServerConfig;
import br.lrferr.mcp.service.McpClientService;
import br.lrferr.mcp.service.McpConfigLoader;
import br.lrferr.mcp.service.McpProcessManagerService;
import br.lrferr.mcp.service.McpProcessManagerService.RunningServer;

@ShellComponent
public class McpShellCommands {

	private final McpConfigLoader configLoader;

	private final McpProcessManagerService processManagerService;

	private final McpClientService clientService;

	private Map<String, McpServerConfig> loadedServers;

	public McpShellCommands(McpConfigLoader configLoader, McpProcessManagerService processManagerService,
			McpClientService clientService) {
		this.configLoader = configLoader;
		this.processManagerService = processManagerService;
		this.clientService = clientService;
	}

	@ShellMethod(key = "mcp-start", value = "Start MCP servers defined in configuration file")
	public String startServers(@ShellOption(value = { "--file", "-f" }, help = "Configuration file path", defaultValue = ShellOption.NULL) String file) {
		Path path = file != null ? Path.of(file) : configLoader.getDefaultPath();
		loadedServers = configLoader.loadConfiguration(path);
		loadedServers.forEach((name, config) -> processManagerService.startServer(name, config, path));
		return "Started " + loadedServers.size() + " servers from " + path.toAbsolutePath();
	}

	@ShellMethod(key = "mcp-list", value = "List known MCP servers and running status")
	public String listServers() {
		StringBuilder sb = new StringBuilder();
		sb.append("Loaded servers:\n");
		if (loadedServers == null || loadedServers.isEmpty()) {
			sb.append("  [none loaded]\n");
		}
		else {
			loadedServers.forEach((name, config) -> {
				boolean running = processManagerService.getRunningServer(name) != null;
				sb.append("  ").append(name).append(" - ").append(running ? "ACTIVE" : "INACTIVE").append('\n');
			});
		}
		sb.append("Running servers:\n");
		processManagerService.getRunningServers().forEach((name, server) -> sb.append("  ").append(name).append(" (PID ")
				.append(server.process().pid()).append(")\n"));
		return sb.toString();
	}

	@ShellMethod(key = "mcp-stop", value = "Stop a running MCP server")
	public String stopServer(@ShellOption(help = "Server name") String name) {
		processManagerService.stopServer(name);
		return "Stopped server " + name;
	}

	@ShellMethod(key = "mcp-connect", value = "Connect to a running MCP server")
	public String connect(@ShellOption(help = "Server name") String name) {
		RunningServer server = processManagerService.getRunningServer(name);
		if (server == null) {
			throw new IllegalArgumentException("Server not running: " + name);
		}
		clientService.connect(name);
		return "Connected to " + name + " (PID " + server.process().pid() + ")";
	}

	@ShellMethod(key = { "query", "prompt" }, value = "Send prompt to connected MCP server")
	public String query(@ShellOption(help = "Prompt text") String prompt) {
		String response = clientService.query(prompt);
		return "Response: " + response;
	}

	@ShellMethodAvailability({ "mcp-stop", "mcp-connect" })
	public Availability serverRequiredAvailability() {
		return processManagerService.getRunningServers().isEmpty()
				? Availability.unavailable("No servers running. Use mcp-start first.")
				: Availability.available();
	}

	@ShellMethodAvailability("query")
	public Availability connectionRequiredAvailability() {
		return clientService.getCurrentServer().isPresent()
				? Availability.available()
				: Availability.unavailable("No server connected. Use mcp-connect first.");
	}
}

