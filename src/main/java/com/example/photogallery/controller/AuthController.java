package com.example.photogallery.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AuthController {

    @Value("${app.security.oidc.enabled:true}")
    private boolean oidcEnabled;

    @GetMapping("/login")
    public String login(Model model) {
        model.addAttribute("oidcEnabled", oidcEnabled);
        return "login";
    }
}
