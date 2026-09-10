package com.example.amqp_resource;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.integration.amqp.dsl.Amqp;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.core.GenericHandler;
import org.springframework.integration.dsl.DirectChannelSpec;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.MessageChannelSpec;
import org.springframework.integration.dsl.MessageChannels;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authorization.AuthenticatedAuthorizationManager;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.messaging.access.intercept.AuthorizationChannelInterceptor;
import org.springframework.security.messaging.context.SecurityContextChannelInterceptor;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.BearerTokenAuthenticationToken;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationProvider;
import org.springframework.util.Assert;

@SpringBootApplication
public class AmqpResourceApplication {

    public static void main(String[] args) {
        System.setProperty("spring.amqp.deserialization.trust.all", "true");
        SpringApplication.run(AmqpResourceApplication.class, args);
    }

    @Bean
    DirectChannelSpec requests(JwtAuthenticationProvider jwtAuthenticationProvider) {
        var headerName = "messages-token";
        var jwtAuthInterceptor = new JwtAuthenticationInterceptor(headerName, jwtAuthenticationProvider);
        var securityContextChannelInterceptor = new SecurityContextChannelInterceptor(headerName);
        var authorizationChannelInterceptor = new AuthorizationChannelInterceptor(
                AuthenticatedAuthorizationManager.authenticated());
        return MessageChannels
                .direct()
                .interceptor(
                        jwtAuthInterceptor,
                        securityContextChannelInterceptor,
                        authorizationChannelInterceptor
                );
    }

    @Bean
    JwtAuthenticationProvider jwtAuthenticationProvider(JwtDecoder decoder) {
        return new JwtAuthenticationProvider(decoder);
    }

    @Bean
    JwtDecoder jwtDecoder(@Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}") String issuerUri) {
        return NimbusJwtDecoder.withIssuerLocation(issuerUri).build();
    }

    @Bean
    JwtAuthenticationConverter jwtAuthenticationConverter() {
        return new JwtAuthenticationConverter();
    }

    @Bean
    IntegrationFlow securedFlow(MessageChannel requests) {
        return IntegrationFlow
                .from(requests)
                .handle((payload, headers) -> {
                    IO.println("got the message: " + payload + " with headers [" + headers + "]");
                    return null;
                })
                .get();
    }

    @Bean
    IntegrationFlow inbound(MessageChannel requests, ConnectionFactory connectionFactory) {
        return IntegrationFlow
                .from(Amqp.inboundAdapter(connectionFactory, "messages"))
                .channel(requests)
                .get();
    }
}


class JwtAuthenticationInterceptor implements ChannelInterceptor {

    private final JwtAuthenticationProvider authenticationProvider;

    private final String headerName;

    JwtAuthenticationInterceptor(String headerName, JwtAuthenticationProvider ap) {
        this.headerName = headerName;
        this.authenticationProvider = ap;
    }

    @Override
    public org.springframework.messaging.Message<?> preSend(
            org.springframework.messaging.Message<?> message, @NonNull MessageChannel channel) {
        var token = (String) message.getHeaders().get(headerName);
        Assert.hasText(token, "the token must be non-empty!");
        var authentication = this.authenticationProvider
                .authenticate(new BearerTokenAuthenticationToken(token));
        if (authentication.isAuthenticated()) {
            var upt = UsernamePasswordAuthenticationToken.authenticated(authentication.getName(),
                    null, AuthorityUtils.NO_AUTHORITIES);
            return org.springframework.messaging.support.MessageBuilder
                    .fromMessage(message)
                    .setHeader(headerName, upt)
                    .build();
        }
        return org.springframework.messaging.support.MessageBuilder
                .fromMessage(message)
                .setHeader(headerName, null)
                .build();
    }
}