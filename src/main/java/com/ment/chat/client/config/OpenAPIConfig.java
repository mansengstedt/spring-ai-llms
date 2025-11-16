package com.ment.chat.client.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.core.jackson.ModelResolver;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenAPIConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Spring client for LLMs")
                        .version("1.0.0")
                        .description("REST API for client connected to different LLM providers")
                        .contact(new Contact()
                                .name("LLM Client Support")
                                .email("mans.engstedt@gmail.com")
                                .url("https://github.com/mansengstedt/spring-ai-llms/tree/f1")
                        )
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0.html")
                        )
                )
                .servers(List.of(
                        new Server()
                                .url("http://localhost:8999")
                                .description("Development server")
                )
                /*
                .components(new Components()
                        .addSecuritySchemes("bearerAuth", new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                        )
                )
                .addSecurityItem(new SecurityRequirement()
                        .addList("bearerAuth")*/
                );
    }

    /**
     * CRITICAL: This makes SpringDoc use Jackson's ObjectMapper
     * for generating Swagger schemas and examples correct, for example attributes with snake case
     */
    @Bean
    public ModelResolver modelResolver(ObjectMapper objectMapper) {
        return new ModelResolver(objectMapper);
    }
}
