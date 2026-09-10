package com.example.bootiful_javafx;

import org.springframework.security.oauth2.client.annotation.ClientRegistrationId;
import org.springframework.web.service.annotation.GetExchange;

@ClientRegistrationId(StageInitializer.CLIENT_REGISTRATION_ID)
interface MessageClient {

    @GetExchange("http://localhost:8081/message")
    Message message();
}
