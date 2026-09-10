package com.example.bootiful_javafx;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

// the endpoints are spelled out rather than discovered, so that loading the context does not
// depend on the authorization server being up.
@SpringBootTest(properties = { "spring.security.oauth2.client.registration.javafx.provider=static",
		"spring.security.oauth2.client.provider.static.authorization-uri=http://localhost:9090/oauth2/authorize",
		"spring.security.oauth2.client.provider.static.token-uri=http://localhost:9090/oauth2/token",
		"spring.security.oauth2.client.provider.static.user-info-uri=http://localhost:9090/userinfo",
		"spring.security.oauth2.client.provider.static.jwk-set-uri=http://localhost:9090/oauth2/jwks",
		"spring.security.oauth2.client.provider.static.user-name-attribute=sub" })
class DesktopApplicationTests {

	@Test
	void contextLoads() {
	}

}
