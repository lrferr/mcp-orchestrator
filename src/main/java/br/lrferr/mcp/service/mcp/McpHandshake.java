package br.lrferr.mcp.service.mcp;

import java.io.IOException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Handles the MCP initialize handshake and capability negotiation.
 */
public final class McpHandshake {

    private McpHandshake() {
    }

    public static void perform(McpSession session, JsonNode clientCapabilities) throws IOException {
        ObjectMapper mapper = session.getObjectMapper();
        ObjectNode params = mapper.createObjectNode();
        params.set("capabilities", clientCapabilities == null ? mapper.createObjectNode() : clientCapabilities);

        JsonNode response = session.callMethodSync("initialize", params);
        if (response.has("error")) {
            throw new McpProtocolException("MCP initialize failed: " + response.get("error"));
        }

        session.callMethodSync("initialized", mapper.createObjectNode());
    }
}

