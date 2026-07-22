package by.taverna.shlyapnika.config;

import by.taverna.shlyapnika.security.InternalApiTokenFilter;
import by.taverna.shlyapnika.security.PublicPostRateLimitFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {
  @Bean
  SecurityFilterChain securityFilterChain(HttpSecurity http, TavernaProperties properties, PublicPostRateLimitFilter rateLimitFilter) throws Exception {
    return http
        .cors(Customizer.withDefaults())
        .csrf(AbstractHttpConfigurer::disable)
        .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(auth -> auth
            .requestMatchers("/api/internal/**").authenticated()
            .requestMatchers(HttpMethod.GET, "/api/**", "/health", "/ready", "/actuator/health", "/actuator/info", "/uploads/**").permitAll()
            .requestMatchers(HttpMethod.POST, "/api/game-signups", "/api/service-requests").permitAll()
            .anyRequest().permitAll()
        )
        .addFilterBefore(new InternalApiTokenFilter(properties.internalApiToken()), UsernamePasswordAuthenticationFilter.class)
        .addFilterBefore(rateLimitFilter, InternalApiTokenFilter.class)
        .build();
  }
}
