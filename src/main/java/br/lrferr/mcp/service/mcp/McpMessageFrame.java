package br.lrferr.mcp.service.mcp;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Handles Model Context Protocol message framing using Content-Length headers.
 */
public class McpMessageFrame {

    private static final String CONTENT_LENGTH = "Content-Length";
    private static final String HEADER_DELIMITER = "\r\n\r\n";

    private final ObjectMapper objectMapper;

    public McpMessageFrame(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Writes a JSON RPC message with proper MCP framing to the given writer.
     */
    public synchronized void write(Writer writer, JsonNode message) throws IOException {
        byte[] payload = objectMapper.writeValueAsBytes(message);
        writer.write(CONTENT_LENGTH + ": " + payload.length + "\r\n\r\n");
        writer.write(new String(payload, StandardCharsets.UTF_8));
        writer.flush();
    }

    /**
     * Reads a framed JSON RPC message from the given reader.
     */
    public synchronized JsonNode read(BufferedReader reader) throws IOException {
        StringBuilder headerBuilder = new StringBuilder();
        int ch;
        while ((ch = reader.read()) != -1) {
            headerBuilder.append((char) ch);
            if (headerBuilder.toString().endsWith(HEADER_DELIMITER)) {
                break;
            }
        }

        if (headerBuilder.length() == 0) {
            throw new McpProtocolException("Reached EOF before reading MCP headers");
        }

        String headers = headerBuilder.toString();
        int contentLength = parseContentLength(headers);

        char[] body = new char[contentLength];
        int read = 0;
        while (read < contentLength) {
            int result = reader.read(body, read, contentLength - read);
            if (result == -1) {
                throw new McpProtocolException("Unexpected EOF while reading MCP body");
            }
            read += result;
        }

        return objectMapper.readTree(new String(body));
    }

    private int parseContentLength(String headers) {
        for (String line : headers.split("\r\n")) {
            if (line.toLowerCase().startsWith(CONTENT_LENGTH.toLowerCase())) {
                String[] parts = line.split(":");
                if (parts.length != 2) {
                    throw new McpProtocolException("Invalid Content-Length header: " + line);
                }
                try {
                    return Integer.parseInt(parts[1].trim());
                }
                catch (NumberFormatException ex) {
                    throw new McpProtocolException("Invalid Content-Length value: " + line, ex);
                }
            }
        }
        throw new McpProtocolException("Missing Content-Length header in MCP message");
    }
}

