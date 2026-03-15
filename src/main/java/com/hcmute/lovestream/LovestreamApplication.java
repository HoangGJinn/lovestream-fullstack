package com.hcmute.lovestream;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class LovestreamApplication {

	public static void main(String[] args) {
		SpringApplication.run(LovestreamApplication.class, args);
	}

}
