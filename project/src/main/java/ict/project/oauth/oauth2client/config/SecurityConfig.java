package ict.project.oauth.oauth2client.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpMethod;

import ict.project.oauth.oauth2client.service.CustomOAuth2SuccessHandler;
import ict.project.oauth.oauth2client.service.CustomOAuth2UserService;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
@Order(2) // ★ API 체인(@Order(1)) 뒤에 오도록
public class SecurityConfig {

    private final CustomOAuth2UserService customOAuth2UserService;
    private final CustomOAuth2SuccessHandler customOAuth2SuccessHandler;
    // private final CustomAuthExceptionHandler customAuthExceptionHandler; // 있으면 사용

    @Bean
    public SecurityFilterChain webFilterChain(HttpSecurity http) throws Exception {
        http
                // 이 체인은 /api/** 를 다루지 않습니다. (/api/** 는 @Order(1) 체인이 처리)
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(
                                "/", "/access-guest",
                                "/questions/**",
                                "/css/**", "/js/**", "/images/**"
                        ).permitAll()
                        .anyRequest().authenticated()
                )
                .oauth2Login(oauth2 -> oauth2
                        .userInfoEndpoint(userInfo -> userInfo.userService(customOAuth2UserService))
                        .successHandler(customOAuth2SuccessHandler)
                )
                // 루트 접근 시 카카오 로그인으로
                .formLogin(form -> form
                        .loginPage("/oauth2/authorization/kakao")
                        .permitAll()
                );
        // .exceptionHandling(ex -> ex.authenticationEntryPoint(customAuthExceptionHandler)); // 필요 시

        return http.build();
    }

    // ⚠️ PasswordEncoder는 AuthConfig 쪽에 이미 있으면 여기선 선언하지 마세요.
    // @Bean
    // public PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }
}



//package ict.project.oauth.oauth2client.config;
//
//import lombok.RequiredArgsConstructor;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.core.annotation.Order;
//import org.springframework.security.config.annotation.web.builders.HttpSecurity;
//import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
//import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
//import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
//import org.springframework.security.crypto.password.PasswordEncoder;
//import org.springframework.security.web.SecurityFilterChain;
//
//import ict.project.oauth.oauth2client.service.CustomOAuth2UserService;
//import ict.project.oauth.oauth2client.service.CustomOAuth2SuccessHandler;
//import ict.project.oauth.oauth2client.service.CustomAuthExceptionHandler;
//
//@Configuration
//@EnableWebSecurity
//@RequiredArgsConstructor
//public class SecurityConfig {
//
//    private final CustomOAuth2UserService customOAuth2UserService;
//    private final CustomOAuth2SuccessHandler customOAuth2SuccessHandler;
//
//    @Bean
//    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
//        http
//                .csrf(AbstractHttpConfigurer::disable)
//                .authorizeHttpRequests(auth -> auth
//                        .requestMatchers("/access-guest").permitAll()
//                        .requestMatchers("/questions/**").permitAll()
//                        .requestMatchers("/api/users/**").permitAll()
//                        .anyRequest().authenticated()
//                )
//                .oauth2Login(oauth2 -> oauth2
//                        .userInfoEndpoint(userInfo -> userInfo
//                                .userService(customOAuth2UserService)
//                        )
//                        .successHandler(customOAuth2SuccessHandler)
//                )
//                // ✅ 루트 접근 시 바로 카카오 로그인으로 보냄
//                .formLogin(form -> form
//                        .loginPage("/oauth2/authorization/kakao")
//                );
//
//        return http.build();
//    }
//
//    @Bean
//    public PasswordEncoder passwordEncoder() {
//        return new BCryptPasswordEncoder();
//    }
//}
