package com.prime.userservice;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.stereotype.Component;

@SpringBootApplication
public class UserServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(UserServiceApplication.class, args);
	}
}

@Component
class MyRunner implements CommandLineRunner {

    @Value("${spring.datasource.url}")
    private String dbUrl;
    @Override
    public void run(String... args) throws Exception {
        System.out.println("Database URL: " + dbUrl);
    }
}