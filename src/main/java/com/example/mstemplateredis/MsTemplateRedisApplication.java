package com.example.mstemplateredis;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class MsTemplateRedisApplication {

	public static void main(String[] args) {
		SpringApplication.run(MsTemplateRedisApplication.class, args);
	}

}
