package predictions.dapp.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {

    private final JwtRequestFilter jwtRequestFilter;

    public SecurityConfig(JwtRequestFilter jwtRequestFilter) {
        this.jwtRequestFilter = jwtRequestFilter;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // Disable CSRF for APIs
                .csrf(csrf -> csrf.disable())

                // Authorization rules
                .authorizeHttpRequests(auth -> auth
                        // Allow access to Swagger UI and API docs (ALL resources)
                        .requestMatchers(
                                "/swagger-ui.html",
                                "/swagger-ui/**",
                                "/v3/api-docs/**",
                                "/swagger-resources/**",
                                "/webjars/**"
                        ).permitAll()
                        // Allow access to auth endpoints
                        .requestMatchers("/auth/**").permitAll()
                        // Allow accesss to actuator
                        .requestMatchers("/actuator/**").permitAll()
                        // Allow access to the scraping API endpoints
                        .requestMatchers(HttpMethod.GET, "/api/player/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/match/**").permitAll()
                        // Allow access to ALL football API endpoints (public)
                        .requestMatchers(HttpMethod.GET, "/api/football/**").permitAll()
                        // Allow access to prediction, performance, and history endpoints
                        // NOTE: These check authentication internally and return appropriate messages
                        .requestMatchers("/api/performance/**", "/api/history").permitAll()
                        // Allow access to predictions endpoint (checks authentication internally)
                        .requestMatchers(HttpMethod.GET, "/api/predictions/**").permitAll()
                        .anyRequest().authenticated() // All other requests require authentication
                )

                // Stateless (JWT only)
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // JWT filter
                .addFilterBefore(jwtRequestFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration cfg) throws Exception {
        return cfg.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}