package ict.project.oauth.httpinterface.service;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import ict.project.oauth.model.user.OauthType;

@Getter
@RequiredArgsConstructor
public class OAuth2UserInfo {
    private final OauthType oauthType;
    private final String oauthId;
    private final String name;
}
