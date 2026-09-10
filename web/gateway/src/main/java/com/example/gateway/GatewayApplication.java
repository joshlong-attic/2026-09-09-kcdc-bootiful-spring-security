package com.example.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.gateway.server.mvc.filter.BeforeFilterFunctions;
import org.springframework.cloud.gateway.server.mvc.filter.TokenRelayFilterFunctions;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.servlet.function.RequestPredicate;
import org.springframework.web.servlet.function.RequestPredicates;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerResponse;

import static org.springframework.cloud.gateway.server.mvc.handler.GatewayRouterFunctions.route;
import static org.springframework.cloud.gateway.server.mvc.handler.HandlerFunctions.http;

@SpringBootApplication
public class GatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }

    @Order(Ordered.LOWEST_PRECEDENCE)
    @Bean
    RouterFunction<ServerResponse> ui() {
        return route()
                .route(RequestPredicates.path("/**"), http())
                .before(BeforeFilterFunctions.uri("http://localhost:8020"))
                .build();
    }

    @Order(Ordered.HIGHEST_PRECEDENCE)
    @Bean
    RouterFunction<ServerResponse> api() {
        return route()
                .route(RequestPredicates.path("/api/**"), http())
                .before(BeforeFilterFunctions.uri("http://localhost:8081"))
                .before(BeforeFilterFunctions.rewritePath("/api", "/"))
                .filter(TokenRelayFilterFunctions.tokenRelay())
                .build();
    }
}
