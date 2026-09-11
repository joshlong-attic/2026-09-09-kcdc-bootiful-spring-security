package com.example.service;

import io.arconia.multitenancy.web.context.resolvers.OAuth2TenantResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class ArconiaConfiguration {

	// <.>
	@Bean
	OAuth2TenantResolver oauth2TenantResolver() {
		return OAuth2TenantResolver.builder().tenantClaimName("tenant").build();
	}

}
