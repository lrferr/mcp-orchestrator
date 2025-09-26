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

## REST API + Swagger UI

### Iniciar a aplicação:
```bash
java -jar target/mcp-orchestrator-0.0.1-SNAPSHOT.jar
```

A aplicação estará disponível em:
- **API Base**: http://localhost:8080/api/mcp
- **Swagger UI**: http://localhost:8080/swagger-ui.html

### Documentation:
- **Complete API Reference**: [docs/API_ENDPOINTS.md](docs/API_ENDPOINTS.md) *(Recommended)*
- **Ollama Integration Examples**: [docs/API_EXAMPLES.md](docs/API_EXAMPLES.md)
- **Help**: [docs/HELP.md](docs/HELP.md)

### Endpoints Disponíveis:

#### `POST /api/mcp/start`
Inicia todos os servidores MCP carregando configuração do arquivo JSON.
- **Parâmetros opcionais**: `file` (caminho do arquivo JSON)
- **Resposta**: lista de servidores iniciados com status

#### `GET /api/mcp/list` 
Lista servidores conhecidos e seus estados.
- **Resposta**: servidores carregados com status RUNNING/STOPPED e PID

#### `DELETE /api/mcp/{serverName}`
Para um servidor específico.
- **Parâmetros**: `serverName` - nome do servidor
- **Resposta**: confirmação da parada

#### `POST /api/mcp/{serverName}/connect`
Conecta a um servidor rodando para interação.
- **Parâmetros**: `serverName` - nome do servidor
- **Resposta**: confirmação da conexão e PID

#### `POST /api/mcp/query`
Envia um prompt ao servidor conectado.
- **Parâmetros**: `prompt` - texto da consulta
- **Resposta**: resposta do servidor MCP via Ollama

#### Ollama Integration Endpoints:
- **`GET /api/mcp/ollama/models`** - Lista modelos disponíveis no Ollama
- **`GET /api/mcp/ollama/models/{modelName}`** - Detalhes de modelo específico
- **`GET /api/mcp/ollama/test-connection`** - Testa conexão com Ollama

*Ver `docs/API_EXAMPLES.md` para exemplos completos de uso*

---

**Swagger UI Web Interface**
- Documentação interativa e testes de API disponível em:
- **http://localhost:8080/swagger-ui.html**
- **OpenAPI JSON espectro em: http://localhost:8080/v3/api-docs**
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
