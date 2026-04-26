package com.banco.workflow;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class WorkflowEngineApplication {

	public static void main(String[] args) {
		System.setProperty("https.protocols", "TLSv1.2");
		System.setProperty("jdk.tls.client.protocols", "TLSv1.2");
		SpringApplication.run(WorkflowEngineApplication.class, args);
	}

}
