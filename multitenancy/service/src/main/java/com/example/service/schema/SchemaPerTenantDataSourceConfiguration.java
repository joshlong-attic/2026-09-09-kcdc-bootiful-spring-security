package com.example.service.schema;

import com.example.service.DataSourceInitializer;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;
import java.util.concurrent.atomic.AtomicReference;

@Configuration
class SchemaPerTenantDataSourceConfiguration {

	// <.>
	@Bean
	static SchemaPerTenantDataSourceBeanPostProcessor sptBPP() {
		return new SchemaPerTenantDataSourceBeanPostProcessor();
	}

	static class SchemaPerTenantDataSourceBeanPostProcessor implements BeanPostProcessor, BeanFactoryAware {

		private final AtomicReference<ObjectProvider<DataSourceInitializer>> dataSourceInitializer //
				= new AtomicReference<>();

		@Override
		public @Nullable Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
			if (bean instanceof DataSource dataSource) {
				// <.>
				var dsi = this.dataSourceInitializer //
					.get()
					.getIfAvailable();
				return new SchemaPerTenantDataSource(//
						dataSource, dsi);
			}
			return bean;
		}

		@Override
		public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
			var beanProvider = beanFactory //
				.getBeanProvider(DataSourceInitializer.class);
			this.dataSourceInitializer.set(beanProvider);
		}

	}

}
