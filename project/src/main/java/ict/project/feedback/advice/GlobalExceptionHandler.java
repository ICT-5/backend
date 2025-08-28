package ict.project.feedback.advice;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@Component("feedbackGlobalExceptionHandler")                // ★ 빈 이름 고유화
@RestControllerAdvice(basePackages = "org.example.feedback") // ★ 적용 범위 한정(피드백 모듈 내부만)
@Order(Ordered.HIGHEST_PRECEDENCE)                          // (선택) 우선순위 부여
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Map<String, Object> handleValidation(MethodArgumentNotValidException e) {
        return Map.of(
                "apiVersion", "1.0",
                "error", Map.of(
                        "code", "BAD_REQUEST",
                        "message", e.getBindingResult().toString()
                )
        );
    }
}
