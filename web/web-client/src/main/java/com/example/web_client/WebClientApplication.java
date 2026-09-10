package com.example.web_client;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.annotation.ClientRegistrationId;
import org.springframework.security.oauth2.client.annotation.RegisteredOAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.web.ClientAttributes;
import org.springframework.security.oauth2.client.web.client.OAuth2ClientHttpRequestInterceptor;
import org.springframework.security.oauth2.client.web.client.support.OAuth2RestClientHttpServiceGroupConfigurer;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.client.RestClient;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.registry.ImportHttpServices;

import java.security.Principal;
import java.util.Map;

@SpringBootApplication
public class WebClientApplication {

    public static void main(String[] args) {
        SpringApplication.run(WebClientApplication.class, args);
    }
}

/*
@Component
class MessageClient {

    private final RestClient http;

    MessageClient(RestClient.Builder http, OAuth2AuthorizedClientManager auth2AuthorizedClientManager) {
        this.http = http
                .requestInterceptor(new OAuth2ClientHttpRequestInterceptor(
                        auth2AuthorizedClientManager))
                .build();
    }

    Message message(
            String at
    ) {
        return this.http
                .get()
                .uri("http://localhost:8081/message")
                .attributes(ClientAttributes.clientRegistrationId("messages"))
                // .headers(h -> h.setBearerAuth(at))
                .retrieve()
                .body(Message.class);
    }
}
*/
@Configuration
@ImportHttpServices(MessageClient.class)
class MessageClientConfiguration {

    @Bean
    OAuth2RestClientHttpServiceGroupConfigurer oAuth2RestClientHttpServiceGroupConfigurer(OAuth2AuthorizedClientManager acm) {
        return OAuth2RestClientHttpServiceGroupConfigurer
                .from(acm);
    }
}

@ClientRegistrationId("messages")
interface MessageClient {

    @GetExchange("http://localhost:8081/message")
    Message message();
}

@Controller
@ResponseBody
class MeController {

    private final MessageClient messageClient;

    MeController(MessageClient messageClient) {
        this.messageClient = messageClient;
    }

    @GetMapping("/")
    Message me(@RegisteredOAuth2AuthorizedClient("messages") OAuth2AuthorizedClient client) {
        return this.messageClient.message(
//                client.getAccessToken().getTokenValue()
        );
    }

}

record Message(String message) {
}