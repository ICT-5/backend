package ict.project.oauth.httpinterface.provider.kakao;

import org.springframework.http.HttpHeaders;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.PostExchange;

public interface KakaoApiClient {
    @GetExchange("https://kauth.kakao.com/oauth/authorize")
    String getAuthorizationCode(
            @RequestParam("client_id") String clientId,
            @RequestParam("redirect_uri") String redirectUri,
            @RequestParam("response_type") String responseType);

    @PostExchange("https://kauth.kakao.com/oauth/token")
    KakaoToken getAccessToken(
            @RequestHeader(HttpHeaders.CONTENT_TYPE) String contentType,
            @RequestBody MultiValueMap request);

    @GetExchange("https://kapi.kakao.com/v2/user/me")
    KakaoUserInfo getUserInfo(@RequestHeader(name = HttpHeaders.AUTHORIZATION) String token);


//    @PostExchange("https://kapi.kakao.com/v1/user/unlink")
//    ResponseEntity<UnlinkOAuth2Account.KakaoResponse> unlinkAccount(
//            @RequestHeader(HttpHeaders.CONTENT_TYPE) String contentType,
//            @RequestHeader(name = HttpHeaders.AUTHORIZATION) String appAdminKey,
//            @RequestBody MultiValueMap request);
}
