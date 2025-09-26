# MCP Orchestrator API - Complete Endpoints Documentation

## Overview

REST API for managing Model Context Protocol (MCP) servers and Ollama integration. This API provides endpoints to start, stop, connect to MCP servers, query the connected LLM through Ollama, and manage local language models.

## Base URL

```
http://localhost:8080/api/mcp
```

## Authentication

No authentication required for current implementation.

---

## MCP Server Management Endpoints

### 1. **Start MCP Servers**

**Endpoint:** `POST /api/mcp/start`

**Description:** Starts all configured MCP servers from JSON configuration file.

**Query Parameters:**
- `file` (optional): Path to custom JSON configuration file

**Request Example:**
```bash
POST /api/mcp/start
POST /api/mcp/start?file=./custom-config.json
```

**Response Example:**
```json
{
  "message": "Started 2 servers",
  "servers": ["oracle-monitor", "mysql-monitor"],
  "source": "/path/to/mcp.json"
}
```

---

### 2. **List All Servers**

**Endpoint:** `GET /api/mcp/list`

**Description:** Lists all configured servers and their current status (RUNNING/STOPPED).

**Response Example:**
```json
{
  "total": 2,
  "servers": [
    {
      "name": "oracle-monitor",
      "config": {
        "command": "node",
        "args": ["./servers/oracle-monitor/index.js"],
        "env": {
          "NODE_ENV": "production"
        },
        "workingDirectory": "./"
      },
      "status": "RUNNING",
      "pid": 15496
    },
    {
      "name": "mysql-monitor",
      "config": {
        "command": "node",
        "args": ["./servers/mysql-monitor/index.js"],
        "env": {
          "NODE_ENV": "production"
        },
        "workingDirectory": "./"
      },
      "status": "STOPPED",
      "pid": null
    }
  ]
}
```

---

### 3. **Stop Server**

**Endpoint:** `DELETE /api/mcp/{serverName}`

**Description:** Stops a specific MCP server.

**Path Parameters:**
- `serverName`: Name of the server to stop

**Request Example:**
```bash
DELETE /api/mcp/oracle-monitor
```

**Response Example:**
```json
{
  "message": "Stopped server oracle-monitor",
  "serverName": "oracle-monitor"
}
```

---

### 4. **Connect to Server**

**Endpoint:** `POST /api/mcp/{serverName}/connect`

**Description:** Connects to a running MCP server for interaction.

**Path Parameters:**
- `serverName`: Name of the server to connect to

**Request Example:**
```bash
POST /api/mcp/oracle-monitor/connect
```

**Response Example:**
```json
{
  "message": "Connected to oracle-monitor",
  "serverName": "oracle-monitor",
  "pid": 15496
}
```

---

### 5. **Query Server**

**Endpoint:** `POST /api/mcp/query`

**Description:** Sends a prompt to the currently connected MCP server. For data-related prompts the orchestrator routes the request through the MCP protocol, calling the server tools (e.g., `execute_safe_query`) to return real database results. For general prompts the request is answered via the configured Ollama model.

**Query Parameters:**
- `prompt`: Text prompt to send to the LLM

**Request Example:**
```bash
POST /api/mcp/query?prompt=Analyze Oracle database performance
```

**Response Example:**
```json
{
  "prompt": "retorne os 5 primeiros registros da tabela frota.motorista",
  "response": "## ✅ Query Executed Successfully\n\n**Database**: oracle\n**SQL**: ```SELECT * FROM frota.motorista WHERE ROWNUM <= 5```\n\n{\n  \"rows\": [\n    { \"ID_MOTORISTA\": 174, \"APELIDO\": \"Kassab\" },\n    { \"ID_MOTORISTA\": 175, \"APELIDO\": \"Silva\" }\n  ],\n  \"metadata\": { \"count\": 2 }\n}"
}
```

---

## Ollama Integration Endpoints

### 6. **List Available Models**

**Endpoint:** `GET /api/mcp/ollama/models`

**Description:** Lists all available language models in the local Ollama installation.

**Response Example:**
```json
{
  "total": 3,
  "models": [
    {
      "name": "llama3:latest",
      "size": 4808364084,
      "modifiedAt": "2024-01-15T10:30:00.000Z",
      "format": "gguf",
      "family": "llama3",
      "parameterSize": "8B",
      "quantizationLevel": "Q4_0"
    },
    {
      "name": "mistral:7b",
      "size": 4214570084,
      "modifiedAt": "2024-01-10T08:45:00.000Z",
      "format": "gguf",
      "family": "mistral",
      "parameterSize": "7B",
      "quantizationLevel": "Q4_0"
    }
  ]
}
```

---

### 7. **Get Model Details**

**Endpoint:** `GET /api/mcp/ollama/models/{modelName}`

**Description:** Retrieves detailed information about a specific model.

**Path Parameters:**
- `modelName`: Name of the model to query

**Request Example:**
```bash
GET /api/mcp/ollama/models/llama3:latest
```

**Response Example:**
```json
{
  "model": {
    "name": "llama3:latest",
    "size": 4808364084,
    "modifiedAt": "2024-01-15T10:30:00.000Z",
    "format": "gguf",
    "family": "llama3",
    "parameterSize": "8B",
    "quantizationLevel": "Q4_0",
    "template": "{{ if .System }}<|start_header_id|>system<|end_header_id|>\\n{{ .System }}<|eot_id|>",
    "parameters": "num_predict 50 temperature 0.4 top_k 40 top_p 0.9"
  }
}
```

---

### 8. **Test Ollama Connection**

**Endpoint:** `GET /api/mcp/ollama/test-connection`

**Description:** Tests connection to the Ollama server and verifies availability.

**Response Example (Success):**
```json
{
  "status": "SUCCESS",
  "baseUrl": "http://localhost:11434",
  "message": "Connection to Ollama successful"
}
```

**Response Example (Error):**
```json
{
  "status": "ERROR",
  "baseUrl": "http://localhost:11434",
  "message": "Failed to connect to Ollama: Connection refused"
}
```

---

### 9. **Select Model**

**Endpoint:** `POST /api/mcp/ollama/select-model`

**Description:** Choose which Ollama model to use for queries. Check available models via /ollama/models first.

**Query Parameters:**
- `modelName`: Model name to select (e.g. llama3:latest, mistral:7b)

**Request Example:**
```bash
POST /api/mcp/ollama/select-model?modelName=llama3:latest
```

**Response Example:**
```json
{
  "message": "Model selected successfully",
  "selectedModel": "llama3:latest",
  "connectedServer": "oracle-monitor"
}
```

---

### 10. **Get Current Configuration**

**Endpoint:** `GET /api/mcp/status`

**Description:** Shows currently connected server and selected Ollama model configuration.

**Response Example:**
```json
{
  "connectedServer": "oracle-monitor",
  "model": "llama3:latest",
  "type": "ollama"
}
```

**Response Example (No Connection):**
```json
{
  "connectedServer": null,
  "model": "llama3:latest",
  "type": "ollama"
}
```

---

## Error Handling

### Common Error Responses

**Server Not Running Error:**
```json
{
  "error": "Server not running: oracle-monitor"
}
```

**No Connection Error:**
```json
{
  "error": "No MCP server connected. Use /connect endpoint first."
}
```

**Failed to Fetch Models Error:**
```json
{
  "error": "Failed to fetch models: Connection refused"
}
```

### HTTP Status Codes

- `200 OK`: Successful operation
- `400 Bad Request`: Invalid request or server not running
- `500 Internal Server Error`: Server processing error or Ollama unavailable

---

## Configuration

### Application Properties

```properties
# MCP configuration
mcp.config.path=./mcp.json

# Ollama configuration
spring.ai.ollama.base-url=http://localhost:11434/
spring.ai.ollama.chat.model=llama3

# Server settings
server.port=8080
springdoc.swagger-ui.path=/swagger-ui.html
```

### Example MCP Configuration File (mcp.json)

```json
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
    },
    "mysql-monitor": {
      "command": "python",
      "args": ["./servers/mysql-monitor/app.py"],
      "env": {
        "PYTHON_ENV": "production"
      },
      "workingDirectory": "./"
    }
  }
}
```

---

## Swagger UI Integration

### Access Swagger Documentation

1. Start the application:
   ```bash
   java -jar target/mcp-orchestrator-0.0.1-SNAPSHOT.jar
   ```

2. Open Swagger UI in the browser:
   ```
   http://localhost:8080/swagger-ui.html
   ```

3. Explore and test all available endpoints interactively.

### OpenAPI JSON Specification

Access the raw OpenAPI specification:
```
http://localhost:8080/v3/api-docs
```

---

## Architecture

### Service Layer

- **McpConfigLoader**: Loads JSON configurations
- **McpProcessManagerService**: Manages server processes
- **McpClientService**: Handles connections and queries
- **OllamaService**: Ollama integration and model management

### Controller Layer

- **McpServerController**: Main REST endpoints for MCP operations and Ollama integration

---

## Usage Examples

### Complete Workflow

1. **Test Ollama Connection:**
   ```bash
   GET /api/mcp/ollama/test-connection
   ```

2. **List Available Models:**
   ```bash
   GET /api/mcp/ollama/models
   ```

3. **Select a Specific Model:**
   ```bash
   POST /api/mcp/ollama/select-model?modelName=llama3:latest
   ```

4. **Start MCP Servers:**
   ```bash
   POST /api/mcp/start
   ```

5. **Connect to a Server:**
   ```bash
   POST /api/mcp/oracle-monitor/connect
   ```

6. **Check Current Configuration:**
   ```bash
   GET /api/mcp/status
   ```

7. **Query the Connected Server with Selected Model:**
   ```bash
   POST /api/mcp/query?prompt=Check database status
   ```

### Integration Examples

- Use with `curl`, Postman, or any API client
- Test through Swagger UI interface
- Build custom frontend applications
- Integrate with monitoring tools
