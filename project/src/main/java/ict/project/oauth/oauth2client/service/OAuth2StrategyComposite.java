package ict.project.oauth.oauth2client.service;

import ict.project.oauth.model.user.OauthType;
import ict.project.oauth.oauth2client.service.strategy.OAuth2Strategy;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static java.util.function.UnaryOperator.identity;
import static java.util.stream.Collectors.toMap;

@Component
public class OAuth2StrategyComposite {
    private final Map<OauthType, OAuth2Strategy> oauth2ProviderMap;

    public OAuth2StrategyComposite(Set<OAuth2Strategy> clients) {
        this.oauth2ProviderMap = clients.stream()
                .collect(toMap(OAuth2Strategy::getOAuth2ProviderType, identity()));
    }

    public OAuth2Strategy getOAuth2Strategy(OauthType provider) {
        return Optional.ofNullable(oauth2ProviderMap.get(provider))
                .orElseThrow(() -> new OAuth2AuthenticationException("not supported OAuth2 provider"));
    }
}
