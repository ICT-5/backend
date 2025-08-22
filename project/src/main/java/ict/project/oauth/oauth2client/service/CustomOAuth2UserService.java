package ict.project.oauth.oauth2client.service;

import ict.project.oauth.model.user.OauthType;
import ict.project.oauth.oauth2client.service.OAuth2StrategyComposite;
import ict.project.oauth.httpinterface.service.OAuth2UserInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final OAuth2StrategyComposite oauth2StrategyComposite;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oauth2User = super.loadUser(userRequest);

        // ✅ OAuth2UserInfo 가져오기
        OAuth2UserInfo oauth2UserInfo = oauth2StrategyComposite
                .getOAuth2Strategy(getSocialProvider(userRequest))
                .getUserInfo(oauth2User);

        // ✅ 무조건 ROLE_USER 부여
        List<SimpleGrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_USER"));

        log.info("✅ 로그인 성공: {} {}", oauth2UserInfo.getOauthId(), oauth2UserInfo.getName());

        // ✅ 카카오의 PK는 "id"
        return new DefaultOAuth2User(authorities, oauth2User.getAttributes(), "id");
    }

    private OauthType getSocialProvider(OAuth2UserRequest userRequest) {
        return OauthType.ofType(userRequest.getClientRegistration().getRegistrationId());
    }
}
