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
import org.springframework.security.oauth2.core.endpoint.OAuth2AccessTokenResponse;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationExchange;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationResponse;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.endpoint.OidcParameterNames;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.util.Base64;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

@Service
class SystemBrowserOAuth2Login {

	private final StringKeyGenerator state = new Base64StringKeyGenerator(Base64.getUrlEncoder());

	private final RestClientAuthorizationCodeTokenResponseClient accessTokens = new RestClientAuthorizationCodeTokenResponseClient();

	private final OidcIdTokenDecoderFactory idTokens = new OidcIdTokenDecoderFactory();

	private final OidcUserService users = new OidcUserService();

	private final AtomicReference<SignIn> inFlight = new AtomicReference<>();

	private final ClientRegistrationRepository registrations;

	private final OAuth2AuthorizedClientService authorizedClients;

	private final AuthorizationBrowser browser;

	private final ApplicationEventPublisher events;

	SystemBrowserOAuth2Login(ClientRegistrationRepository registrations,
			OAuth2AuthorizedClientService authorizedClients, AuthorizationBrowser browser,
			ApplicationEventPublisher events) {
		this.registrations = registrations;
		this.authorizedClients = authorizedClients;
		this.browser = browser;
		this.events = events;
	}

	// hands back a promise of the tokens; `finish` keeps that promise
	CompletableFuture<OAuth2AuthorizedClient> start(String registrationId) {
		var registration = this.registrations.findByRegistrationId(registrationId);
		var builder = OAuth2AuthorizationRequest.authorizationCode()
			.clientId(registration.getClientId())
			.authorizationUri(registration.getProviderDetails().getAuthorizationUri())
			.redirectUri(registration.getRedirectUri())
			.scopes(registration.getScopes())
			.state(this.state.generateKey());
		// a desktop app cannot keep a client secret, so PKCE is what proves
		// the code is being redeemed by the app that asked for it
		OAuth2AuthorizationRequestCustomizers.withPkce().accept(builder);
		var signIn = new SignIn(builder.build(), new CompletableFuture<>());
		this.inFlight.set(signIn);
		this.browser.open(signIn.request().getAuthorizationRequestUri());
		return signIn.tokens();
	}

	UserSignedInEvent finish(String registrationId, Map<String, String> parameters) {
		var signIn = this.inFlight.getAndSet(null);
		Assert.state(signIn != null, "there is no sign-in waiting for a code");
		var returnedState = parameters.get(OAuth2ParameterNames.STATE);
		Assert.state(Objects.equals(signIn.request().getState(), returnedState), "the state parameter does not match");
		var response = OAuth2AuthorizationResponse.success(parameters.get(OAuth2ParameterNames.CODE))
			.redirectUri(Objects.requireNonNull(signIn.request().getRedirectUri()))
			.state(returnedState)
			.build();
		var registration = this.registrations.findByRegistrationId(registrationId);
		var authentication = exchange(registration, new OAuth2AuthorizationExchange(signIn.request(), response));
		signIn.tokens().complete(this.authorizedClients.loadAuthorizedClient(registrationId, authentication.getName()));
		var event = new UserSignedInEvent(authentication);
		this.events.publishEvent(event);
		return event;
	}

	private OAuth2AuthenticationToken exchange(ClientRegistration registration, OAuth2AuthorizationExchange exchange) {
		var tokens = this.accessTokens
			.getTokenResponse(new OAuth2AuthorizationCodeGrantRequest(registration, exchange));
		var request = new OidcUserRequest(registration, tokens.getAccessToken(), idToken(registration, tokens),
				tokens.getAdditionalParameters());
		var user = this.users.loadUser(request);
		var authentication = new OAuth2AuthenticationToken(user, user.getAuthorities(),
				registration.getRegistrationId());

		// give the tokens to Spring Security, which will refresh them from here on out
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

	private record SignIn(OAuth2AuthorizationRequest request, CompletableFuture<OAuth2AuthorizedClient> tokens) {
	}

}
