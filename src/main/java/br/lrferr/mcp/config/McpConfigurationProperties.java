package br.lrferr.mcp.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "mcp.config")
public class McpConfigurationProperties {

	/**
	 * Path to the primary MCP configuration JSON file. This value can be overridden via
	 * the {@code mcp.config.path} property.
	 */
	private String path = "./mcp.json";

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}
}

