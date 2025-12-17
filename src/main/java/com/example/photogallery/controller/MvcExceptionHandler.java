package com.example.photogallery.controller;

import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@ControllerAdvice(annotations = Controller.class)
public class MvcExceptionHandler {

    @ExceptionHandler(
        { MaxUploadSizeExceededException.class, MultipartException.class }
    )
    public String handleMultipartTooLarge(
        Exception ex,
        HttpServletRequest request,
        RedirectAttributes redirectAttributes
    ) {
        redirectAttributes.addFlashAttribute(
            "message",
            "Upload too large. Try fewer photos at once, or increase PHOTO_GALLERY_MAX_REQUEST_SIZE."
        );

        String target = "/";
        String referer = request.getHeader("Referer");
        if (referer != null) {
            try {
                URI uri = URI.create(referer);
                String path = uri.getPath();
                if (path != null && path.startsWith("/")) {
                    target = path;
                    if (uri.getQuery() != null && !uri.getQuery().isBlank()) {
                        target += "?" + uri.getQuery();
                    }
                }
            } catch (IllegalArgumentException ignored) {}
        }

        return "redirect:" + target;
    }
}

