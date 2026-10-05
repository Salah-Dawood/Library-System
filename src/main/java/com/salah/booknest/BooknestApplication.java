package com.salah.booknest;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class BooknestApplication {

	public static void main(String[] args) {
		SpringApplication.run(BooknestApplication.class, args);
	}

}
