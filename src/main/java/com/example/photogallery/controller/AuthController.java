package com.example.photogallery.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class AuthController {

    @GetMapping("/login")
    public String login(
        @RequestParam(value = "logout", required = false) String logout,
        @RequestParam(value = "prompt", required = false) String prompt
    ) {
        String oauth2Login = "redirect:/oauth2/authorization/keycloak";

        if (logout != null) {
            return oauth2Login + "?prompt=login";
        }

        if ("login".equals(prompt)) {
            return oauth2Login + "?prompt=login";
        }

        return oauth2Login;
    }
}
