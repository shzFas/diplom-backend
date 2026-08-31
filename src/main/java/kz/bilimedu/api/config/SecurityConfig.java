package kz.bilimedu.api.config;

import kz.bilimedu.api.security.JwtAuthenticationFilter;
import kz.bilimedu.api.security.RestAccessDeniedHandler;
import kz.bilimedu.api.security.RestAuthenticationEntryPoint;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Отправная точка: в версии 2023 года сервер не авторизовал никого.
 * checkAuth проверял только подпись публично известным секретом, не различал
 * учителя и ученика и не висел на большинстве эндпоинтов вовсе.
 *
 * <p>Здесь всё наоборот: запрещено по умолчанию, разрешено списком.
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    /** Ровно четыре публичных эндпоинта (docs/permissions.md). Регистрации самозаписью нет. */
    private static final String[] PUBLIC_POST = {"/api/v1/auth/login", "/api/v1/auth/refresh"};
    private static final String[] PUBLIC_GET = {"/api/v1/health", "/api/v1/docs"};

    @Bean
    public PasswordEncoder passwordEncoder() {
        return Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   JwtAuthenticationFilter jwtFilter,
                                                   RestAuthenticationEntryPoint entryPoint,
                                                   RestAccessDeniedHandler accessDeniedHandler) throws Exception {
        return http
                // Сессий нет, токен передаётся заголовком — CSRF-токену нечего защищать.
                .csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.POST, PUBLIC_POST).permitAll()
                        .requestMatchers(HttpMethod.GET, PUBLIC_GET).permitAll()
                        .anyRequest().authenticated())
                .exceptionHandling(handling -> handling
                        .authenticationEntryPoint(entryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }
}
