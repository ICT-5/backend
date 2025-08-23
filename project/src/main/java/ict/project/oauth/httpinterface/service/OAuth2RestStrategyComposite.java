package ict.project.oauth.httpinterface.service;

import ict.project.oauth.httpinterface.provider.RestOAuth2Strategy;
import ict.project.oauth.model.user.OauthType;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static java.util.function.UnaryOperator.identity;
import static java.util.stream.Collectors.toMap;

@Component
public class OAuth2RestStrategyComposite {
    private final Map<OauthType, RestOAuth2Strategy> oauth2ProviderMap;

    public OAuth2RestStrategyComposite(Set<RestOAuth2Strategy> clients) {
        this.oauth2ProviderMap = clients.stream()
                .collect(toMap(RestOAuth2Strategy::getOAuth2Type, identity()));
    }

    public RestOAuth2Strategy getOAuth2Strategy(OauthType provider) {
        return Optional.ofNullable(oauth2ProviderMap.get(provider))
                .orElseThrow(() -> new OAuth2AuthenticationException("not supported OAuth2 provider"));
    }
}
