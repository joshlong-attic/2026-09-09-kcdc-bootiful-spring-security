package com.example.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.gateway.server.mvc.filter.BeforeFilterFunctions;
import org.springframework.cloud.gateway.server.mvc.filter.TokenRelayFilterFunctions;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
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
                .route(RequestPredicates.path("/**").and(RequestPredicates.path("/error").negate()), http())
                .before(BeforeFilterFunctions.uri("http://localhost:8020"))
                .build();
    }

    @Order(Ordered.HIGHEST_PRECEDENCE)
    @Bean
    RouterFunction<ServerResponse> api() {
        return route()
                .route(RequestPredicates.path("/api/**"), http())
                .before(BeforeFilterFunctions.uri("http://localhost:8081"))
                .before(BeforeFilterFunctions.rewritePath("/api/(?<segment>.*)", "/${segment}"))
                .filter(TokenRelayFilterFunctions.tokenRelay())
                .build();
    }

    @Bean
    Customizer<HttpSecurity> httpSecurityCustomizer() {
        return http -> http.csrf(AbstractHttpConfigurer::disable);
    }

  /*  @Bean
    Customizer<HttpSecurity> httpSecurityCustomizer() throws Exception {
        return http -> {
            var csrfHandler = new CsrfTokenRequestAttributeHandler();
            csrfHandler.setCsrfRequestAttributeName(null); // eager load, so the cookie actually gets written
            http.csrf(c -> c
                    .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                    .csrfTokenRequestHandler(csrfHandler));
        };
       }
    */


}
