# MCP Orchestrator (CLI-first)

CLI-first Spring Boot application that orchestrates Model Context Protocol (MCP) servers from JSON configuration.

## Overview

- Loads one or more JSON configs with MCP server definitions (`command`, `args`, `env`, `workingDirectory`).
- Starts each MCP server as an external process using `ProcessBuilder`.
- Tracks running servers, allows connecting and sending prompts (stubbed for now).
- Uses Spring Shell for CLI commands; services encapsulate logic for future REST API.

## Building

```bash
./mvnw -DskipTests package
```

## Configuration

Default config file path comes from `mcp.config.path` (see `application.properties`). Example `mcp.json`:
```
{
  "mcpServers": {
    "oracle-monitor": {
      "command": "node",
      "args": ["./servers/oracle-monitor/index.js"],
      "env": {
        "NODE_ENV": "production",
        "ORACLE_CONN": "oracle://user:pass@localhost:1521/xe"
      },
      "workingDirectory": "./"
    }
  }
}
```

## CLI Commands

- `mcp-start --file <config>` – load and start all servers from JSON (defaults to `mcp.config.path`).
- `mcp-list` – show loaded servers and running status (PID for running processes).
- `mcp-stop <name>` – stop a running server.
- `mcp-connect <name>` – select a running server for interaction.
- `query "<prompt>"` – send text to connected server (stubbed response for now).

## REST + Swagger UI

- REST endpoints live under `/api/mcp/**` (see `src/main/java/br/lrferr/mcp/controller/McpServerController`).
- Swagger UI available at `/swagger-ui.html`; OpenAPI JSON at `/v3/api-docs`.
- Start the application (`./mvnw spring-boot:run`) and open Swagger UI to explore/start/stop/list MCP servers.

## Architecture

```
br.lrferr.mcp
├── config
│   ├── McpConfigurationProperties – binds `mcp.config.path` property.
│   └── McpConfig – enables configuration properties.
├── model
│   ├── McpConfiguration – root JSON structure.
│   └── McpServerConfig – per-server config fields.
├── service
│   ├── McpConfigLoader – reads JSON via Jackson.
│   ├── McpProcessManagerService – starts/stops processes, tracks running servers.
│   └── McpClientService – manages current server connection (stub for MCP interactions).
└── shell
    └── McpShellCommands – Spring Shell CLI mapping to service layer.
```

## ProcessBuilder Example

```java
McpServerConfig config = new McpServerConfig();
config.setCommand("node");
config.setArgs(List.of("./servers/oracle-monitor/index.js"));
config.setEnv(Map.of(
    "NODE_ENV", "production",
    "ORACLE_CONN", "oracle://user:pass@localhost:1521/xe"
));

ProcessBuilder builder = new ProcessBuilder();
builder.command("node", "./servers/oracle-monitor/index.js");
builder.environment().putAll(config.getEnv());
```

## Next Steps

- Implement real MCP protocol client via Spring AI + MCP SDK.
- Expand REST controllers and keep OpenAPI docs in sync.
- Add automated tests (integration + CLI commands).
- Integrate Playwright or equivalent tests for future frontend.
