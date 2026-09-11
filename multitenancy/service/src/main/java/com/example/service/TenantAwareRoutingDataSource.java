package com.example.service;

import io.arconia.multitenancy.core.context.TenantContext;
import org.aopalliance.intercept.MethodInterceptor;
import org.springframework.aop.framework.ProxyFactoryBean;
import org.springframework.jdbc.datasource.DelegatingDataSource;
import org.springframework.util.StringUtils;

import javax.sql.DataSource;
import javax.sql.XADataSource;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

class TenantAwareRoutingDataSource extends DelegatingDataSource {

	TenantAwareRoutingDataSource(DataSource primary, Supplier<DataSource> multitenantDataSource) {
		var resolved = new AtomicReference<DataSource>();
		var pfb = new ProxyFactoryBean();
		pfb.addInterface(XADataSource.class);
		pfb.addInterface(DataSource.class);
		pfb.addAdvice((MethodInterceptor) invocation -> {
			resolved.compareAndSet(null, multitenantDataSource.get());// <.>
			var tenantId = TenantContext.getTenantIdentifier();
			var db = StringUtils.hasText(tenantId) ? resolved.get() : primary;
			return invocation.getMethod().invoke(db, invocation.getArguments());
		});
		var dataSource = (DataSource) pfb.getObject();
		this.setTargetDataSource(dataSource);
	}

}
