package com.example.personal_finance_tracker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@EntityScan("com.example.personal_finance_tracker.app.models")
@EnableJpaRepositories("com.example.personal_finance_tracker.app")
@SpringBootApplication(scanBasePackages = {"com.example.personal_finance_tracker.app"})
@EnableTransactionManagement
public class PersonalFinanceTrackerApplication {

	public static void main(String[] args) {
		SpringApplication.run(PersonalFinanceTrackerApplication.class, args);
	}

}
