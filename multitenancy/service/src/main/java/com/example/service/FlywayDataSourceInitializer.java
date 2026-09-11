package com.example.service;

import org.flywaydb.core.Flyway;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;

@Component
class FlywayDataSourceInitializer implements DataSourceInitializer {

	@Override
	public DataSource initialize(String tenantId, DataSource dataSource) {

		// <.>
		Flyway.configure(getClass().getClassLoader())
			.dataSource(dataSource)
			.locations("classpath:db/tenants/common", "classpath:db/tenants/" + tenantId + "/")
			.load()
			.migrate();
		return dataSource;
	}

}
