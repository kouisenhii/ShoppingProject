package com.tw.shopping.main.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI shoppingOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Shopping Mall API 文件") // 標題
                        .description("這是我的購物商城專案 API 測試文件") // 描述
                        .version("v1.0.0") // 版本
                        .license(new License().name("Apache 2.0").url("http://springdoc.org")));
                        
                        // http://localhost:8080/swagger-ui/index.html
    }
}