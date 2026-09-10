package com.example.bootiful_javafx;

import org.springframework.context.ApplicationEvent;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.util.StringUtils;

import java.util.Objects;

class UserSignedInEvent extends ApplicationEvent {

	UserSignedInEvent(OAuth2AuthenticationToken stage) {
		super(stage);
	}

	OidcUser user() {
		return (OidcUser) authentication().getPrincipal();
	}

	String name() {
		return StringUtils.hasText(user().getPreferredUsername())
				? Objects.requireNonNull(user().getPreferredUsername()) : user().getName();
	}

	OAuth2AuthenticationToken authentication() {
		return (OAuth2AuthenticationToken) getSource();
	}

}
