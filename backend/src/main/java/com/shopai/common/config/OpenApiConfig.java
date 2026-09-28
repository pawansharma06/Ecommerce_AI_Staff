package com.shopai.common.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Value("${app.version:1.0.0-SNAPSHOT}")
    private String appVersion;

    @Bean
    public OpenAPI shopAiOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("ShopAI API")
                        .description("ShopAI — Open-Source Shopify-Centric AI Agent Platform")
                        .version(appVersion)
                        .contact(new Contact()
                                .name("ShopAI"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0")))
                .servers(List.of(
                        new Server().url("/").description("Current server")
                ));
    }
}
