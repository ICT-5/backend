package ict.project.oauth.model.user;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class User {
    private int id;
    private String name;
    private String oauthId;
    private OauthType oauthType;
}
