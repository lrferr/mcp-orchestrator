# MCP Orchestrator API - Ollama Integration Endpoints

## Ollama Integration Module

The project includes specific endpoints for checking and listing available Ollama models.

> **📚 Complete Documentation:** See [API_ENDPOINTS.md](API_ENDPOINTS.md) for full API documentation.

### Endpoints Adicionados:

#### 1. **Listar Modelos do Ollama**
```
GET /api/mcp/ollama/models
```

**Exemplo de resposta:**
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

#### 2. **Obter Informações Detalhadas sobre um Modelo Específico**
```
GET /api/mcp/ollama/models/{modelName}
```

**Exemplo:**
```
GET /api/mcp/ollama/models/llama3:latest
```

**Exemplo de resposta:**
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
    "template": "{{ if .System }}<|start_header_id|>system<|end_header_id|>\n{{ .System }}<|eot_id|>",
    "parameters": "num_predict 50 temperature 0.4 top_k 40 top_p 0.9"
  }
}
```

#### 3. **Testar Conexão com Ollama**
```
GET /api/mcp/ollama/test-connection
```

**Exemplo de resposta (sucesso):**
```json
{
  "status": "SUCCESS",
  "baseUrl": "http://localhost:11434",
  "message": "Connection to Ollama successful"
}
```

**Exemplo de resposta (erro):**
```json
{
  "status": "ERROR",
  "baseUrl": "http://localhost:11434",
  "message": "Failed to connect to Ollama: Connection refused"
}
```

## Como Usar no Swagger UI

1. **Acesse o Swagger UI**: `http://localhost:8080/swagger-ui.html`
2. **Navegue até a seção "McpApi"**
3. **Encontre os endpoints "ollama" para interação com modelos**
4. **Use o botão "Try it out" para testar os endpoints**

## Configuração

O Ollama application properties já está configurado no `application.properties`:

```properties
# Configuração do Ollama
spring.ai.ollama.base-url=http://localhost:11434/
spring.ai.ollama.chat.model=llama3
```

## Arquitetura

O novo módulo inclui:

1. **`OllamaService.java`** - Serviço para interação com API do Ollama
2. **Endpoints atualizados no `McpServerController.java`** para integração com Ollama
3. **Tratamento de exceções completo** para cenários offline/disconectado do Ollama
4. **Mapeamento detalhado** de informações dos modelos (tamanho, formato, família, etc.)

## Benefícios

- ✅ **Verificação rápida** dos modelos disponíveis via API
- ✅ **Informações detalhadas** sobre cada modelo
- ✅ **Monitoramento da conexão** com o servidor Ollama
- ✅ **Integração direta** com a API REST existente no Swagger UI
- ✅ **Tratamento de erros robusto** quando Ollama não está disponível
