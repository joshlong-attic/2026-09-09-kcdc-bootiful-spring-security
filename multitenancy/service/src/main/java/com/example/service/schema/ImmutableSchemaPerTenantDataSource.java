package com.example.service.schema;

import com.example.service.DataSourceInitializer;
import org.aopalliance.intercept.MethodInterceptor;
import org.springframework.aop.framework.ProxyFactoryBean;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

class ImmutableSchemaPerTenantDataSource extends SchemaPerTenantDataSource {

	ImmutableSchemaPerTenantDataSource(DataSource dataSource, DataSourceInitializer dbi) {
		super(dataSource, dbi);
	}

	@Override
	protected Connection initializeUnderlyingConnection(Connection connection) throws SQLException {
		var initialized = super.initializeUnderlyingConnection(connection);
		return this.buildImmutableSchemaConnection(initialized);
	}

	// <.>
	private Connection buildImmutableSchemaConnection(Connection connection) {
		var pfb = new ProxyFactoryBean();
		pfb.addInterface(Connection.class);
		for (var i : connection.getClass().getInterfaces())
			pfb.addInterface(i);
		pfb.addAdvice((MethodInterceptor) invocation -> {
			var method = invocation.getMethod();
			if (method.getName().equals("setSchema"))
				return null;
			return invocation.getMethod().invoke(connection, invocation.getArguments());
		});
		pfb.setTarget(connection);
		return (Connection) pfb.getObject();
	}

}
