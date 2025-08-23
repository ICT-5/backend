package ict.project.oauth.httpinterface.provider.kakao;


public record KakaoToken(
        String tokenType,
        String accessToken,
        String refreshToken,
        Long expiresIn,
        Long refreshTokenExpiresIn,
        String scope
) {
    public String getAccessToken() {
        return accessToken;
    }

    @Override
    public String toString() {
        return "KakaoToken{" +
                "tokenType='" + tokenType + '\'' +
                ", accessToken='" + accessToken + '\'' +
                ", refreshToken='" + refreshToken + '\'' +
                '}';
    }
}