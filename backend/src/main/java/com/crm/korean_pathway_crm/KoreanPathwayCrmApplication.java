package com.crm.korean_pathway_crm;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class KoreanPathwayCrmApplication {

	public static void main(String[] args) {
		SpringApplication.run(KoreanPathwayCrmApplication.class, args);
	}

}
