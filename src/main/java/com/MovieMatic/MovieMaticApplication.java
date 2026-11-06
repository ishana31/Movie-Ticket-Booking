package com.MovieMatic;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class MovieMaticApplication {

	public static void main(String[] args) {
		SpringApplication.run(MovieMaticApplication.class, args);
	}

}
