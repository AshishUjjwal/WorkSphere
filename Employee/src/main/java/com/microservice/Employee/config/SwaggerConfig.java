package com.microservice.Employee.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI workSphereOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("WorkSphere Employee API")
                        .description("Enterprise REST API documentation for the WorkSphere Employee Management System. \nIncludes Endpoints for V1 (Async) and V2 (Kafka Event-Driven) flows.")
                        .version("v1.0"));
    }
}
