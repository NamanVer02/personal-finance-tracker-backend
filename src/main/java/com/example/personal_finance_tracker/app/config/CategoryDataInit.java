package com.example.personal_finance_tracker.app.config;

import com.example.personal_finance_tracker.app.models.Category;
import com.example.personal_finance_tracker.app.repository.CategoryRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;

@Component
@Order(2)
@Slf4j
public class CategoryDataInit implements CommandLineRunner {

    @Autowired
    private CategoryRepository categoryRepository;

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        log.info("Starting category data initialization");

        try {
            initializeCategories();
            log.info("Category initialization completed successfully");
        } catch (Exception e) {
            log.error("Category initialization failed: {}", e.getMessage());
            throw e;
        }
    }

    private void initializeCategories() {
        List<String> expenseCategories = Arrays.asList(
                "Housing",
                "Utilities",
                "Groceries",
                "Transportation",
                "Insurance",
                "Healthcare",
                "Debt Repayment",
                "Entertainment & Leisure",
                "Savings & Emergency Fund",
                "Investments",
                "Miscellaneous"
        );

        List<String> incomeCategories = Arrays.asList(
                "Salary",
                "Business",
                "Investments",
                "Gifts",
                "Miscellaneous"
        );

        log.debug("Initializing {} expense categories", expenseCategories.size());
        initializeCategoryType(expenseCategories, "Expense");

        log.debug("Initializing {} income categories", incomeCategories.size());
        initializeCategoryType(incomeCategories, "Income");
    }

    private void initializeCategoryType(List<String> categories, String type) {
        for (String categoryName : categories) {
            try {
                handleCategoryCreation(categoryName, type);
            } catch (Exception e) {
                log.error("Error processing category '{}': {}", categoryName, e.getMessage());
            }
        }
    }

    private void handleCategoryCreation(String name, String type) {
        categoryRepository.findByName(name).ifPresentOrElse(
                existingCategory -> updateCategoryType(existingCategory, type),
                () -> createNewCategory(name, type)
        );
    }

    private void createNewCategory(String name, String type) {
        Category category = new Category();
        category.setName(name);
        category.setType(type);
        categoryRepository.save(category);
        log.info("Created new {} category: {}", type, name);
    }

    private void updateCategoryType(Category existingCategory, String newType) {
        if (!existingCategory.getType().equals(newType)) {
            String oldType = existingCategory.getType();
            existingCategory.setType(newType);
            categoryRepository.save(existingCategory);
            log.warn("Updated category '{}' type from {} to {}",
                    existingCategory.getName(), oldType, newType);
        } else {
            log.debug("Category '{}' already has type {}", existingCategory.getName(), newType);
        }
    }
}
