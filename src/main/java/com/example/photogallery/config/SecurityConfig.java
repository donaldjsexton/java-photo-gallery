package com.example.photogallery.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.security.core.Authentication;

@Configuration
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(
        HttpSecurity http,
        LogoutSuccessHandler logoutSuccessHandler,
        OAuth2AuthorizationRequestResolver authorizationRequestResolver
    ) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/css/**", "/js/**", "/images/**").permitAll()
                .requestMatchers("/", "/favicon.ico").permitAll()
                .requestMatchers("/login").permitAll()
                .requestMatchers("/oauth2/**", "/login/oauth2/**").permitAll()
                .requestMatchers("/share/**").permitAll()
                .anyRequest().authenticated()
            )
            .oauth2Login(oauth2 -> oauth2
                .loginPage("/login")
                .defaultSuccessUrl("/dashboard", true)
                .authorizationEndpoint(endpoint -> endpoint
                    .authorizationRequestResolver(authorizationRequestResolver)
                )
            )
            .logout(logout -> logout
                .logoutSuccessHandler(logoutSuccessHandler)
                .invalidateHttpSession(true)
                .clearAuthentication(true)
                .deleteCookies("JSESSIONID")
            );

        return http.build();
    }

    /**
     * Ensures Keycloak doesn't silently re-authenticate users right after logout when an SSO
     * session still exists at the IdP. When the request includes {@code ?prompt=login}, we
     * forward that into the OAuth2 authorization request.
     */
    @Bean
    OAuth2AuthorizationRequestResolver authorizationRequestResolver(
        ClientRegistrationRepository clientRegistrationRepository
    ) {
        DefaultOAuth2AuthorizationRequestResolver delegate =
            new DefaultOAuth2AuthorizationRequestResolver(
                clientRegistrationRepository,
                "/oauth2/authorization"
            );

        return new OAuth2AuthorizationRequestResolver() {
            @Override
            public OAuth2AuthorizationRequest resolve(HttpServletRequest request) {
                return customize(request, delegate.resolve(request));
            }

            @Override
            public OAuth2AuthorizationRequest resolve(
                HttpServletRequest request,
                String registrationId
            ) {
                return customize(request, delegate.resolve(request, registrationId));
            }

            private OAuth2AuthorizationRequest customize(
                HttpServletRequest request,
                OAuth2AuthorizationRequest authorizationRequest
            ) {
                if (authorizationRequest == null) {
                    return null;
                }

                Map<String, Object> additionalParameters =
                    new LinkedHashMap<>(authorizationRequest.getAdditionalParameters());

                String prompt = request.getParameter("prompt");
                if (StringUtils.hasText(prompt)) {
                    additionalParameters.put("prompt", prompt.trim());
                }

                return OAuth2AuthorizationRequest
                    .from(authorizationRequest)
                    .additionalParameters(additionalParameters)
                    .build();
            }
        };
    }

    @Bean
    LogoutSuccessHandler logoutSuccessHandler(
        ClientRegistrationRepository clientRegistrationRepository
    ) {
        ClientRegistration keycloak =
            clientRegistrationRepository.findByRegistrationId("keycloak");

        return (HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication) -> {
            String postLogoutRedirectUri = ServletUriComponentsBuilder
                .fromRequestUri(request)
                .replacePath(request.getContextPath() + "/login")
                .replaceQuery("logout")
                .build()
                .toUriString();

            String endSessionEndpoint = resolveEndSessionEndpoint(keycloak);
            if (!StringUtils.hasText(endSessionEndpoint)) {
                response.sendRedirect(request.getContextPath() + "/login?logout");
                return;
            }

            String idTokenHint = null;
            if (authentication instanceof OAuth2AuthenticationToken oauth2Token) {
                if (oauth2Token.getPrincipal() instanceof OidcUser oidcUser) {
                    idTokenHint = oidcUser.getIdToken().getTokenValue();
                }
            }

            UriComponentsBuilder redirect = UriComponentsBuilder
                .fromUriString(endSessionEndpoint)
                .queryParam("post_logout_redirect_uri", postLogoutRedirectUri);

            if (StringUtils.hasText(idTokenHint)) {
                redirect.queryParam("id_token_hint", idTokenHint);
            }
            if (keycloak != null && StringUtils.hasText(keycloak.getClientId())) {
                redirect.queryParam("client_id", keycloak.getClientId());
            }

            response.sendRedirect(redirect.build().encode().toUriString());
        };
    }

    private static String resolveEndSessionEndpoint(ClientRegistration clientRegistration) {
        if (clientRegistration == null) {
            return null;
        }

        Object fromMetadata = clientRegistration
            .getProviderDetails()
            .getConfigurationMetadata()
            .get("end_session_endpoint");
        if (fromMetadata instanceof String endSessionEndpoint &&
            StringUtils.hasText(endSessionEndpoint)) {
            return endSessionEndpoint;
        }

        String authorizationUri = clientRegistration
            .getProviderDetails()
            .getAuthorizationUri();
        if (!StringUtils.hasText(authorizationUri)) {
            return null;
        }

        String keycloakAuthSuffix = "/protocol/openid-connect/auth";
        if (authorizationUri.endsWith(keycloakAuthSuffix)) {
            return authorizationUri.substring(
                0,
                authorizationUri.length() - keycloakAuthSuffix.length()
            ) + "/protocol/openid-connect/logout";
        }

        return null;
    }
}
