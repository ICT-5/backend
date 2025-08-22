package ict.project.oauth.httpinterface.provider.kakao;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "oauth2.kakao")
@Getter
@RequiredArgsConstructor
public class KakaoAuthProperties {
    private final String clientId;
    private final String clientSecret;
    private final String redirectUri;
}
