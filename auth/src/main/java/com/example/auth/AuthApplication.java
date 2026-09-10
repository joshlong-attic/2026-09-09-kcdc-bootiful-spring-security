package com.example.auth;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.ott.OneTimeToken;
import org.springframework.security.authorization.AuthorizationManagerFactories;
import org.springframework.security.authorization.RequiredFactor;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authorization.EnableMultiFactorAuthentication;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.authority.FactorGrantedAuthority;
import org.springframework.security.provisioning.JdbcUserDetailsManager;
import org.springframework.security.web.authentication.ott.OneTimeTokenGenerationSuccessHandler;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.sql.DataSource;
import java.io.IOException;
import java.security.Principal;
import java.time.Duration;
import java.util.Map;

@SpringBootApplication
public class AuthApplication {

    public static void main(String[] args) {
        SpringApplication.run(AuthApplication.class, args);
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

    @Bean
    Customizer<HttpSecurity> authServerConfiguration() {
        return security -> security
                .oauth2AuthorizationServer(a -> a
                        .oidc(Customizer.withDefaults())
                        // NOTE: required for Shell
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

//
//@Controller
//@ResponseBody
//class MeController {
//
//    @GetMapping("/")
//    Map<String, String> me(Principal principal) {
//        return Map.of("name", principal.getName());
//    }
//
//}