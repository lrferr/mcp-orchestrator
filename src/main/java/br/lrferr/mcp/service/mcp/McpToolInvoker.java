package br.lrferr.mcp.service.mcp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

@Component
public class McpToolInvoker {

    private static final Logger log = LoggerFactory.getLogger(McpToolInvoker.class);

    private final ObjectMapper objectMapper;

    public McpToolInvoker(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public JsonNode invokeTool(McpSession session, String toolName, JsonNode arguments) {
        ObjectNode params = objectMapper.createObjectNode();
        params.put("name", toolName);
        params.set("arguments", arguments);

        JsonNode response = session.callMethodSync("tools/call", params);
        if (response.has("error")) {
            JsonNode error = response.get("error");
            String message = error.has("message") ? error.get("message").asText() : error.toString();
            throw new McpProtocolException("MCP tool call failed: " + message);
        }

        JsonNode result = response.get("result");
        log.debug("Tool {} returned: {}", toolName, result);
        return result;
    }
}

