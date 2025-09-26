package br.lrferr.mcp.service.mcp;

/**
 * Exception thrown when the MCP protocol negotiation or message exchange fails.
 */
public class McpProtocolException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public McpProtocolException(String message) {
        super(message);
    }

    public McpProtocolException(String message, Throwable cause) {
        super(message, cause);
    }
}

