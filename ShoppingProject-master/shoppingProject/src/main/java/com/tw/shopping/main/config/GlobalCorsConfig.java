package com.tw.shopping.main.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration // 標記這是一個配置類別
public class GlobalCorsConfig implements WebMvcConfigurer {

	@Override
    public void addCorsMappings(CorsRegistry registry) {
        // 設定允許跨域請求
        registry.addMapping("/**") // 1. 對全站所有 API 路徑生效
                // 2. 允許的來源：使用 allowedOriginPatterns("*") 比 allowedOrigins("*") 更靈活，
                //    它允許 localhost, ngrok 網址, 甚至是 file:// 開啟的 HTML
                .allowedOriginPatterns("*") 
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS") // 3. 允許的 HTTP 方法
                .allowedHeaders("*") // 4. 允許所有的 Header
                .allowCredentials(true); // 5. 允許攜帶 Cookie/Session (這點很重要，否則登入狀態會掉)
    }
}