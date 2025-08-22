package ict.project.oauth.oauth2client.service.strategy;

import ict.project.oauth.model.user.OauthType;
import ict.project.oauth.httpinterface.service.OAuth2UserInfo;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class KakaoOAuth2Strategy implements OAuth2Strategy {

    @Override
    public OauthType getOAuth2ProviderType() {

        return OauthType.KAKAO;
    }

    @Override
    public OAuth2UserInfo getUserInfo(OAuth2User user) {
        final Map<String, Object> attributes = user.getAttributes();
        final String oauthId = String.valueOf(attributes.get("id"));
        final String oauthName = String.valueOf(((Map<String, Object>) attributes.get("properties")).get("nickname"));

        return new OAuth2UserInfo(OauthType.KAKAO, oauthId, oauthName);
    }
}
