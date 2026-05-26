package com.maritel.trustay.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;

@Configuration
//@OpenAPIDefinition(
//        info = @Info(title = "My API", version = "1.0", description = "API documentation with JWT"),
//        security = @SecurityRequirement(name = "bearerAuth")
//)
//@SecurityScheme(
//        name = "bearerAuth",
//        type = SecuritySchemeType.HTTP,
//        scheme = "bearer",
//        bearerFormat = "JWT"
//)
public class SwaggerConfig {

    @Value("${spring.application.name}")
    private String appName;

    private String host = "";

    static {
        io.swagger.v3.core.jackson.ModelResolver.enumsAsRef = true;
    }

    @Bean
    public GroupedOpenApi publicApi() {
        String[] paths = {"/**"};
        return GroupedOpenApi.builder()
                .group("api")
                .pathsToMatch(paths)
                .build();
    }

    @Bean
    public OpenAPI commonOpenAPI() {
        Info info = new Info().title("TRUSTAY Project")
                .description("TRUSTAY API Document")
                .version("v1");

        Parameter parameter = new Parameter();
        parameter.in("header");
        parameter.description("Language Code");
        parameter.example("ko_KR");

        SecurityScheme securityScheme = new SecurityScheme()
                .type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")
                .in(SecurityScheme.In.HEADER).name("Authorization");
        SecurityRequirement schemaRequirement = new SecurityRequirement().addList("bearerAuth");

        Components components = new Components();
        components.addSecuritySchemes("bearerAuth", securityScheme);
        components.addParameters("Accept-Language", parameter);

        return new OpenAPI()
                .components(components)
                .security(Arrays.asList(schemaRequirement))
                .addServersItem(new Server().url("/"))
                .info(new Info().title(String.format("%s Application API", appName)).description(
                        String.format("", appName)));
    }





}
