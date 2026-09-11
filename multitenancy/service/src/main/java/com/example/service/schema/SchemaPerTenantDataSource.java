package com.example.service.schema;

import com.example.service.DataSourceInitializer;
import com.example.service.DataSourceInitializers;
import io.arconia.multitenancy.core.context.TenantContext;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.DelegatingDataSource;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

class SchemaPerTenantDataSource extends DelegatingDataSource {

	private final DataSourceInitializer dataSourceInitializer;

	// <.>
	SchemaPerTenantDataSource(DataSource dataSource, DataSourceInitializer dbi) {
		this.dataSourceInitializer = DataSourceInitializers.caching((tenantId, dataSource1) -> {
			// <.>
			var jdbc = JdbcClient.create(dataSource1);
			jdbc.sql("create schema if not exists " + schema()).update();
			return dbi.initialize(tenantId, dataSource1);
		});
		this.setTargetDataSource(dataSource);
	}

	// <.>
	private String schema() {
		return "schema_" + TenantContext.getTenantIdentifier();
	}

	// <.>
	protected Connection initializeUnderlyingConnection(Connection connection) throws SQLException {
		var tenantIdentifier = TenantContext.getTenantIdentifier();
		connection.setSchema(schema());
		var singleConnectionDataSource = new SingleConnectionDataSource(connection, true);
		this.dataSourceInitializer.initialize(tenantIdentifier, singleConnectionDataSource);
		return connection;
	}

	@Override
	public Connection getConnection(String username, String password) throws SQLException {
		return this.initializeUnderlyingConnection(super.getConnection(username, password));
	}

	@Override
	public Connection getConnection() throws SQLException {
		return this.initializeUnderlyingConnection(super.getConnection());
	}

}
