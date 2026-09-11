package com.example.service;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Collection;

@Controller
@ResponseBody
class CustomerController {

	private final CustomerRepository repository;

	CustomerController(CustomerRepository repository) {
		this.repository = repository;
	}

	@GetMapping("/customers")
	Collection<Customer> customers() {
		return repository.findAll();
	}

}
