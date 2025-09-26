package br.lrferr.mcp.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI mcpOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("MCP Host API")
                        .description("CLI-first MCP orchestration API. Future REST frontend will also use these endpoints.")
                        .version("v0.1.0")
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0.html")))
                .externalDocs(new ExternalDocumentation()
                        .description("Project README")
                        .url("https://github.com/ufpr/mcp_host_java"));
    }
}
