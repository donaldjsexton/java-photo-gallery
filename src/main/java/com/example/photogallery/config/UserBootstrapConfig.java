package com.example.photogallery.config;

import com.example.photogallery.model.User;
import com.example.photogallery.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.util.StringUtils;

@Configuration
public class UserBootstrapConfig {

    @Bean
    CommandLineRunner bootstrapAdminUser(
        UserRepository userRepository,
        PasswordEncoder passwordEncoder
    ) {
        // Optional bootstrap for production: supply env vars via your process manager
        // (for example systemd: EnvironmentFile=/etc/gallery.env).
        return args -> {
            String enabledFlag = System.getenv("PHOTO_GALLERY_BOOTSTRAP_ADMIN");
            if (!"true".equalsIgnoreCase(enabledFlag)) {
                return;
            }

            String username = System.getenv("PHOTO_GALLERY_ADMIN_USERNAME");
            String email = System.getenv("PHOTO_GALLERY_ADMIN_EMAIL");
            String password = System.getenv("PHOTO_GALLERY_ADMIN_PASSWORD");

            if (!StringUtils.hasText(username) ||
                !StringUtils.hasText(email) ||
                !StringUtils.hasText(password)) {
                return;
            }

            if (userRepository.existsByUsername(username)) {
                return;
            }

            User admin = new User(
                username,
                email,
                passwordEncoder.encode(password),
                "ADMIN",
                true
            );
            userRepository.save(admin);
        };
    }
}
