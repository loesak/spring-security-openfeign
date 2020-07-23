package org.loesak.springframework.security.openfeign.oauth2;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.endpoint.DefaultClientCredentialsTokenResponseClient;
import org.springframework.security.oauth2.client.endpoint.OAuth2AccessTokenResponseClient;
import org.springframework.security.oauth2.client.endpoint.OAuth2ClientCredentialsGrantRequest;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.endpoint.OAuth2AccessTokenResponse;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;

@Slf4j
@RequiredArgsConstructor
public class OAuth2FeignRequestInterceptor implements RequestInterceptor {

    private final ClientRegistrationRepository clientRegistrationRepository;
    private final OAuth2AuthorizedClientService authorizedClientService;
    private final String clientRegistrationId;

    private final Clock clock = Clock.systemUTC();
    private final Duration accessTokenExpiresSkew = Duration.ofMinutes(1);

    private OAuth2AccessTokenResponseClient<OAuth2ClientCredentialsGrantRequest> clientCredentialsTokenResponseClient = new DefaultClientCredentialsTokenResponseClient();

    /*
    TODO
        need ability to use current authenticated users authentication on downstream calls
        or to use the configured client credentials, and a means to decide when do one or the other
        - always use current security authentication if its oauth based?
        - always use configured oauth client
        - try the first than the other?
        - use the first if blah, else if system operation?
     */

    @Override
    public void apply(RequestTemplate template) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        // TODO: should pass along existing authentication?

        OAuth2AuthorizedClient oauthClient = this.authorizedClientService.loadAuthorizedClient(
                this.clientRegistrationId,
                this.clientRegistrationId);

        if (oauthClient != null) {
            ClientRegistration clientRegistration = oauthClient.getClientRegistration();
            if (!this.isClientCredentialsGrantType(clientRegistration)) {
                throw new UnsupportedOperationException("Only client_credentials grant type is supported");
            }

            if (this.hasTokenExpired(oauthClient)) {
                oauthClient = this.getAuthorizedClient(clientRegistration);
            }
        } else {
            ClientRegistration clientRegistration = this.clientRegistrationRepository.findByRegistrationId(this.clientRegistrationId);
            if (clientRegistration == null) {
                throw new IllegalArgumentException("Could not find ClientRegistration with id " + this.clientRegistrationId);
            }
            if (!this.isClientCredentialsGrantType(clientRegistration)) {
                throw new UnsupportedOperationException("Only client_credentials grant type is supported");
            }
            oauthClient = this.getAuthorizedClient(clientRegistration);
        }

        template.header(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", oauthClient.getAccessToken().getTokenValue()));
    }


    private boolean isClientCredentialsGrantType(ClientRegistration clientRegistration) {
        return AuthorizationGrantType.CLIENT_CREDENTIALS.equals(clientRegistration.getAuthorizationGrantType());
    }

    private OAuth2AuthorizedClient getAuthorizedClient(ClientRegistration clientRegistration) {

        OAuth2ClientCredentialsGrantRequest clientCredentialsGrantRequest = new OAuth2ClientCredentialsGrantRequest(clientRegistration);
        OAuth2AccessTokenResponse tokenResponse = this.clientCredentialsTokenResponseClient.getTokenResponse(clientCredentialsGrantRequest);

        Authentication authentication = new Authentication() {
            @Override
            public Collection<? extends GrantedAuthority> getAuthorities() {
                throw unsupported();
            }

            @Override
            public Object getCredentials() {
                throw unsupported();
            }

            @Override
            public Object getDetails() {
                throw unsupported();
            }

            @Override
            public Object getPrincipal() {
                throw unsupported();
            }

            @Override
            public boolean isAuthenticated() {
                throw unsupported();
            }

            @Override
            public void setAuthenticated(boolean isAuthenticated)
                    throws IllegalArgumentException {
                throw unsupported();
            }

            @Override
            public String getName() {
                return OAuth2FeignRequestInterceptor.this.clientRegistrationId;
            }

            private UnsupportedOperationException unsupported() {
                return new UnsupportedOperationException("Not Supported");
            }
        };

        OAuth2AuthorizedClient authorizedClient = new OAuth2AuthorizedClient(
                clientRegistration,
                authentication.getName(),
                tokenResponse.getAccessToken());

        this.authorizedClientService.saveAuthorizedClient(
                authorizedClient,
                authentication);

        return authorizedClient;
    }

    private boolean hasTokenExpired(OAuth2AuthorizedClient authorizedClient) {
        Instant now = this.clock.instant();
        Instant expiresAt = authorizedClient.getAccessToken().getExpiresAt();
        if (now.isAfter(expiresAt.minus(this.accessTokenExpiresSkew))) {
            return true;
        }
        return false;
    }

}