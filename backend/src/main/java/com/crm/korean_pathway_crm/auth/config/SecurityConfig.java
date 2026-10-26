package com.crm.korean_pathway_crm.auth.config;

import com.crm.korean_pathway_crm.auth.repository.InvalidatedTokenRepository;
import com.crm.korean_pathway_crm.user.repository.UserRepository;
import com.crm.korean_pathway_crm.user.entity.User;
import com.crm.korean_pathway_crm.user.dto.ApiResponse;
import com.crm.korean_pathway_crm.role.enums.RoleStatus;
import com.crm.korean_pathway_crm.exception.ErrorCode;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpServletResponse;
import tools.jackson.databind.json.JsonMapper;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.security.oauth2.server.resource.web.DefaultBearerTokenResolver;

import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import java.util.Set;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@EnableConfigurationProperties(JwtProperties.class)
public class SecurityConfig {
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(10);
    }

    @Bean
    public JwtDecoder jwtDecoder(JwtProperties properties, InvalidatedTokenRepository invalidatedTokens,
                                 UserRepository users) {
        byte[] keyBytes = properties.signingKeyBytes();
        NimbusJwtDecoder decoder = NimbusJwtDecoder
                .withSecretKey(new SecretKeySpec(keyBytes, "HmacSHA512"))
                .macAlgorithm(MacAlgorithm.HS512)
                .build();

        OAuth2TokenValidator<Jwt> notRevoked = token -> {
            if (token.getExpiresAt() == null || token.getIssuedAt() == null
                    || token.getId() == null || token.getId().isBlank()
                    || invalidatedTokens.existsById(token.getId())) {
                return OAuth2TokenValidatorResult.failure(
                        new OAuth2Error("invalid_token", "Token is missing required claims or has been revoked", null));
            }
            return OAuth2TokenValidatorResult.success();
        };
        OAuth2TokenValidator<Jwt> currentAccount = token -> {
            UUID userId;
            try {
                userId = UUID.fromString(token.getSubject());
            } catch (IllegalArgumentException | NullPointerException exception) {
                return invalidSession();
            }
            User user = users.findWithRoleById(userId).orElse(null);
            LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
            if (user == null || !user.isActive() || user.getRole() == null
                    || user.getRole().getStatus() != RoleStatus.ACTIVE
                    || (user.getLockedUntil() != null && user.getLockedUntil().isAfter(now))) {
                return invalidSession();
            }
            String credentialVersion = user.getPasswordChangedAt() == null ? "" : user.getPasswordChangedAt().toString();
            if (!credentialVersion.equals(token.getClaimAsString("passwordChangedAt"))
                    || !user.getRole().getId().toString().equals(token.getClaimAsString("roleId"))
                    || !("ROLE_" + user.getRole().getCode()).equals(token.getClaimAsString("scope"))) {
                return invalidSession();
            }
            return OAuth2TokenValidatorResult.success();
        };
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                new JwtTimestampValidator(Duration.ZERO),
                new JwtIssuerValidator(properties.getIssuer()),
                notRevoked,
                currentAccount));
        return decoder;
    }

    private OAuth2TokenValidatorResult invalidSession() {
        return OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token", "Session is no longer valid", null));
    }

    @Bean
    public BearerTokenResolver bearerTokenResolver() {
        DefaultBearerTokenResolver resolver = new DefaultBearerTokenResolver();
        Set<String> credentialEndpoints = Set.of("/api/v1/auth/log-in", "/api/v1/auth/refresh", "/api/v1/auth/logout");
        return request -> {
            // Refresh uses its body credential; logout verifies even expired access tokens in the service.
            if ("POST".equals(request.getMethod()) && credentialEndpoints.contains(request.getServletPath())) {
                return null;
            }
            return resolver.resolve(request);
        };
    }

    @Bean
    public AuthenticationEntryPoint authenticationEntryPoint(JsonMapper json) {
        return (request, response, exception) -> {
            response.setHeader("WWW-Authenticate", "Bearer");
            writeError(response, ErrorCode.INVALID_ACCESS_TOKEN, json);
        };
    }

    @Bean
    public AccessDeniedHandler accessDeniedHandler(JsonMapper json) {
        return (request, response, exception) -> writeError(response, ErrorCode.ACCESS_DENIED, json);
    }

    private void writeError(HttpServletResponse response, ErrorCode error, JsonMapper json) throws IOException {
        response.setStatus(error.getHttpStatus().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(json.writeValueAsString(ApiResponse.<Void>builder()
                .code(error.getCode()).message(error.getMessage()).build()));
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter authorities = new JwtGrantedAuthoritiesConverter();
        authorities.setAuthorityPrefix("");
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(authorities);
        return converter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, JwtAuthenticationConverter converter,
                                                  BearerTokenResolver resolver, AuthenticationEntryPoint entryPoint,
                                                  AccessDeniedHandler deniedHandler) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> authorize
                        .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                        .requestMatchers(HttpMethod.POST,
                                "/api/v1/auth/log-in", "/api/v1/auth/refresh", "/api/v1/auth/logout").permitAll()
                        .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**", "/actuator/health").permitAll()
                        .anyRequest().authenticated())
                .exceptionHandling(errors -> errors.authenticationEntryPoint(entryPoint).accessDeniedHandler(deniedHandler))
                .oauth2ResourceServer(resourceServer -> resourceServer
                        .bearerTokenResolver(resolver)
                        .authenticationEntryPoint(entryPoint)
                        .accessDeniedHandler(deniedHandler)
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(converter)));
        return http.build();
    }
}
