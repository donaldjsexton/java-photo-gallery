package com.example.photogallery.service;

import com.example.photogallery.model.User;
import com.example.photogallery.repository.UserRepository;
import java.util.Collections;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class DatabaseUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public DatabaseUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository
            .findByUsername(username)
            .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        String role = user.getRole();
        String authority = StringUtils.hasText(role) && role.startsWith("ROLE_")
            ? role
            : "ROLE_" + (StringUtils.hasText(role) ? role : "CLIENT");

        return new org.springframework.security.core.userdetails.User(
            user.getUsername(),
            user.getPasswordHash(),
            user.isEnabled(),
            true,
            true,
            true,
            Collections.singleton(new SimpleGrantedAuthority(authority))
        );
    }
}
