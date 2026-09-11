package com.example.amqp_oauth_client;

import org.jspecify.annotations.NonNull;
import org.springframework.amqp.core.*;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.amqp.dsl.Amqp;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.dsl.DirectChannelSpec;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.MessageChannelSpec;
import org.springframework.integration.dsl.MessageChannels;
import org.springframework.integration.json.ObjectToJsonTransformer;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.annotation.RegisteredOAuth2AuthorizedClient;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.service.annotation.GetExchange;

import java.time.Instant;
import java.util.Map;

@SpringBootApplication
public class AmqpOauthClientApplication {

    public static void main(String[] args) {
        SpringApplication.run(AmqpOauthClientApplication.class, args);
    }

}

@Controller
@ResponseBody
class ProducerController {

    private final MessageChannel outbound;

    ProducerController(@Qualifier(ProducerConfiguration.MESSAGES) MessageChannel outbound) {
        this.outbound = outbound;
    }

    @GetExchange("/bad")
    boolean send() {
        var message = MessageBuilder
                .withPayload("you've been pwned! (" + Instant.now() + ")")
                .build();
        return this.outbound.send(message);
    }

    @GetExchange("/good")
    boolean send(@RegisteredOAuth2AuthorizedClient("messages") OAuth2AuthorizedClient client) {
        var mh = new MessageHeaders(Map.of("messages-token", client.getAccessToken().getTokenValue()));
        var message = MessageBuilder
                .createMessage("Hello, " + client.getPrincipalName() + "! (" +
                        Instant.now() + ")", mh);
        return this.outbound.send(message);
    }

}

@Configuration
class ProducerConfiguration {

    static final String MESSAGES = "messages";

    @Bean
    IntegrationFlow outboundMessageIntegrationFlow(
            AmqpTemplate template, @Qualifier(MESSAGES) MessageChannel channel) {
        var amqp = Amqp
                .outboundAdapter(template)
                .exchangeName(MESSAGES)
                .routingKey(MESSAGES);
        return IntegrationFlow
                .from(channel)
                .transform(new ObjectToJsonTransformer())
                .handle(amqp)
                .get();
    }


    @Bean
    InitializingBean applicationRunner (Binding binding , 
                                       Exchange e, Queue queue, AmqpAdmin amqpAdmin) {
        return new InitializingBean() {
            @Override
            public void afterPropertiesSet() throws Exception {
                amqpAdmin.declareBinding(binding);
            }
        } ;
    }
    
    @Bean
    Queue messagesQueue() {
        return QueueBuilder.durable(MESSAGES).build();
    }

    @Bean
    Binding messagesBinding() {
        return BindingBuilder.bind(messagesQueue()).to(exchange()).with(MESSAGES).noargs();
    }

    @Bean
    Exchange exchange() {
        return ExchangeBuilder.directExchange(MESSAGES).build();
    }

    @Bean(MESSAGES)
    MessageChannelSpec<DirectChannelSpec, DirectChannel> emails() {
        return MessageChannels.direct();
    }

}