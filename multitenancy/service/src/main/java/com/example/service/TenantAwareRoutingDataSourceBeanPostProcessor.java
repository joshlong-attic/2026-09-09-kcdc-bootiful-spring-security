package com.example.service;

import org.jspecify.annotations.Nullable;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;

import javax.sql.DataSource;
import java.util.function.Supplier;

class TenantAwareRoutingDataSourceBeanPostProcessor implements BeanPostProcessor {

	private final Supplier<DataSource> dataSourceSupplier;

	TenantAwareRoutingDataSourceBeanPostProcessor(Supplier<DataSource> dataSourceSupplier) {
		this.dataSourceSupplier = dataSourceSupplier;
	}

	@Override
	public @Nullable Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
		if (bean instanceof DataSource dataSource) {
			return new TenantAwareRoutingDataSource(dataSource, this.dataSourceSupplier);
		}
		return BeanPostProcessor.super.postProcessAfterInitialization(bean, beanName);
	}

}
