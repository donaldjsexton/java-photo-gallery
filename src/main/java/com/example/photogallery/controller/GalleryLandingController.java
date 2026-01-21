package com.example.photogallery.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class GalleryLandingController {

    @GetMapping("/gallery")
    public String galleryRoot() {
        return "redirect:/dashboard";
    }
}
