package com.example.graphql_resource;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Objects;

@SpringBootApplication
public class GraphqlResourceApplication {

    public static void main(String[] args) {
        SpringApplication.run(GraphqlResourceApplication.class, args);
    }

}

@Controller
class MessageController {

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