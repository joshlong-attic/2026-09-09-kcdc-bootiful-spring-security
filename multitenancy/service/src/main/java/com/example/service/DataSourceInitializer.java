package com.example.service;

import javax.sql.DataSource;

public interface DataSourceInitializer {

	// <.>
	DataSource initialize(String tenantId, DataSource dataSource);

}
