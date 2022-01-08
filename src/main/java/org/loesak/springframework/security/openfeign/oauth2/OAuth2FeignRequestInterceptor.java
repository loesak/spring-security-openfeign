package org.loesak.springframework.security.openfeign.oauth2;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.oauth2.client.*;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.core.OAuth2AccessToken;

import java.util.Objects;

public class OAuth2FeignRequestInterceptor implements RequestInterceptor {

    private static final Authentication ANONYMOUS_AUTHENTICATION = new AnonymousAuthenticationToken(
            "anonymous", "anonymousUser", AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS"));

    private final OAuth2AuthorizedClientManager authorizedClientManager;
    private final String clientRegistrationId;

    public OAuth2FeignRequestInterceptor(
            final ClientRegistrationRepository clientRegistrationRepository,
            final OAuth2AuthorizedClientService authorizedClientService,
            final String clientRegistrationId) {

        OAuth2AuthorizedClientProvider authorizedClientProvider = OAuth2AuthorizedClientProviderBuilder
                .builder()
                .clientCredentials()
                .build();

        AuthorizedClientServiceOAuth2AuthorizedClientManager authorizedClientManager =
                new AuthorizedClientServiceOAuth2AuthorizedClientManager(
                        clientRegistrationRepository,
                        authorizedClientService);
        authorizedClientManager.setAuthorizedClientProvider(authorizedClientProvider);

        this.authorizedClientManager = authorizedClientManager;
        this.clientRegistrationId = clientRegistrationId;
    }

    public OAuth2FeignRequestInterceptor(
            final OAuth2AuthorizedClientManager authorizedClientManager,
            final String clientRegistrationId) {
        this.authorizedClientManager = authorizedClientManager;
        this.clientRegistrationId = clientRegistrationId;
    }


    @Override
    public void apply(RequestTemplate template) {
        if (this.authorizedClientManager == null) {
            return;
        }

        OAuth2AuthorizeRequest authorizeRequest = OAuth2AuthorizeRequest
                .withClientRegistrationId(this.clientRegistrationId)
                .principal(ANONYMOUS_AUTHENTICATION)
                .build();

        OAuth2AuthorizedClient authorizedClient = this.authorizedClientManager.authorize(authorizeRequest);
        OAuth2AccessToken accessToken = Objects.requireNonNull(authorizedClient).getAccessToken();
        template.header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", accessToken.getTokenValue()));
    }

}