package org.goden.svdemo.security.config;

import org.goden.svdemo.security.filter.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;  // 新增
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity   // 开启 @PreAuthorize 等注解支持
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
            // 设置会话管理为无状态（STATELESS）
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            // 配置异常处理委托：让 Spring Security 将权限异常直接抛出，由全局异常处理器捕获
            .exceptionHandling(exception -> exception
                    .accessDeniedHandler((request, response, accessDeniedException) -> {
                        // 直接抛出，交给 @RestControllerAdvice
                        throw accessDeniedException;
                    })
                    .authenticationEntryPoint((request, response, authException) -> {
                        // 认证异常也直接抛出
                        throw authException;
                    })
            )

            .authorizeHttpRequests(auth -> auth
                    // 配置无需认证的端点
                    .requestMatchers(
                            "/user/login",
                            "/user/refresh",
                            "/user/register")
                    .permitAll()
                    // 如果需要基于 URL 的权限控制，可以在这里配置（可选）
                    // .requestMatchers("/admin/**").hasRole("ADMIN")
                    .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}