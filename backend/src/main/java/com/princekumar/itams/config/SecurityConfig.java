package com.princekumar.itams.config;

import com.princekumar.itams.auth.AuthRateLimitFilter;
import com.princekumar.itams.auth.JwtAuthenticationFilter;
import com.princekumar.itams.auth.JwtProperties;
import com.princekumar.itams.auth.RestAccessDeniedHandler;
import com.princekumar.itams.auth.RestAuthenticationEntryPoint;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Security configuration.
 *
 * <ul>
 *   <li>Stateless (JWT); no server-side sessions.</li>
 *   <li>CSRF disabled — no cookies + no browser form posts, only bearer-token JSON.</li>
 *   <li>Public endpoints: {@code /auth/login}, {@code /auth/refresh}, {@code /auth/logout},
 *       health, and Swagger UI.</li>
 *   <li>Everything else requires authentication; per-role rules live on
 *       controllers via {@code @PreAuthorize}.</li>
 * </ul>
 */
@Configuration
@EnableMethodSecurity(prePostEnabled = true)
@EnableConfigurationProperties(JwtProperties.class)
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtFilter;
    private final AuthRateLimitFilter rateLimitFilter;
    private final RestAuthenticationEntryPoint entryPoint;
    private final RestAccessDeniedHandler accessDeniedHandler;

    public SecurityConfig(JwtAuthenticationFilter jwtFilter,
                          AuthRateLimitFilter rateLimitFilter,
                          RestAuthenticationEntryPoint entryPoint,
                          RestAccessDeniedHandler accessDeniedHandler) {
        this.jwtFilter = jwtFilter;
        this.rateLimitFilter = rateLimitFilter;
        this.entryPoint = entryPoint;
        this.accessDeniedHandler = accessDeniedHandler;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // Public
                .requestMatchers(HttpMethod.POST, "/api/v1/auth/login").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/v1/auth/refresh").permitAll()
                // Logout is authorised by the refresh token in the body (like /refresh),
                // so it must work after the 15-min access token has expired.
                .requestMatchers(HttpMethod.POST, "/api/v1/auth/logout").permitAll()
                .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                .requestMatchers(
                    "/v3/api-docs", "/v3/api-docs/**",
                    "/swagger-ui.html", "/swagger-ui/**"
                ).permitAll()
                // Everything else needs a valid token; per-role rules on
                // each controller via @PreAuthorize.
                .anyRequest().authenticated()
            )
            .exceptionHandling(eh -> eh
                .authenticationEntryPoint(entryPoint)
                .accessDeniedHandler(accessDeniedHandler)
            )
            // Rate-limit login BEFORE any authentication work, so an attacker
            // hammering /auth/login doesn't get to spin up BCrypt work per request.
            .addFilterBefore(rateLimitFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * {@code DelegatingPasswordEncoder} writes hashes with a prefix, e.g.
     * {@code {bcrypt}$2a$10$…} — this lets us migrate to a stronger
     * algorithm (Argon2, scrypt) later without rehashing existing users.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
