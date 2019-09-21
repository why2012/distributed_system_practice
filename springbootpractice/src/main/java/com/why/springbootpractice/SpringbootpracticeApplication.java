package com.why.springbootpractice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.context.annotation.ImportResource;

@SpringBootApplication
@EnableScheduling
@ComponentScan(basePackages = {"com.why"})
@ImportResource(locations = {"classpath:dubbo-provider.xml"})
public class SpringbootpracticeApplication {

	public static void main(String[] args) {
		SpringApplication.run(SpringbootpracticeApplication.class, args);
	}

}
