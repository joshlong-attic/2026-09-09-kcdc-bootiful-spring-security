package com.example.auth;

import io.arconia.multitenancy.details.jdbc.JdbcTenantDetailsService;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.authorization.AuthorizationManagerFactories;
import org.springframework.security.authorization.RequiredFactor;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenCustomizer;
import org.springframework.security.provisioning.JdbcUserDetailsManager;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.sql.DataSource;
import java.time.Duration;
import java.util.Objects;

@SpringBootApplication
public class AuthApplication {

    public static void main(String[] args) {
        SpringApplication.run(AuthApplication.class, args);
    }

}

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
        IO.println("the tenant is "  + tenant);
        context.getClaims().claim("tenant_id", tenant);
    }
}

@Configuration
class MultitenancyConfiguration {
    
    @Bean
    TenantOAuth2TokenCustomizer tenantOAuth2TokenCustomizer(JdbcClient jdbcClient) {
        return new TenantOAuth2TokenCustomizer(jdbcClient);
    }

    @Bean
    JdbcTenantDetailsService jdbcTenantDetails(DataSource dataSource) {
        return JdbcTenantDetailsService
                .builder()
                .dataSource(dataSource)
                .build();
    }
}

@Configuration
//@EnableMultiFactorAuthentication( authorities = {
//        FactorGrantedAuthority.OTT_AUTHORITY,
//        FactorGrantedAuthority.PASSWORD_AUTHORITY
//})
class SecurityConfiguration {

    @Bean
    JdbcUserDetailsManager jdbcUserDetailsManager(DataSource dataSource) {
        var u = new JdbcUserDetailsManager(dataSource);
        u.setEnableUpdatePassword(true);
        return u;
    }

   // @Bean
    OAuth2TokenCustomizer<JwtEncodingContext> jwtEncodingContextOAuth2TokenCustomizer() {
        return context -> {
            var isAdmin = context.getPrincipal().getAuthorities()
                    .stream().anyMatch(ga -> {
                        var authority = ga.getAuthority();
                        return Objects
                                .requireNonNull(authority).contains("ROLE_ADMIN");
                    });
            context.getClaims().claim("admin", isAdmin);
        };
    }

    @Bean
    Customizer<HttpSecurity> authServerConfig() {
        return http -> http.oauth2AuthorizationServer(a -> a
                .oidc(Customizer.withDefaults())
                .deviceAuthorizationEndpoint(Customizer.withDefaults())
                .deviceVerificationEndpoint(Customizer.withDefaults())
        );
    }

    // @Bean
    Customizer<HttpSecurity> securityCustomizer() {
        var mfa = AuthorizationManagerFactories
                .multiFactor()
                .requireFactors(b -> b
                        .requireFactor(x -> x.passwordAuthority().validDuration(Duration.ofSeconds(10)))
                        .requireFactor(RequiredFactor.Builder::ottAuthority))
//                .requireFactors(
//                        FactorGrantedAuthority.OTT_AUTHORITY,
//                        FactorGrantedAuthority.PASSWORD_AUTHORITY)
                .build();
        return security -> security
                .authorizeHttpRequests(a -> a
                        .requestMatchers("/admin").access(mfa.authenticated())
                        .anyRequest().authenticated()
                )
                .webAuthn(s -> s.allowedOrigins("http://localhost:8080").rpName("kcdc").rpId("localhost"))
                .oneTimeTokenLogin(ott -> ott
                        .tokenGenerationSuccessHandler((request, response, oneTimeToken) -> {
                            response.getWriter().println("you've got console mail!");
                            response.setContentType(MediaType.TEXT_PLAIN_VALUE);
                            IO.println(oneTimeToken.getUsername() + ", please go to http://localhost:8080/login/ott?token=" + oneTimeToken
                                    .getTokenValue());
                        }));
    }
}

@Controller
@ResponseBody
class DeviceActivatedController {

    @GetMapping("/")
    String home() {
        return "You're all set. Go back to your terminal.";
    }

}