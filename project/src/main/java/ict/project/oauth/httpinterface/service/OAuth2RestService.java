package ict.project.oauth.httpinterface.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ict.project.oauth.model.user.OauthType;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
@Slf4j
@RequiredArgsConstructor
public class OAuth2RestService {
    private final OAuth2RestStrategyComposite oauth2RestStrategyComposite;

    public boolean authenticate(OauthType oauthType) {
        log.info("oauthType: {}", oauthType);
        final OAuth2UserInfo userInfo = oauth2RestStrategyComposite.getOAuth2Strategy(oauthType).getUserInfo();

        return Objects.nonNull(userInfo);
    }
}
