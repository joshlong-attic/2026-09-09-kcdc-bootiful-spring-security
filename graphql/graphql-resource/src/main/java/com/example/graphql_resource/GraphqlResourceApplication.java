package com.example.graphql_resource;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Objects;

@EnableMethodSecurity (securedEnabled = true)
@SpringBootApplication
public class GraphqlResourceApplication {

    public static void main(String[] args) {
        SpringApplication.run(GraphqlResourceApplication.class, args);
    }

}

@Controller
class MessageController {

    // or @PreAuthorize( "hasRole('SCOPE_openid')") since we're in a resource server
    @PreAuthorize(" isAuthenticated() ")
    @QueryMapping
    Message message() {
        var name = Objects.requireNonNull(SecurityContextHolder
                        .getContextHolderStrategy()
                        .getContext()
                        .getAuthentication())
                .getName();
        return new Message("Hello, " + name + "!");
    }
}

record Message(String message) {
}