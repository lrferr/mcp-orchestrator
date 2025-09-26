package br.lrferr.mcp.config;

import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;

@Configuration
public class OpenApiConfig {

	public io.swagger.v3.oas.models.OpenAPI customOpenAPI() {
		return new OpenAPI()
			.info(new Info()
				.title("MCP Orchestrator API")
				.description("RESTful API para gerenciar Model Context Protocol (MCP) servers")
				.version("0.0.1")
				.contact(new Contact()
					.name("MCP Orchestrator")
					.email("contato@lrferr.com"))
				.license(new License()
					.name("MIT License")
					.url("https://opensource.org/licenses/MIT")))
			.addServersItem(new Server()
				.url("http://localhost:8080")
				.description("Servidor de desenvolvimento"));
	}
}
