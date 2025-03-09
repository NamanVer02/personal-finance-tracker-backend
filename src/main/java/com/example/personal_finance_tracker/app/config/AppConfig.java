//package com.example.personal_finance_tracker.app.config;
//
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import com.example.personal_finance_tracker.app.services.FinanceEntryService;
//import com.example.personal_finance_tracker.app.repository.JpaFinanceEntryRepo;
//import com.example.personal_finance_tracker.app.interfaces.FinanceEntryRepoInterface;
//
//@Configuration
//public class AppConfig {
//    @Bean
//    public FinanceEntryService financeEntryService (FinanceEntryRepoInterface financeEntryRepoInterface) {
//        return new FinanceEntryService(financeEntryRepoInterface);
//    }
//
//    @Bean
//    public FinanceEntryRepoInterface financeEntryRepoInterface (JpaFinanceEntryRepo jpaFinanceEntryRepo) {
//        return jpaFinanceEntryRepo;
//    }
//}
