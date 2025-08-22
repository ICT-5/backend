package ict.project.oauth.model.user;

import java.util.Arrays;

public enum OauthType {
    KAKAO("kakao"), GITHUB("github");

    private final String type;

    OauthType(String type) {
        this.type = type;
    }

    public static OauthType ofType(String type) {
        return Arrays.stream(values())
                .filter(oauthType -> oauthType.type.equals(type))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No such OauthType"));
    }

    public String getType() {
        return type;
    }
}
