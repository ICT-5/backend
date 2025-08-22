package ict.project.oauth.controller;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController
public class AccessController {

    @GetMapping("/")
    public void redirectToLogin(HttpServletResponse response) throws IOException {
        response.sendRedirect("/oauth2/authorization/kakao");  // ✅ 카카오 로그인 바로 실행
    }

    @GetMapping("/access-user")
    public String accessUser() {
        return "✅ 로그인 성공! USER 전용 페이지입니다.";
    }

    @GetMapping("/access-guest")
    public String accessGuest() {
        return "👋 게스트 접근 페이지입니다.";
    }
}

