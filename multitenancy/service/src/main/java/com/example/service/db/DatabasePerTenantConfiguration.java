package com.example.service.db;

import com.example.service.DataSourceInitializer;
import com.example.service.DataSourceInitializers;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.transport.DockerHttpClient;
import com.github.dockerjava.zerodep.ZerodepDockerHttpClient;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import javax.sql.DataSource;
import java.util.function.Function;

// <.>
@Profile("db")
@Configuration
class DatabasePerTenantConfiguration {

	@Bean
	DatabasePerTenantDataSource dataSource(//
			DataSourceInitializer dataSourceInitializer, //
			DatabaseRegistry registry) {//

		// <.>
		var initializer = DataSourceInitializers.caching(dataSourceInitializer);

		// <.>
		var dataSourceForTenantFunction = (Function<String, DataSource>) tenantId -> {

			// <.>
			var connectionDetails = registry //
				.getConnectionDetails(tenantId);
			var db = DataSourceBuilder.create()//
				.url(connectionDetails.getJdbcUrl())//
				.username(connectionDetails.getUsername())//
				.password(connectionDetails.getPassword())//
				.type(HikariDataSource.class)//
				.build();
			// <.>
			return initializer.initialize(tenantId, db);
		};
		return new DatabasePerTenantDataSource(dataSourceForTenantFunction);
	}

	// <.>
	@Bean
	DefaultDockerClientConfig dockerClientConfig() {
		return DefaultDockerClientConfig//
			.createDefaultConfigBuilder()
			.build();
	}

	@Bean
	ZerodepDockerHttpClient zerodepDockerHttpClient(DockerClientConfig config) {
		return new ZerodepDockerHttpClient.Builder() //
			.dockerHost(config.getDockerHost())//
			.sslConfig(config.getSSLConfig())//
			.build();
	}

	@Bean
	DockerDatabaseRegistry dockerDatabaseRegistry(DockerHttpClient httpClient, DockerClientConfig config) {
		return new DockerDatabaseRegistry(httpClient, config);
	}

}
