package com.example.cli;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.shell.core.command.annotation.Command;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;

import java.time.Duration;
import java.util.Map;
import java.util.Objects;

@SpringBootApplication
public class CliApplication {

    public static void main(String[] args) {
        SpringApplication.run(CliApplication.class, args);
    }

}

@Component
class Granter {

    private final RestClient http;
    private final ClientRegistration registration;

    Granter(
            InMemoryClientRegistrationRepository repository,
            RestClient.Builder http) {
        this.registration = repository.findByRegistrationId("messages");
        this.http = http
                .defaultHeaders(headers -> {
                    headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
                    headers.setBasicAuth(this.registration.getClientId(), this.registration.getClientSecret());
                })
                .build();
    }

    Grant grant() throws Exception {
        var json = this.http
                .post()
                .uri(this.registration.getProviderDetails().getConfigurationMetadata()
                        .get("device_authorization_endpoint") + "")
                .body(MultiValueMap.fromSingleValue(
                        Map.of("scope", String.join(" ", this.registration.getScopes()),
                                "client_id", this.registration.getClientId())
                ))
                .retrieve()
                .body(Map.class);
        var uri = json.get("verification_uri_complete") + "";
        var deviceCode = json.get("device_code") + "";
        IO.println("please go to " + uri);
        while (true) {
            try {
                Thread.sleep(Duration.ofSeconds(5));
                return new Grant(uri, this.token(deviceCode));
            } catch (Throwable throwable) {
                // meh
            }
        }
    }

    private String token(String deviceCode) {
        var tokenJson = http.post()
                .uri(this.registration.getProviderDetails().getTokenUri())
                .body(MultiValueMap.fromSingleValue(Map.of(
                        "client_id", this.registration.getClientId(), "device_code", deviceCode,
                        "grant_type", AuthorizationGrantType.DEVICE_CODE.getValue())))
                .retrieve()
                .body(JsonNode.class);
        return tokenJson.get("access_token").asString();
    }
}

record Grant(String verificationUri, String accessToken) {
}

@Component
class ShellClientComponent {

    private final RestClient http;
    private final Granter granter;

    ShellClientComponent(RestClient.Builder http, Granter granter) {
        this.http = http.build();
        this.granter = granter;
    }

    @Command(description = "get a secured message")
    String message() throws Exception {
        var at = this.granter.grant().accessToken();
        return Objects.requireNonNull(this.http
                        .get()
                        .uri("http://localhost:8081/message")
                        .headers(h -> h.setBearerAuth(at))
                        .retrieve()
                        .body(Message.class))
                .message();
    }

    record Message(String message) {
    }
}
