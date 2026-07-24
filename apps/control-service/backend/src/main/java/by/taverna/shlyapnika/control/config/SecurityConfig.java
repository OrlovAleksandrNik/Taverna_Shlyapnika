package by.taverna.shlyapnika.control.config;

import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {
  @Bean
  SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    return http
        .cors(Customizer.withDefaults())
        .csrf(csrf -> csrf
            .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
            .ignoringRequestMatchers(
                "/api/v1/auth/login",
                "/api/v1/auth/invitations/accept",
                "/api/v1/auth/password-reset",
                "/api/v1/auth/password-reset/confirm",
                "/api/v1/public/**"))
        .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
        .headers(headers -> {
          headers.contentSecurityPolicy(csp -> csp.policyDirectives("default-src 'self'; img-src 'self' data:; style-src 'self' 'unsafe-inline'; script-src 'self'; frame-ancestors 'none'"));
          headers.referrerPolicy(Customizer.withDefaults());
          headers.permissionsPolicy(policy -> policy.policy("camera=(), microphone=(), geolocation=()"));
          headers.httpStrictTransportSecurity(hsts -> hsts.includeSubDomains(true).preload(true));
        })
        .authorizeHttpRequests(auth -> auth
            .requestMatchers("/actuator/health/**", "/health", "/ready", "/api/v1/public/**", "/v3/api-docs/**", "/swagger-ui/**").permitAll()
            .requestMatchers(
                "/api/v1/auth/login",
                "/api/v1/auth/invitations/accept",
                "/api/v1/auth/password-reset",
                "/api/v1/auth/password-reset/confirm",
                "/api/v1/auth/csrf").permitAll()
            .anyRequest().authenticated())
        .logout(logout -> logout.logoutUrl("/api/v1/auth/logout").deleteCookies("CONTROLSESSION"))
        .build();
  }

  @Bean
  PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder(12);
  }

  @Bean
  AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
    return configuration.getAuthenticationManager();
  }

  @Bean
  CorsConfigurationSource corsConfigurationSource(ControlProperties properties) {
    CorsConfiguration config = new CorsConfiguration();
    config.setAllowedOrigins(List.of(properties.frontendOrigin()));
    config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
    config.setAllowedHeaders(List.of(HttpHeaders.CONTENT_TYPE, "X-XSRF-TOKEN", "X-Requested-With"));
    config.setAllowCredentials(true);
    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", config);
    return source;
  }
}
