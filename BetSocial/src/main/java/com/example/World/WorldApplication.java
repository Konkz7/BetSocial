package com.example.World;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



@SpringBootApplication
@EnableScheduling
@EnableTransactionManagement
public class WorldApplication {

	// SLF4J, like everything else. java.util.logging went through a bridge and
	// ignored the Logback configuration this application actually uses, so this
	// one line was formatted differently from every other line in the log.
	private static final Logger logger = LoggerFactory.getLogger(WorldApplication.class);

	public static void main(String[] args) {
		SpringApplication.run(WorldApplication.class, args);
		logger.info("WorldApplication started successfully!");
	}

	// Two commented-out blocks stood here, printing a generated secret and
	// CIRCLE_SECRET to standard out. Gone with the Circle integration they
	// belonged to - and printing a secret is not something to leave lying around
	// as a comment somebody might uncomment.

}
