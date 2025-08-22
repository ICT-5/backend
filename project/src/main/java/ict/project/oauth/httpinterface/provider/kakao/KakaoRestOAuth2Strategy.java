package ict.project.oauth.httpinterface.provider.kakao;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ict.project.oauth.httpinterface.provider.RestOAuth2Strategy;
import ict.project.oauth.httpinterface.service.OAuth2UserInfo;
import ict.project.oauth.model.user.OauthType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

@Component
@Slf4j
@RequiredArgsConstructor
public class KakaoRestOAuth2Strategy implements RestOAuth2Strategy {
    private final KakaoApiClient kakaoApiClient;
    private final KakaoAuthProperties kakaoAuthProperties;

    @Override
    public OauthType getOAuth2Type() {
        return OauthType.KAKAO;
    }

    @Override
    public OAuth2UserInfo getUserInfo() {
        final String authorizationCode = getAuthorizationCode();

        final KakaoToken tokenResponse = getKakaoToken(authorizationCode);

        KakaoUserInfo userInfoResponse = getKakaoUserInfo(tokenResponse);

        return new OAuth2UserInfo(OauthType.KAKAO, userInfoResponse.getId(), userInfoResponse.getNickname());
    }

    private KakaoUserInfo getKakaoUserInfo(KakaoToken tokenResponse) {
        final String token = "Bearer " + tokenResponse.getAccessToken();
        KakaoUserInfo userInfoResponse = kakaoApiClient.getUserInfo(token);
        log.info(userInfoResponse.toString());

        return userInfoResponse;
    }

    private KakaoToken getKakaoToken(String authorizationCode) {
        final String contentType = "application/x-www-form-urlencoded;charset=utf-8";
        final MultiValueMap<String, String> requestBody = new LinkedMultiValueMap<>();
        requestBody.add("grant_type", "authorization_code");
        requestBody.add("client_id", kakaoAuthProperties.getClientId());
        requestBody.add("redirect_uri", kakaoAuthProperties.getRedirectUri());
        requestBody.add("code", authorizationCode);
        requestBody.add("client_secret", kakaoAuthProperties.getClientSecret());
        final KakaoToken tokenResponse = kakaoApiClient.getAccessToken(contentType, requestBody);
        log.info(tokenResponse.toString());

        return tokenResponse;
    }

    private String getAuthorizationCode() {
        final String clientId = kakaoAuthProperties.getClientId();
        final String redirectUri = kakaoAuthProperties.getRedirectUri();
        final String responseType = "code";
        final String authorizationCode = kakaoApiClient.getAuthorizationCode(clientId, redirectUri, responseType);
        log.info("authorizationCode: {}", authorizationCode);

        return authorizationCode;
    }

//    @Override
//    public boolean unlinkOAuth2Account(final String oauthValue) {
//        final String appAdminKey = "KakaoAK " + kakaoAuthProperties.getAppAdminKey();
//        final MultiValueMap<String, String> requestBody = new LinkedMultiValueMap<>();
//        requestBody.add("target_id_type", "user_id");
//        requestBody.add("target_id", oauthValue);

//        ResponseEntity<UnlinkOAuth2AccountDto.KakaoResponse> response = kakaoApiClient.unlinkAccount(
//                MediaType.APPLICATION_FORM_URLENCODED_VALUE, appAdminKey, requestBody);
//        if (response.getStatusCode().is2xxSuccessful()) {
//            log.info("Kakao account unlink success response {}", response.getBody().getId());
//        return true;
//        } else {
//            log.error("Kakao account unlink failed response {}", response.getBody());
//            throw new AuthException(AuthErrorCode.SERVER_ERROR_UNLINK_FAILED);
//        }
//    }
}
