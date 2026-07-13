package com.politico.admin;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.config.oauth2.client.CommonOAuth2Provider;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Two chains:
 * - Order(1) covers /api/v1/admin/**, /login/oauth2/**, /oauth2/**.
 *   - Prod (Google client-id set + not `local` profile) uses OIDC login with email whitelist.
 *   - `local` profile OR missing Google creds → falls back to HTTP-Basic (admin/change-me).
 * - Order(2) is a catch-all permitAll for the public API/SPA.
 *
 * Whitelisted emails come from `politico.admin.allowed-emails` (comma-separated).
 * A 401 JSON body is returned for unauthenticated /api/v1/admin/** so the SPA can react cleanly.
 */
@Configuration
@RequiredArgsConstructor
public class AdminSecurityConfig {

    @Value("${politico.admin.username}")
    private String username;

    @Value("${politico.admin.password}")
    private String password;

    @Value("${politico.admin.allowed-emails:}")
    private String allowedEmailsCsv;

    @Value("${GOOGLE_OAUTH_CLIENT_ID:}")
    private String googleClientId;

    @Value("${GOOGLE_OAUTH_CLIENT_SECRET:}")
    private String googleClientSecret;

    private static final RequestMatcher ADMIN_MATCHER = new OrRequestMatcher(
            new AntPathRequestMatcher("/api/v1/admin/**"),
            new AntPathRequestMatcher("/login/oauth2/**"),
            new AntPathRequestMatcher("/oauth2/**")
    );

    /**
     * Registers Google OIDC only when the client-id is present.
     * Guards against Spring's auto-config failing on empty credentials.
     */
    @Bean
    @ConditionalOnExpression("'${GOOGLE_OAUTH_CLIENT_ID:}' != ''")
    public ClientRegistrationRepository clientRegistrationRepository() {
        ClientRegistration google = CommonOAuth2Provider.GOOGLE
                .getBuilder("google")
                .clientId(googleClientId)
                .clientSecret(googleClientSecret)
                .scope("openid", "email", "profile")
                .build();
        return new InMemoryClientRegistrationRepository(google);
    }

    @Bean
    public InMemoryUserDetailsManager userDetailsManager() {
        UserDetails admin = User.withDefaultPasswordEncoder()
                .username(username)
                .password(password)
                .roles("ADMIN")
                .build();
        return new InMemoryUserDetailsManager(admin);
    }

    private boolean oidcEnabled(String[] activeProfiles) {
        boolean isLocal = Arrays.asList(activeProfiles).contains("local");
        return !isLocal && googleClientId != null && !googleClientId.isBlank();
    }

    @Bean
    @Order(1)
    public SecurityFilterChain adminChain(HttpSecurity http,
                                          org.springframework.core.env.Environment env,
                                          ObjectMapper objectMapper) throws Exception {
        http
                .securityMatcher(ADMIN_MATCHER)
                .csrf(c -> c.disable())
                .exceptionHandling(e -> e.defaultAuthenticationEntryPointFor(
                        jsonUnauthorized(objectMapper),
                        new AntPathRequestMatcher("/api/v1/admin/**")))
                .authorizeHttpRequests(a -> a
                        .requestMatchers("/login/oauth2/**", "/oauth2/**").permitAll()
                        .anyRequest().hasRole("ADMIN"));

        if (oidcEnabled(env.getActiveProfiles())) {
            http
                    .oauth2Login(cfg -> cfg
                            .userInfoEndpoint(u -> u.oidcUserService(customOidcService()))
                            .defaultSuccessUrl("/admin", true)
                            .failureHandler((request, response, exception) -> {
                                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                                response.setContentType("application/json");
                                response.getWriter().write(
                                        "{\"error\":\"oidc_login_failed\",\"message\":\""
                                                + safe(exception.getMessage()) + "\"}");
                            }))
                    .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED));
        } else {
            http
                    .httpBasic(b -> {})
                    .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
        }
        return http.build();
    }

    @Bean
    @Order(2)
    public SecurityFilterChain publicChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(a -> a.anyRequest().permitAll())
                .csrf(c -> c.disable())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
        return http.build();
    }

    /**
     * Wraps the default OidcUserService and rejects users whose email isn't in the allow list.
     * Grants ROLE_ADMIN to allowed users so `.hasRole("ADMIN")` matches.
     */
    @Bean
    public OAuth2UserService<OidcUserRequest, OidcUser> customOidcService() {
        OidcUserService delegate = new OidcUserService();
        Set<String> allowed = parseAllowedEmails(allowedEmailsCsv);
        return userRequest -> {
            OidcUser user = delegate.loadUser(userRequest);
            String email = user.getEmail();
            String normalized = email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
            if (normalized.isEmpty() || !allowed.contains(normalized)) {
                throw new OAuth2AuthenticationException(
                        new OAuth2Error("access_denied", "Email not on admin allow-list: " + email, null));
            }
            Set<org.springframework.security.core.GrantedAuthority> authorities = new HashSet<>(user.getAuthorities());
            authorities.add(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_ADMIN"));
            return new org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser(
                    authorities, user.getIdToken(), user.getUserInfo(), "email");
        };
    }

    private static Set<String> parseAllowedEmails(String csv) {
        Set<String> out = new HashSet<>();
        if (csv == null) return out;
        for (String p : csv.split(",")) {
            String s = p.trim().toLowerCase(Locale.ROOT);
            if (!s.isEmpty()) out.add(s);
        }
        return out;
    }

    private static AuthenticationEntryPoint jsonUnauthorized(ObjectMapper mapper) {
        return (request, response, authException) -> writeJson401(response, mapper, authException);
    }

    private static void writeJson401(HttpServletResponse response, ObjectMapper mapper,
                                     AuthenticationException ex) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("error", "unauthorized");
        body.put("message", ex == null ? "authentication required" : ex.getMessage());
        response.getWriter().write(mapper.writeValueAsString(body));
    }

    private static String safe(String s) {
        return s == null ? "" : s.replace("\"", "'");
    }
}
