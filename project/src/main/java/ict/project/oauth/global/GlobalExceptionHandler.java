package ict.project.oauth.global;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
    @ExceptionHandler(OAuth2AuthenticationException.class)
    public ResponseEntity handleAuthenticationException(OAuth2AuthenticationException e, HttpServletRequest request) {
        log.warn(e.getMessage(), e);
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED.value())
                .body(e.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity handlerException(Exception e, HttpServletRequest request) {
        log.warn(e.getMessage(), e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .body(e.getMessage());
    }
}
