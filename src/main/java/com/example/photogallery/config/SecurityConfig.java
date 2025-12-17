package com.example.photogallery.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.Customizer;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.oidc.web.logout.OidcClientInitiatedLogoutSuccessHandler;

@Configuration
public class SecurityConfig {

    @Bean
    @ConditionalOnProperty(
        prefix = "app.security.oidc",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true
    )
    SecurityFilterChain securityFilterChain(
        HttpSecurity http,
        LogoutSuccessHandler logoutSuccessHandler
    ) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/css/**", "/js/**", "/images/**").permitAll()
                .requestMatchers("/login").permitAll()
                .requestMatchers("/oauth2/**", "/login/oauth2/**").permitAll()
                .anyRequest().authenticated()
            )
            .oauth2Login(oauth2 -> oauth2
                .loginPage("/login")
                .defaultSuccessUrl("/dashboard", true)
            )
            .logout(logout -> logout.logoutSuccessHandler(logoutSuccessHandler));

        return http.build();
    }

    @Bean
    @ConditionalOnProperty(
        prefix = "app.security.oidc",
        name = "enabled",
        havingValue = "false",
        matchIfMissing = false
    )
    SecurityFilterChain localSecurityFilterChain(HttpSecurity http)
        throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/css/**", "/js/**", "/images/**").permitAll()
                .requestMatchers("/login").permitAll()
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .defaultSuccessUrl("/dashboard", true)
                .permitAll()
            )
            .logout(logout -> logout.logoutSuccessUrl("/login?logout"))
            .httpBasic(Customizer.withDefaults());

        return http.build();
    }

    @Bean
    @ConditionalOnProperty(
        prefix = "app.security.oidc",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true
    )
    LogoutSuccessHandler logoutSuccessHandler(
        ClientRegistrationRepository clientRegistrationRepository
    ) {
        OidcClientInitiatedLogoutSuccessHandler handler =
            new OidcClientInitiatedLogoutSuccessHandler(
                clientRegistrationRepository
            );
        handler.setPostLogoutRedirectUri("{baseUrl}/login?logout");
        return handler;
    }

    @Bean
    @ConditionalOnProperty(
        prefix = "app.security.oidc",
        name = "enabled",
        havingValue = "false",
        matchIfMissing = false
    )
    UserDetailsService userDetailsService(
        PasswordEncoder passwordEncoder,
        @Value("${app.security.local.username:admin}") String username,
        @Value("${app.security.local.password:admin}") String password
    ) {
        UserDetails admin = User
            .withUsername(username)
            .password(passwordEncoder.encode(password))
            .roles("ADMIN")
            .build();
        return new InMemoryUserDetailsManager(admin);
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
