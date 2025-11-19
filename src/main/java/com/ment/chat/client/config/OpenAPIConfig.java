package com.ment.chat.client.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.core.jackson.ModelResolver;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@Configuration
public class OpenAPIConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Spring client for different LLM providers")
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

    @Bean
    public OpenApiCustomizer sortSchemasAlphabetically() {
        return openApi -> {
            if (openApi.getComponents() != null && openApi.getComponents().getSchemas() != null) {
                // Use TreeMap to sort by schema name
                openApi.getComponents().setSchemas(
                        new TreeMap<>(openApi.getComponents().getSchemas())
                );
            }
        };
    }

    record PathOp(String path,
                  PathItem.HttpMethod method,
                  Operation op) {}

    @Bean
    public OpenApiCustomizer sortOperations() {
        return openApi -> {
            if (openApi.getPaths() == null) return;

            // 1. Extract all operations into a sortable structure
            Map<String, PathItem> paths = openApi.getPaths();


            // Flatten: (/chat -> GET), (/chat -> POST) into list for sorting
            var opList = paths.entrySet().stream()
                    .flatMap(entry -> entry.getValue().readOperationsMap().entrySet().stream()
                            .map(e -> new PathOp(entry.getKey(), e.getKey(), e.getValue())))
                    .sorted(operationComparator())  // our custom comparator
                    .toList();

            // 2. Rebuild sorted Paths structure in Swagger order
            LinkedHashMap<String, PathItem> sortedPaths = new LinkedHashMap<>();

            for (PathOp po : opList) {
                PathItem pathItem = sortedPaths.computeIfAbsent(po.path, k -> new PathItem());
                pathItem.operation(po.method, po.op);
            }

            openApi.setPaths(new io.swagger.v3.oas.models.Paths());
            openApi.getPaths().putAll(sortedPaths);
        };
    }

    // Comparator: first by POST > GET > others, second by path alphabetically
    private Comparator<PathOp> operationComparator() {
        return Comparator
                .comparing((PathOp o) -> httpMethodRank(o.method))   // 1. operation type priority
                .thenComparing(o -> o.path);                    // 2. alphabetical path
    }

    // Defines the priority of HTTP methods
    private int httpMethodRank(PathItem.HttpMethod method) {
        return switch (method) {
            case POST -> 0;
            case GET -> 1;
            case PUT -> 2;
            case DELETE -> 3;
            default -> 4;
        };
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
