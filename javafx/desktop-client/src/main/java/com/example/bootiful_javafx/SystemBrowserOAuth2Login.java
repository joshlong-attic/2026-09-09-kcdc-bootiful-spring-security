package com.example.bootiful_javafx;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.keygen.Base64StringKeyGenerator;
import org.springframework.security.crypto.keygen.StringKeyGenerator;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.client.endpoint.OAuth2AuthorizationCodeGrantRequest;
import org.springframework.security.oauth2.client.endpoint.RestClientAuthorizationCodeTokenResponseClient;
import org.springframework.security.oauth2.client.oidc.authentication.OidcIdTokenDecoderFactory;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestCustomizers;
import org.springframework.security.oauth2.core.endpoint.*;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.endpoint.OidcParameterNames;
import org.springframework.stereotype.Service;

import java.util.Base64;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

@Service
class SystemBrowserOAuth2Login {

	private final StringKeyGenerator state = new Base64StringKeyGenerator(Base64.getUrlEncoder());

	private final RestClientAuthorizationCodeTokenResponseClient accessTokens = new RestClientAuthorizationCodeTokenResponseClient();

	private final OidcIdTokenDecoderFactory idTokens = new OidcIdTokenDecoderFactory();

	private final OidcUserService users = new OidcUserService();

	private final ClientRegistrationRepository registrations;

	private final OAuth2AuthorizedClientService authorizedClients;

	private final AuthorizationBrowser browser;

	private final ApplicationEventPublisher events;

	private final AtomicReference<OAuth2AuthorizationRequest> inFlight = new AtomicReference<>();

	SystemBrowserOAuth2Login(ClientRegistrationRepository registrations,
			OAuth2AuthorizedClientService authorizedClients, AuthorizationBrowser browser,
			ApplicationEventPublisher events) {
		this.registrations = registrations;
		this.authorizedClients = authorizedClients;
		this.browser = browser;
		this.events = events;
	}

	void start(String registrationId) {
		var registration = this.registrations.findByRegistrationId(registrationId);
		var builder = OAuth2AuthorizationRequest.authorizationCode()
			.clientId(registration.getClientId())
			.authorizationUri(registration.getProviderDetails().getAuthorizationUri())
			.redirectUri(registration.getRedirectUri())
			.scopes(registration.getScopes())
			.state(state.generateKey());
		OAuth2AuthorizationRequestCustomizers.withPkce().accept(builder);
		var request = builder.build();
		this.inFlight.set(request);
		this.browser.open(request.getAuthorizationRequestUri());
	}

	UserSignedInEvent finish(String registrationId, Map<String, String> parameters) {
		var request = this.inFlight.getAndSet(null);
		var state1 = parameters.get(OAuth2ParameterNames.STATE);
		var response = OAuth2AuthorizationResponse.success(parameters.get(OAuth2ParameterNames.CODE))
			.redirectUri(Objects.requireNonNull(request.getRedirectUri()))
			.state(state1)
			.build();
		var exchange = new OAuth2AuthorizationExchange(request, response);
		var event = new UserSignedInEvent(exchange(this.registrations.findByRegistrationId(registrationId), exchange));
		this.events.publishEvent(event);
		return event;
	}

	private OAuth2AuthenticationToken exchange(ClientRegistration registration, OAuth2AuthorizationExchange exchange) {
		var tokens = this.accessTokens
			.getTokenResponse(new OAuth2AuthorizationCodeGrantRequest(registration, exchange));
		var idToken = this.idToken(registration, tokens);
		var oidcUserRequest = new OidcUserRequest(registration, tokens.getAccessToken(), idToken,
				tokens.getAdditionalParameters());
		var user = this.users.loadUser(oidcUserRequest);
		var authentication = new OAuth2AuthenticationToken(user, user.getAuthorities(),
				registration.getRegistrationId());

		// give the token to Spring Security who'll handle refreshing it
		this.authorizedClients.saveAuthorizedClient(new OAuth2AuthorizedClient(registration, user.getName(),
				tokens.getAccessToken(), tokens.getRefreshToken()), authentication);

		var context = SecurityContextHolder.getContextHolderStrategy().createEmptyContext();
		context.setAuthentication(authentication);
		SecurityContextHolder.getContextHolderStrategy().setContext(context);
		return authentication;
	}

	private OidcIdToken idToken(ClientRegistration registration, OAuth2AccessTokenResponse tokens) {
		var value = (String) tokens.getAdditionalParameters().get(OidcParameterNames.ID_TOKEN);
		var jwt = this.idTokens.createDecoder(registration).decode(value);
		return new OidcIdToken(jwt.getTokenValue(), jwt.getIssuedAt(), jwt.getExpiresAt(), jwt.getClaims());
	}

}
