package com.project.conveyor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;


@SpringBootApplication
public class ConveyorApplication {

	static final Logger logger = LoggerFactory.getLogger(ConveyorApplication.class);

	public static void main(String[] args) {
		SpringApplication.run(ConveyorApplication.class, args);
	}

}
