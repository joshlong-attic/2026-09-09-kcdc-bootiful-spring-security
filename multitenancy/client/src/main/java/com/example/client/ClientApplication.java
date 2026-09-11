package com.example.client;

import io.arconia.multitenancy.web.context.resolvers.OAuth2TenantResolver;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.annotation.RegisteredOAuth2AuthorizedClient;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.client.RestClient;

@SpringBootApplication
public class ClientApplication {

    public static void main(String[] args) {
        SpringApplication.run(ClientApplication.class, args);
    }

    @Bean
    OAuth2TenantResolver oAuth2TenantResolver() {
        return OAuth2TenantResolver
                .builder()
                .tenantClaimName("tenant")
                .build();
    }

}

@Controller
@ResponseBody
class ClientController {

    private final RestClient http;

    ClientController(RestClient.Builder http) {
        this.http = http.build();
    }

    @GetMapping("/")
    TenantInfo me(@RegisteredOAuth2AuthorizedClient("spring") OAuth2AuthorizedClient auth2AuthorizedClient) {
        return this.http
                .get()
                .uri("http://localhost:8081")
                .headers(h -> h
                        .setBearerAuth(auth2AuthorizedClient.getAccessToken()
                               .getTokenValue()))
                .retrieve()
                .body(TenantInfo.class);
    }
}

record TenantInfo(String tenant, String user) {
}