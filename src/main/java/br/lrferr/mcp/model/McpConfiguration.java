package br.lrferr.mcp.model;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class McpConfiguration {

	private Map<String, McpServerConfig> mcpServers;

	public Map<String, McpServerConfig> getMcpServers() {
		return mcpServers;
	}

	public void setMcpServers(Map<String, McpServerConfig> mcpServers) {
		this.mcpServers = mcpServers;
	}
}

