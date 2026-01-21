package com.example.photogallery.controller;

import com.example.photogallery.model.User;
import com.example.photogallery.repository.UserRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin")
public class UserAdminController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserAdminController(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping("/users")
    public String users(Model model) {
        model.addAttribute("users", userRepository.findAll());
        model.addAttribute("createForm", new CreateUserForm());
        model.addAttribute("passwordForm", new PasswordResetForm());
        model.addAttribute("roles", List.of("ADMIN", "CLIENT"));
        return "admin-users";
    }

    @PostMapping("/users")
    public String createUser(
        @Valid @ModelAttribute("createForm") CreateUserForm form,
        BindingResult bindingResult,
        Model model,
        RedirectAttributes redirectAttributes
    ) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("users", userRepository.findAll());
            model.addAttribute("passwordForm", new PasswordResetForm());
            model.addAttribute("roles", List.of("ADMIN", "CLIENT"));
            return "admin-users";
        }

        if (userRepository.existsByUsername(form.getUsername())) {
            bindingResult.rejectValue("username", "duplicate", "Username already exists.");
        }
        if (userRepository.findByEmail(form.getEmail()).isPresent()) {
            bindingResult.rejectValue("email", "duplicate", "Email already exists.");
        }
        if (bindingResult.hasErrors()) {
            model.addAttribute("users", userRepository.findAll());
            model.addAttribute("passwordForm", new PasswordResetForm());
            model.addAttribute("roles", List.of("ADMIN", "CLIENT"));
            return "admin-users";
        }

        String role = normalizeRole(form.getRole());
        User user = new User(
            form.getUsername().trim(),
            form.getEmail().trim().toLowerCase(Locale.ROOT),
            passwordEncoder.encode(form.getPassword()),
            role,
            true
        );
        userRepository.save(user);
        redirectAttributes.addFlashAttribute("message", "User created.");
        return "redirect:/admin/users";
    }

    @PostMapping("/users/{id}/password")
    public String resetPassword(
        @PathVariable("id") Long id,
        @Valid @ModelAttribute("passwordForm") PasswordResetForm form,
        BindingResult bindingResult,
        RedirectAttributes redirectAttributes,
        Model model
    ) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("users", userRepository.findAll());
            model.addAttribute("createForm", new CreateUserForm());
            model.addAttribute("passwordForm", new PasswordResetForm());
            model.addAttribute("roles", List.of("ADMIN", "CLIENT"));
            return "admin-users";
        }

        Optional<User> user = userRepository.findById(id);
        if (user.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "User not found.");
            return "redirect:/admin/users";
        }

        User target = user.get();
        target.setPasswordHash(passwordEncoder.encode(form.getPassword()));
        userRepository.save(target);
        redirectAttributes.addFlashAttribute("message", "Password updated.");
        return "redirect:/admin/users";
    }

    private static String normalizeRole(String input) {
        if (!"ADMIN".equalsIgnoreCase(input)) {
            return "CLIENT";
        }
        return "ADMIN";
    }

    public static class CreateUserForm {
        @NotBlank
        @Size(min = 3, max = 150)
        private String username;

        @NotBlank
        @Email
        @Size(max = 255)
        private String email;

        @NotBlank
        @Size(min = 8, max = 100)
        private String password;

        @NotBlank
        private String role = "CLIENT";

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public String getRole() {
            return role;
        }

        public void setRole(String role) {
            this.role = role;
        }
    }

    public static class PasswordResetForm {
        @NotBlank
        @Size(min = 8, max = 100)
        private String password;

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }
    }
}
