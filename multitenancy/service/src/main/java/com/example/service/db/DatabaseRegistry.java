package com.example.service.db;

import org.jspecify.annotations.Nullable;
import org.springframework.boot.jdbc.autoconfigure.JdbcConnectionDetails;

interface DatabaseRegistry {

	@Nullable JdbcConnectionDetails getConnectionDetails(String tenantId);

}
