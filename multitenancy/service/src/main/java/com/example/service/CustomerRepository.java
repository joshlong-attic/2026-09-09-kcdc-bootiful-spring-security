package com.example.service;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.util.Collection;

@Repository
class CustomerRepository {

	private final JdbcClient db;

	CustomerRepository(JdbcClient db) {
		this.db = db;
	}

	Collection<Customer> findAll() {
		return this.db//
			.sql("select * from customer") //
			.query((rs, _) -> new Customer(rs.getInt("id"), //
					rs.getString("name")) //
			)//
			.list();
	}

}
