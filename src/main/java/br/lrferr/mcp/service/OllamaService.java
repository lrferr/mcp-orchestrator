package br.lrferr.mcp.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Service
public class OllamaService {
    
    private static final Logger log = LoggerFactory.getLogger(OllamaService.class);
    
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final String baseUrl;
    
    public OllamaService(@Value("${spring.ai.ollama.base-url:http://localhost:11434}") String baseUrl) {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }
    
    /**
     * List all available models in Ollama
     */
    public List<Map<String, Object>> listModels() {
        try {
            String url = baseUrl + "/api/tags";
            String response = restTemplate.getForObject(URI.create(url), String.class);
            
            JsonNode root = objectMapper.readTree(response);
            JsonNode models = root.get("models");
            
            if (models == null || models.isNull() || !models.isArray()) {
                return List.of();
            }
            
            return StreamSupport.stream(models.spliterator(), false)
                .map(modelNode -> {
                    Map<String, Object> modelInfo = new java.util.LinkedHashMap<>();
                    modelInfo.put("name", modelNode.get("name").asText());
                    modelInfo.put("size", modelNode.get("size").asLong());
                    modelInfo.put("modifiedAt", modelNode.get("modified_at").asText());
                    
                    if (modelNode.has("details")) {
                        JsonNode details = modelNode.get("details");
                        if (details.has("format")) {
                            modelInfo.put("format", details.get("format").asText());
                        }
                        if (details.has("family")) {
                            modelInfo.put("family", details.get("family").asText());
                        }
                        if (details.has("parameter_size")) {
                            modelInfo.put("parameterSize", details.get("parameter_size").asText());
                        }
                        if (details.has("quantization_level")) {
                            modelInfo.put("quantizationLevel", details.get("quantization_level").asText());
                        }
                    }
                    
                    return modelInfo;
                })
                .collect(Collectors.toList());
                
        } catch (Exception e) {
            log.error("Failed to list models from Ollama", e);
            throw new RuntimeException("Failed to fetch models from Ollama: " + e.getMessage(), e);
        }
    }
    
    /**
     * Get information about a specific model
     */
    public Map<String, Object> getModelInfo(String modelName) {
        try {
            String url = baseUrl + "/api/show";
            String requestBody = String.format("{\"name\":\"%s\"}", modelName);
            
            String response = restTemplate.postForObject(
                URI.create(url), 
                requestBody, 
                String.class
            );
            
            JsonNode root = objectMapper.readTree(response);
            
            Map<String, Object> modelDetails = new java.util.LinkedHashMap<>();
            modelDetails.put("name", root.get("name").asText());
            modelDetails.put("size", root.get("size").asLong());
            modelDetails.put("modifiedAt", root.get("modified_at").asText());
            
            JsonNode details = root.get("details");
            if (details != null && !details.isNull()) {
                modelDetails.put("format", details.get("format").asText());
                modelDetails.put("family", details.get("family").asText());
                modelDetails.put("parameterSize", details.get("parameter_size").asText());
                modelDetails.put("quantizationLevel", details.get("quantization_level").asText());
            }
            
            if (root.has("template")) {
                modelDetails.put("template", root.get("template").asText());
            }
            
            if (root.has("parameters")) {
                modelDetails.put("parameters", root.get("parameters").asText());
            }
            
            return modelDetails;
            
        } catch (Exception e) {
            log.error("Failed to get model info for: " + modelName, e);
            throw new RuntimeException("Failed to get model info: " + e.getMessage(), e);
        }
    }
    
    /**
     * Test connection to Ollama server
     */
    public Map<String, Object> testConnection() {
        try {
            String url = baseUrl + "/api/tags";
            restTemplate.getForObject(URI.create(url), String.class);
            
            return Map.of(
                "status", "SUCCESS",
                "baseUrl", baseUrl,
                "message", "Connection to Ollama successful"
            );
        } catch (Exception e) {
            log.error("Failed to connect to Ollama", e);
            return Map.of(
                "status", "ERROR",
                "baseUrl", baseUrl,
                "message", "Failed to connect to Ollama: " + e.getMessage()
            );
        }
    }
}
