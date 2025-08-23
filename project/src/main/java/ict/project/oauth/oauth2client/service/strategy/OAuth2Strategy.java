package ict.project.oauth.oauth2client.service.strategy;

import ict.project.oauth.model.user.OauthType;
import ict.project.oauth.httpinterface.service.OAuth2UserInfo;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;

public interface OAuth2Strategy {
    OauthType getOAuth2ProviderType();

    OAuth2UserInfo getUserInfo(OAuth2User user);

    // boolean unlinkOAuth2Account();

    default void isOauthIdExist(String oauthId) {
        if (null == oauthId) {
            throw new OAuth2AuthenticationException("oauthId does not exist");
        }
    }
}
