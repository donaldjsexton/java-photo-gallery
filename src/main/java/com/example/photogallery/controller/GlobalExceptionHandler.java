package com.example.photogallery.controller;

import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.NoSuchElementException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;

@RestControllerAdvice(assignableTypes = { PhotoRestController.class })
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(
        GlobalExceptionHandler.class
    );

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleBadRequest(
        IllegalArgumentException ex,
        HttpServletRequest request
    ) {
        log.warn(
            "400 Bad Request at {}: {}",
            request.getRequestURI(),
            ex.getMessage()
        );

        ErrorResponse body = ErrorResponse.of(
            HttpStatus.BAD_REQUEST,
            "Bad Request",
            ex.getMessage(),
            request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(
        {
            NoSuchElementException.class,
            EntityNotFoundException.class,
            FileNotFoundException.class,
        }
    )
    public ResponseEntity<ErrorResponse> handleNotFound(
        Exception ex,
        HttpServletRequest request
    ) {
        log.info(
            "404 Not Found at {}: {}",
            request.getRequestURI(),
            ex.getMessage()
        );

        ErrorResponse body = ErrorResponse.of(
            HttpStatus.NOT_FOUND,
            "Not Found",
            ex.getMessage(),
            request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
    }

    @ExceptionHandler(IOException.class)
    public ResponseEntity<ErrorResponse> handleIOException(
        IOException ex,
        HttpServletRequest request
    ) {
        log.error(
            "I/O Error at {}: {}",
            request.getRequestURI(),
            ex.getMessage()
        );

        ErrorResponse body = ErrorResponse.of(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "I/O Error",
            ex.getMessage(),
            request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
            body
        );
    }

    @ExceptionHandler(
        { MaxUploadSizeExceededException.class, MultipartException.class }
    )
    public ResponseEntity<ErrorResponse> handleMultipartTooLarge(
        Exception ex,
        HttpServletRequest request
    ) {
        log.warn(
            "413 Payload Too Large at {}: {}",
            request.getRequestURI(),
            ex.getMessage()
        );

        ErrorResponse body = ErrorResponse.of(
            HttpStatus.PAYLOAD_TOO_LARGE,
            "Payload Too Large",
            "Upload too large. Try fewer photos at once, or increase PHOTO_GALLERY_MAX_REQUEST_SIZE.",
            request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(body);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(
        Exception ex,
        HttpServletRequest request
    ) {
        log.error(
            "500 Unexpected Server Error at {}: {}",
            request.getRequestURI(),
            ex.getMessage()
        );

        ErrorResponse body = ErrorResponse.of(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "Server Error",
            ex.getMessage(),
            request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
            body
        );
    }

    public static class ErrorResponse {

        private final LocalDateTime timestamp;
        private final int status;
        private final String error;
        private final String message;
        private final String path;

        private ErrorResponse(
            LocalDateTime timestamp,
            int status,
            String error,
            String message,
            String path
        ) {
            this.timestamp = timestamp;
            this.status = status;
            this.error = error;
            this.message = message;
            this.path = path;
        }

        public static ErrorResponse of(
            HttpStatus status,
            String error,
            String message,
            String path
        ) {
            return new ErrorResponse(
                LocalDateTime.now(),
                status.value(),
                error,
                message,
                path
            );
        }

        public LocalDateTime getTimestamp() {
            return timestamp;
        }

        public int getStatus() {
            return status;
        }

        public String getError() {
            return error;
        }

        public String getMessage() {
            return message;
        }

        public String getPath() {
            return path;
        }
    }
}
