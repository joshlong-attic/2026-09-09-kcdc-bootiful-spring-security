package com.example.service;

import javax.sql.DataSource;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

class CachingDataSourceInitializer implements DataSourceInitializer {

	private final Map<String, Boolean> initialized = //
			new ConcurrentHashMap<>();

	private final DataSourceInitializer dataSourceInitializer;

	CachingDataSourceInitializer(//
			DataSourceInitializer dataSourceInitializer) {
		this.dataSourceInitializer = dataSourceInitializer;
	}

	@Override
	public DataSource initialize(String tenantId, //
			DataSource dataSource //
	) {
		var initialized = this.initialized //
			.computeIfAbsent(tenantId, _ -> false);
		if (!initialized) {//
			this.dataSourceInitializer //
				.initialize(tenantId, dataSource);
			this.initialized.put(tenantId, true);
		}
		return dataSource;
	}

}
