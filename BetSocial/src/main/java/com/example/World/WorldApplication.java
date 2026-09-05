package com.example.World;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import java.util.logging.Logger;

import java.security.SecureRandom;
import java.util.HexFormat;


@SpringBootApplication
@EnableScheduling
@EnableTransactionManagement
public class WorldApplication {

	private static final Logger logger = Logger.getLogger(WorldApplication.class.getName());
	public static void main(String[] args) {
		SpringApplication.run(WorldApplication.class, args);
		logger.info("WorldApplication started successfully!");

/*
		SecureRandom random = new SecureRandom();
		byte[] secret = new byte[32];
		random.nextBytes(secret);

		String hexSecret = HexFormat.of().formatHex(secret);
		System.out.println("Generated Secret: " + hexSecret);

 */
/*
		String circleEntitySecret = System.getenv("CIRCLE_SECRET");
		System.out.println("Secret: " + circleEntitySecret);

 */




	}

}
