package com.example.World;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import java.util.logging.Logger;

@SpringBootApplication
@EnableScheduling
@EnableTransactionManagement
public class WorldApplication {

	private static final Logger logger = Logger.getLogger(WorldApplication.class.getName());
	public static void main(String[] args) {
		SpringApplication.run(WorldApplication.class, args);
		logger.info("WorldApplication started successfully!");
	}

}
