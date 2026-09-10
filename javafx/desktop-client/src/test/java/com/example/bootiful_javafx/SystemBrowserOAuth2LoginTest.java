package com.example.bootiful_javafx;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.client.InMemoryOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.OAuth2AuthorizationException;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

class SystemBrowserOAuth2LoginTest {

	private static final ClientRegistration REGISTRATION = ClientRegistration.withRegistrationId("javafx")
		.clientId("javafx")
		.clientAuthenticationMethod(ClientAuthenticationMethod.NONE)
		.authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
		.redirectUri("http://127.0.0.1:8385/login/oauth2/code/javafx")
		.scope("openid")
		.authorizationUri("http://localhost:9090/oauth2/authorize")
		.tokenUri("http://localhost:9090/oauth2/token")
		.jwkSetUri("http://localhost:9090/oauth2/jwks")
		.build();

	private final AtomicReference<String> opened = new AtomicReference<>();

	private final SystemBrowserOAuth2Login login = new SystemBrowserOAuth2Login(
			new InMemoryClientRegistrationRepository(REGISTRATION),
			new InMemoryOAuth2AuthorizedClientService(new InMemoryClientRegistrationRepository(REGISTRATION)),
			this.opened::set, _ -> {
			});

	@Test
	void asksForACodeAndProtectsItWithPkce() throws Exception {
		this.login.start("javafx");
		var parameters = UriComponentsBuilder.fromUriString(this.opened.get())
			.build()
			.getQueryParams()
			.toSingleValueMap();
		assertThat(parameters).containsEntry("response_type", "code")
			.containsEntry("client_id", "javafx")
			.containsEntry("code_challenge_method", "S256")
			.containsKeys("state", "code_challenge");
	}

}
