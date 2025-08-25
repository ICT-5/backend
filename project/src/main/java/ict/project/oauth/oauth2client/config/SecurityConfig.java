package ict.project.oauth.oauth2client.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

import ict.project.oauth.oauth2client.service.CustomOAuth2UserService;
import ict.project.oauth.oauth2client.service.CustomOAuth2SuccessHandler;
import ict.project.oauth.oauth2client.service.CustomAuthExceptionHandler;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomOAuth2UserService customOAuth2UserService;
    private final CustomOAuth2SuccessHandler customOAuth2SuccessHandler;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/access-guest").permitAll()
                        .requestMatchers("/questions/**").permitAll()
                        .anyRequest().authenticated()
                )
                .oauth2Login(oauth2 -> oauth2
                        .userInfoEndpoint(userInfo -> userInfo
                                .userService(customOAuth2UserService)
                        )
                        .successHandler(customOAuth2SuccessHandler)
                )
                // ✅ 루트 접근 시 바로 카카오 로그인으로 보냄
                .formLogin(form -> form
                        .loginPage("/oauth2/authorization/kakao")
                );

        return http.build();
    }
}
