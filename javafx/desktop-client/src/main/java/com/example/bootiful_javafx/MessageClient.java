package com.example.bootiful_javafx;

import org.springframework.security.oauth2.client.annotation.ClientRegistrationId;
import org.springframework.web.service.annotation.GetExchange;

@ClientRegistrationId("javafx")
interface MessageClient {

	@GetExchange("http://localhost:8081/message")
	Message message();

}

record Message(String message) {
}
