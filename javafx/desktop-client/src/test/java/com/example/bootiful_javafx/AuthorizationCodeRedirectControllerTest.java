package com.example.bootiful_javafx;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

// DEFINED_PORT: the point of the embedded Tomcat is that it is listening on the port and path the
// redirect URI names, so this test arrives the way the browser will - over a socket. The endpoints
// are spelled out rather than discovered, so that loading the context does not depend on the
// authorization server being up.
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT, properties = {
		"spring.security.oauth2.client.registration.javafx.provider=static",
		"spring.security.oauth2.client.provider.static.authorization-uri=http://localhost:9090/oauth2/authorize",
		"spring.security.oauth2.client.provider.static.token-uri=http://localhost:9090/oauth2/token",
		"spring.security.oauth2.client.provider.static.user-info-uri=http://localhost:9090/userinfo",
		"spring.security.oauth2.client.provider.static.jwk-set-uri=http://localhost:9090/oauth2/jwks",
		"spring.security.oauth2.client.provider.static.user-name-attribute=sub" })
class AuthorizationCodeRedirectControllerTest {

	// the token exchange on the other side of this needs an authorization server; what is
	// being
	// tested here is the trip from the browser to it, and the page that comes back.
	@MockitoBean
	private SystemBrowserOAuth2Login login;

	@Value("${spring.security.oauth2.client.registration.javafx.redirect-uri}")
	private URI redirectUri;

	@Test
	void handsTheAuthorizationResponseOverAndRendersThePage() throws Exception {
		given(this.login.finish(eq("javafx"), anyMap())).willReturn(signedIn("jlong"));
		try (var http = HttpClient.newHttpClient()) {
			var response = http.send(get(this.redirectUri + "?code=abc&state=xyz%2F1"),
					HttpResponse.BodyHandlers.ofString());
			assertThat(response.statusCode()).isEqualTo(200);
			assertThat(response.body()).contains("You're signed in, jlong.");
		}
		// the registration id comes off the path, the code and state out of the query -
		// decoded
		verify(this.login).finish("javafx", Map.of("code", "abc", "state", "xyz/1"));
	}

	@Test
	void isTheOnlyThingThisAppServes() throws Exception {
		try (var http = HttpClient.newHttpClient()) {
			assertThat(http
				.send(get(this.redirectUri.resolve("/favicon.ico").toString()), HttpResponse.BodyHandlers.ofString())
				.statusCode()).isEqualTo(404);
		}
	}

	private static UserSignedInEvent signedIn(String username) {
		var idToken = OidcIdToken.withTokenValue("id-token")
			.issuedAt(Instant.now())
			.expiresAt(Instant.now().plusSeconds(60))
			.claim("sub", "0f5b8a2e")
			.claim("preferred_username", username)
			.build();
		var user = new DefaultOidcUser(List.of(), idToken);
		return new UserSignedInEvent(new OAuth2AuthenticationToken(user, user.getAuthorities(), "javafx"));
	}

	private static HttpRequest get(String uri) {
		return HttpRequest.newBuilder(URI.create(uri)).GET().build();
	}

}
