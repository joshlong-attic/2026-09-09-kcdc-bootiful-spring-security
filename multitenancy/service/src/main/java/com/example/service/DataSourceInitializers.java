package com.example.service;

public class DataSourceInitializers {

	// <.>
	public static DataSourceInitializer caching(DataSourceInitializer initializer) {
		return new CachingDataSourceInitializer(initializer);
	}

}
