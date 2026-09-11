package com.example.service;

import io.arconia.multitenancy.core.context.TenantContext;
import io.arconia.multitenancy.web.context.resolvers.OAuth2TenantResolver;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Map;

@SpringBootApplication
public class ServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ServiceApplication.class, args);
    }

    @Bean
    OAuth2TenantResolver oauth2TenantResolver() {
        return OAuth2TenantResolver
                .builder()
                 .tenantClaimName("tenant")
                .build();
    }

}

@Controller
@ResponseBody
class TenantController {

    @GetMapping("/")
    Map<String, String> me() {
        return Map.of("tenant", TenantContext.getTenantIdentifier(),
                "user", SecurityContextHolder.getContextHolderStrategy()
                        .getContext()
                        .getAuthentication()
                        .getName());
    }
}