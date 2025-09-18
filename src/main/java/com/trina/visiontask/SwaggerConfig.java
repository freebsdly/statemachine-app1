package com.trina.visiontask;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {


    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info().title("Vision Task API").version("1.0"))
                .addSecurityItem(new SecurityRequirement().addList("sa-token"))
                .components(new Components()
                        .addSecuritySchemes("sa-token",
                                new SecurityScheme()
                                        .name("authorization")
                                        .type(SecurityScheme.Type.APIKEY)
                                        .in(SecurityScheme.In.HEADER)
                                        .description("输入Sa Token，格式: <token>")));
    }
}
