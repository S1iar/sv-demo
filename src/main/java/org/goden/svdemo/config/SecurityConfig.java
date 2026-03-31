package org.goden.svdemo.config;

import org.goden.svdemo.filter.JwtAuthenticationFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
            // 禁用CSRF（因为通常API项目使用无状态认证如JWT）
        http.csrf(AbstractHttpConfigurer::disable)
                // 设置会话管理为无状态（STATELESS）
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // 配置无需认证的端点 (对应原WebConfig中的excludePathPatterns)
                        .requestMatchers(
                                "/user/login",
                                "/user/register").permitAll()
                        // 1. 基于角色的访问控制 (使用 hasRole, 框架会自动匹配"ROLE_"前缀)
                        .requestMatchers("/admin/**").hasRole("ADMIN") // 等价于 hasAuthority("ROLE_ADMIN")
                        .requestMatchers("/user/profile").hasAnyRole("ADMIN", "USER")

//                        // 2. 基于权限（Authority）的访问控制 (更细粒度)
//                        .requestMatchers(HttpMethod.POST, "/api/products").hasAuthority("product:create")
//                        .requestMatchers(HttpMethod.DELETE, "/api/orders/*").hasAuthority("order:delete")
//                        .requestMatchers(HttpMethod.GET, "/api/statistics").hasAuthority("stat:read")

                        // 其他所有请求都需要认证
                        .anyRequest().authenticated()
                )
                // 在UsernamePasswordAuthenticationFilter之前添加我们的JWT过滤器
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}