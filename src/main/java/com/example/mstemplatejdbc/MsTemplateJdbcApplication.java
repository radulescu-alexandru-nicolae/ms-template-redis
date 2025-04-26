package com.example.mstemplatejdbc;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class MsTemplateJdbcApplication {

	public static void main(String[] args) {
		SpringApplication.run(MsTemplateJdbcApplication.class, args);
	}

}
