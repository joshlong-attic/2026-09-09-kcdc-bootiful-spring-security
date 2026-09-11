package com.example.auth;

import io.arconia.multitenancy.core.context.TenantContext;
import io.arconia.multitenancy.details.jdbc.JdbcTenantDetailsService;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenCustomizer;
import org.springframework.security.provisioning.JdbcUserDetailsManager;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.sql.DataSource;
import java.util.Map;
import java.util.Objects;

@SpringBootApplication
public class AuthApplication {

    public static void main(String[] args) {
        SpringApplication.run(AuthApplication.class, args);
    }

    @Bean
    Customizer<HttpSecurity> customizer() {
        return http ->
                http.oauth2AuthorizationServer(a -> a
                        .oidc(Customizer.withDefaults()));
    }

    @Bean
    JdbcUserDetailsManager jdbcUserDetailsManager(DataSource dataSource) {
        var u = new JdbcUserDetailsManager(dataSource);
        u.setEnableUpdatePassword(true);
        return u;
    }

    @Bean
    JdbcTenantDetailsService jdbcTenantDetails(DataSource dataSource) {
        return JdbcTenantDetailsService
                .builder()
                .dataSource(dataSource)
                .build();
    }
}

@Component
class TenantOAuth2TokenCustomizer implements OAuth2TokenCustomizer<JwtEncodingContext> {

    private final JdbcClient db;

    TenantOAuth2TokenCustomizer(JdbcClient db) {
        this.db = db;
    }

    @Override
    public void customize(JwtEncodingContext context) {
        var tenant = db
                .sql("""
                            select tenant_details_identifier from 
                           users_tenant_details utd  where users_username = ? 
                        """)
                .params(context.getPrincipal().getName())
                .query((rs, rowNum) -> rs.getString("tenant_details_identifier"))
                .single();
        context.getClaims().claim("tenant", tenant);
    }
}


