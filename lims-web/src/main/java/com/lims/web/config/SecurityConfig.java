package com.lims.web.config;

import com.lims.common.security.JwtTokenProvider;
import com.lims.web.security.DevAuthFilter;
import com.lims.web.security.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity   // applies in BOTH dev and prod profiles — was previously
                       // scoped to a separate @Profile("!dev") inner class, which
                       // silently disabled every @PreAuthorize on the controllers
                       // in dev (engineer could approve / reject IN_REVIEW
                       // reports). prePostEnabled defaults to true.
public class SecurityConfig {

    /**
     * Dev profile：所有接口放行，但仍注入一个虚拟 ADMIN 用户，
     * 这样依赖 SecurityUtils.getCurrentUserId() 的代码（数据权限、审计日志、报告创建等）能拿到稳定的 userId。
     */
    @Bean
    @Profile("dev")
    public SecurityFilterChain devFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .addFilterBefore(new DevAuthFilter(), UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    /**
     * Prod profile：JWT 资源服务器模式，自签 token。
     */
    @Bean
    @Profile("!dev")
    public SecurityFilterChain prodFilterChain(HttpSecurity http,
                                               JwtTokenProvider jwtTokenProvider) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/v1/auth/**").permitAll()
                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                .anyRequest().denyAll()
            )
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            .httpBasic(basic -> basic.disable())
            .formLogin(form -> form.disable())
            .addFilterBefore(new JwtAuthenticationFilter(jwtTokenProvider),
                    UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
