package br.lrferr.mcp.service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

import br.lrferr.mcp.config.McpConfigurationProperties;
import br.lrferr.mcp.model.McpConfiguration;
import br.lrferr.mcp.model.McpServerConfig;

@Service
public class McpConfigLoader {

	private final ObjectMapper objectMapper;

	private final McpConfigurationProperties properties;

	public McpConfigLoader(ObjectMapper objectMapper, McpConfigurationProperties properties) {
		this.objectMapper = objectMapper;
		this.properties = properties;
	}

	public Map<String, McpServerConfig> loadDefaultConfiguration() {
		Path defaultPath = getDefaultPath();
		return loadConfiguration(defaultPath);
	}

	public Path getDefaultPath() {
		return Path.of(properties.getPath());
	}

	public Map<String, McpServerConfig> loadConfiguration(Path path) {
		Objects.requireNonNull(path, "path is required");
		if (!Files.exists(path)) {
			throw new IllegalArgumentException("Configuration file not found: " + path.toAbsolutePath());
		}
		try (InputStream in = Files.newInputStream(path)) {
			McpConfiguration configuration = objectMapper.readValue(in, McpConfiguration.class);
			Map<String, McpServerConfig> servers = configuration.getMcpServers();
			return servers == null ? Map.of() : new LinkedHashMap<>(servers);
		}
		catch (IOException ex) {
			throw new IllegalArgumentException("Failed to read configuration from " + path.toAbsolutePath(), ex);
		}
	}

	public Map<String, McpServerConfig> loadAndMerge(List<Path> paths) {
		Map<String, McpServerConfig> merged = new LinkedHashMap<>();
		for (Path path : paths) {
			merged.putAll(loadConfiguration(path));
		}
		return merged;
	}
}

