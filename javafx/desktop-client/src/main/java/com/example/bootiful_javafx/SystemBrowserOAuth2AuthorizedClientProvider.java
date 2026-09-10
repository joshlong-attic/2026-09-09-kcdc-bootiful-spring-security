package com.example.bootiful_javafx;

import org.springframework.context.event.EventListener;
import org.springframework.security.oauth2.client.OAuth2AuthorizationContext;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProvider;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2AuthorizationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

@Component
class SystemBrowserOAuth2AuthorizedClientProvider implements OAuth2AuthorizedClientProvider {

	private static final Duration CLOCK_SKEW = Duration.ofSeconds(60);

	private final BlockingQueue<UserSignedInEvent> signIns = new ArrayBlockingQueue<>(1);

	private final SystemBrowserOAuth2Login login;

	private final OAuth2AuthorizedClientService authorizedClients;

	private final Duration timeout = Duration.ofMinutes(2);

	SystemBrowserOAuth2AuthorizedClientProvider(SystemBrowserOAuth2Login login,
			OAuth2AuthorizedClientService authorizedClients) {
		this.login = login;
		this.authorizedClients = authorizedClients;
	}

	@EventListener
	void on(UserSignedInEvent event) {
		this.signIns.offer(event);
	}

	@Override
	public OAuth2AuthorizedClient authorize(OAuth2AuthorizationContext context) {
		var registration = context.getClientRegistration();
		var current = context.getAuthorizedClient();
		if (!AuthorizationGrantType.AUTHORIZATION_CODE.equals(registration.getAuthorizationGrantType())
				|| (current != null && !expired(current.getAccessToken()))) {
			return null;
		}
		try {
			// whoever signed in before this call did not do it in answer to this call
			this.signIns.clear();
			this.login.start(registration.getRegistrationId());
			var event = this.signIns.poll(this.timeout.toMillis(), TimeUnit.MILLISECONDS);
			if (event == null) {
				throw new OAuth2AuthorizationException(new OAuth2Error("browser_login_timed_out"));
			}
			return this.authorizedClients.loadAuthorizedClient(registration.getRegistrationId(),
					event.authentication().getName());
		} //
		catch (InterruptedException ie) {
			Thread.currentThread().interrupt();
			throw new OAuth2AuthorizationException(new OAuth2Error("browser_login_interrupted"));
		}
	}

	private static boolean expired(OAuth2AccessToken token) {
		var expiresAt = token.getExpiresAt();
		return expiresAt != null && Instant.now().isAfter(expiresAt.minus(CLOCK_SKEW));
	}

}
