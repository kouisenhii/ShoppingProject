package com.tw.shopping.main.config;

import java.nio.charset.StandardCharsets;

import javax.crypto.spec.SecretKeySpec;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.oidc.authentication.OidcIdTokenDecoderFactory;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.BadJwtException; // 若是 Spring Security 6，請確認此 import 是否存在，若無可用 MacAlgorithm 或 JwsAlgorithm
import org.springframework.security.oauth2.jwt.JwtDecoderFactory;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.client.RestTemplate;
import org.springframework.boot.web.client.RestTemplateBuilder;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jwt.SignedJWT;
import com.tw.shopping.main.handler.OAuth3LoginSuccessHandler;
import com.tw.shopping.main.service.CustomOAuth2UserService;
import com.tw.shopping.main.service.OAuth2LoginSuccessHandler;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomOAuth2UserService customOAuth2UserService;
    private final OAuth2LoginSuccessHandler oauth2LoginSuccessHandler;
    private final OAuth3LoginSuccessHandler oauth3LoginSuccessHandler;
    
    
 // 1. 定義 SessionRegistry Bean：這是 Spring Security 用來追蹤所有活動 Session 的核心
    @Bean
    public SessionRegistry sessionRegistry() {
        // 使用 SessionRegistryImpl 是最簡單的標準實作
        return new SessionRegistryImpl();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, SessionRegistry sessionRegistry) throws Exception {

        http
            .csrf(csrf -> csrf.disable())
            .formLogin(form -> form.disable())
            .httpBasic(basic -> basic.disable())

         // 配置 Session 管理，將 SessionRegistry 連結到 Session 處理上
            .sessionManagement(session -> session
                // 必須啟用 concurrentSessionControl，即使您不需要限制多點登入，
                // 這樣 Spring Security 才會將 Session 資訊註冊到 SessionRegistry 中
                .sessionConcurrency(concurrency -> concurrency
                    .sessionRegistry(sessionRegistry)
                )
            )
            
            // ❌ 移除這行，不要手動加 Provider，讓 Spring 自動抓下面的 Decoder Factory
            // .authenticationProvider(oidcAuthProvider())

            .securityContext(context -> context
                    .requireExplicitSave(false)
            )

            .exceptionHandling(ex -> ex
                    .authenticationEntryPoint((request, response, authException) -> {
                        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                        response.setContentType("application/json;charset=UTF-8");
                        response.getWriter().write("""
                        {
                            "error": "Unauthorized",
                            "message": "請先登入"
                        }
                        """);
                    })
                 // 2. ⭐ [新增這裡] 權限不足 (403) -> 顯示「權限不足」
                    // 這是專門給「已經登入，但是想去他不該去的地方」的人看的
                    .accessDeniedHandler((request, response, accessDeniedException) -> {
                        response.setStatus(HttpServletResponse.SC_FORBIDDEN); // 403
                        response.setContentType("application/json;charset=UTF-8");
                        response.getWriter().write("""
                        {
                            "status": 403,
                            "error": "Forbidden",
                            "message": "您沒有權限執行此操作"
                        }
                        """);
                    })
            )

            .logout(logout -> logout
                    .logoutUrl("/api/logout")
                    .logoutSuccessHandler((request, response, authentication) -> {
                        response.setStatus(HttpServletResponse.SC_OK);
                        response.setContentType("application/json;charset=UTF-8");
                        response.getWriter().write("""
                        {
                            "message": "logout success"
                        }
                        """);
                    })
            )

            .oauth2Login(oauth2 -> oauth2
                    .loginPage("/html/login.html")
                    .userInfoEndpoint(userInfo -> userInfo
                            .userService(customOAuth2UserService)
                            .oidcUserService(oidcUserService())   // ⭐ 加入這行！給 LINE (OIDC) 用的
                    )
                    		.successHandler((request, response, authentication) -> {
                        
                        // 第一步：先執行 OAuth3 (補資料)
                        // 這樣 DTO 裡面就會有 userId 了
                        oauth3LoginSuccessHandler.onAuthenticationSuccess(request, response, authentication);
                        
                        // 第二步：再執行 OAuth2 (原本的業務邏輯 + 頁面跳轉)
                        // 它會負責 response.sendRedirect，結束請求
                        oauth2LoginSuccessHandler.onAuthenticationSuccess(request, response, authentication);
                    })
                    .failureHandler((req, res, ex) -> {
                        ex.printStackTrace();
                        System.err.println("OAuth2 Login Failed: " + ex.getMessage());
                        res.sendRedirect("/html/login.html?error=oauth_failure");
                    })
            )

            .oauth2Client(Customizer.withDefaults())

            .authorizeHttpRequests(auth -> auth
                    .requestMatchers(
                            "/",
                            "/index.html",
                            "/search.html",
                            "/search.css",
                            "/search.js",

                            "/components/**",
                            "/css/**",
                            "/html/**",
                            "/img/**",
                            "/js/**",
                            
                            //api
                            "/api/login",
                            "/api/register",
                            "/api/logout",
                            "/api/user/me",
                            "/api/forget/**",
                            "/api/register/**",
                            "/api/login/check-email",
                            "/api/auth/pending-info",
                            "/api/auth/bind-provider",
                            
                            "/api/products/search",
                            "/api/categories/main",
                            "/api/categories/main/**",
                            "/api/product/**",
                            
                            "/oauth2/**",
                            "/login/oauth2/**",

                            // 賴的api
//                            "/api/admin/categories",
//                            "/api/admin/stats/**",
//                            "/api/admin/orders/**",
//                            "/api/admin/products/**",
//                            "/api/admin/users/**",
                            "/api/cart/**",
                            "/home",
                            "/api/ecpay/**",
                            "/api/orders/**",
                            "/api/products/**",
                            "/auth/line",
                            "/favicon.ico",
                            "/error",
                            "/test/**",
                            "/v1/wish",
                            "/v1/wish/**",
                            "/v1/userinfos",
                            "/v1/userinfos/**",
                            "/v1/orders",
                            "/v1/orders/**"
                    ).permitAll()
                    
                    // 賴 新增的 12/6
                    //  [新增] 後台權限設定 
                    // 只有擁有 'ADMIN' 角色的人才能呼叫 /api/admin/**
                    // 這裡會自動對應資料庫裡的 "ROLE_ADMIN"
                    .requestMatchers("/dashboard.html","/api/admin/**").hasRole("ADMIN")

                    // 1. 放行 Swagger 的核心 JSON 資料
                    .requestMatchers("/v3/api-docs/**").permitAll()
                    // 2. 放行 Swagger UI 的靜態資源 (HTML, CSS, JS)
                    .requestMatchers("/swagger-ui/**").permitAll()
                    // 3. 放行 Swagger 的入口頁面
                    .requestMatchers("/swagger-ui.html").permitAll()

                    // .requestMatchers("/admin.html").hasRole("ADMIN") // 如果有後台頁面也可加

                    // 其他所有請求都需要登入
                    .anyRequest().authenticated()
            );

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

 // 請確認有導入這些套件
    // import com.nimbusds.jose.JWSAlgorithm;
    // import com.nimbusds.jwt.SignedJWT;
    // import javax.crypto.spec.SecretKeySpec;
    // import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
    // import java.nio.charset.StandardCharsets;
    // import org.springframework.security.oauth2.jwt.BadJwtException;

    @Bean
    public JwtDecoderFactory<ClientRegistration> idTokenDecoderFactory() {
        return clientRegistration -> {
            String registrationId = clientRegistration.getRegistrationId();
            
            // 針對 LINE 進行特殊處理
            if ("line".equals(registrationId)) {
                String jwkSetUri = clientRegistration.getProviderDetails().getJwkSetUri();
                String clientSecret = clientRegistration.getClientSecret();
                
                // 回傳一個自定義的 Decoder，先判斷演算法再決定怎麼解
                return token -> {
                    try {
                        // 1. 先解析 JWT 檔頭，看看 LINE 到底用了什麼演算法
                        SignedJWT signedJWT = SignedJWT.parse(token);
                        JWSAlgorithm alg = signedJWT.getHeader().getAlgorithm();
                        
                        System.out.println("🔍 LINE 回傳的 Token 演算法是: " + alg.getName());

                        if (JWSAlgorithm.ES256.equals(alg)) {
                            // 情況 A：是 ES256 -> 使用公鑰 (JWK) 驗證
                            System.out.println("✅ 偵測到 ES256，使用 JWK Set 公鑰驗證");
                            return NimbusJwtDecoder.withJwkSetUri(jwkSetUri)
                                    .jwsAlgorithm(SignatureAlgorithm.ES256)
                                    .build()
                                    .decode(token);
                                    
                        } else if (JWSAlgorithm.HS256.equals(alg)) {
                            // 情況 B：是 HS256 -> 使用 Client Secret 驗證
                            System.out.println("✅ 偵測到 HS256，使用 Client Secret 驗證");
                            SecretKeySpec secretKey = new SecretKeySpec(
                                    clientSecret.getBytes(StandardCharsets.UTF_8), 
                                    "HmacSHA256"
                            );
                            return NimbusJwtDecoder.withSecretKey(secretKey)
                                    .macAlgorithm(MacAlgorithm.HS256)
                                    .build()
                                    .decode(token);
                        } else {
                            // 情況 C：其他怪異演算法
                            System.err.println("❌ 不支援的演算法: " + alg.getName());
                            throw new BadJwtException("Unsupported algorithm: " + alg.getName());
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        throw new BadJwtException("LINE ID Token 解碼失敗", e);
                    }
                };
            }

            // 其他 Provider (如 Google) 維持預設行為
            return new OidcIdTokenDecoderFactory().createDecoder(clientRegistration);
        };
    }
    
    /**
     * 自定義 OIDC Service
     * 修正版：針對 LINE，直接使用 ID Token 建立使用者，跳過 UserInfo Endpoint 避免欄位衝突
     */
    @Bean
    public OAuth2UserService<OidcUserRequest, OidcUser> oidcUserService() {
        final OidcUserService delegate = new OidcUserService();

        return (userRequest) -> {
            String registrationId = userRequest.getClientRegistration().getRegistrationId();

            // ⭐ 針對 LINE 進行特殊處理
            if ("line".equals(registrationId)) {
                System.out.println("⚡ 處理 LINE 登入：完全跳過 UserInfo 請求，直接解析 ID Token");
                
                // 1. 取得 ID Token (這是我們在 decoder 解碼成功的)
                var idToken = userRequest.getIdToken();
                
                // 2. 建立權限集合 (Spring Security 需要)
                var authorities = java.util.Collections.singleton(
                    new org.springframework.security.oauth2.core.oidc.user.OidcUserAuthority(idToken, null)
                );

                // 3. 直接回傳 DefaultOidcUser
                // 這裡指定 "sub" 為主鍵，因為 LINE 的 ID Token 裡一定有 sub (代表使用者 ID)
                return new DefaultOidcUser(
                        authorities,
                        idToken,
                        "sub"
                );
            }

            // ⭐ 其他 Provider (如 Google) 維持預設行為，讓 Spring 自己去處理
            return delegate.loadUser(userRequest);
        };
    }
    
    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder.build();
    }
}