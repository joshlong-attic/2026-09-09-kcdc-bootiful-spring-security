package com.example.bootiful_javafx;

import org.springframework.security.oauth2.client.annotation.ClientRegistrationId;
import org.springframework.web.service.annotation.GetExchange;

import static com.example.bootiful_javafx.StageInitializer.CLIENT_REGISTRATION_ID;

@ClientRegistrationId(CLIENT_REGISTRATION_ID)
interface UserinfoClient {

	@GetExchange("${bootiful.api-uri}")
	String get();

}
