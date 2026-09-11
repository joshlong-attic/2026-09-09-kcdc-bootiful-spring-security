package com.example.service.db;

import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.transport.DockerHttpClient;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.jdbc.autoconfigure.JdbcConnectionDetails;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

class DockerDatabaseRegistry implements DatabaseRegistry, InitializingBean {

	private final DockerClientConfig dockerClientConfig;

	private final Map<String, String> mapping = new ConcurrentHashMap<>();

	private final DockerHttpClient dockerHttpClient;

	DockerDatabaseRegistry(DockerHttpClient dockerHttpClient, DockerClientConfig dockerClientConfig) {
		this.dockerClientConfig = dockerClientConfig;
		this.dockerHttpClient = dockerHttpClient;
	}

	@Nullable
	@Override
	public JdbcConnectionDetails getConnectionDetails(String tenantId) {
		if (this.mapping.containsKey(tenantId)) {
			return new JdbcConnectionDetails() {
				@Override
				public String getUsername() {
					return "myuser";
				}

				@Override
				public String getPassword() {
					return "secret";
				}

				@Override
				public String getJdbcUrl() {
					return mapping.get(tenantId);
				}
			};
		}
		return null;
	}

	private void refresh() throws Exception {

		// <.>
		try (var dockerClient = DockerClientImpl.getInstance(this.dockerClientConfig, this.dockerHttpClient)) {
			var containers = dockerClient.listContainersCmd()
				.withShowAll(false)
				.withLabelFilter(List.of("com.docker.compose.service"))
				.exec();
			for (var container : containers) {
				var image = container.getImage();
				if (image == null || !image.contains("postgres")) {
					continue;
				}
				var labels = container.getLabels();
				var serviceName = labels.get("com.docker.compose.service");
				var ports = container.getPorts();
				for (var port : ports) {
					this.mapping.put(serviceName,
							"jdbc:postgresql://localhost:" + port.getPublicPort() + "/" + serviceName);
				}
			}
		}
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		this.refresh();
	}

}
