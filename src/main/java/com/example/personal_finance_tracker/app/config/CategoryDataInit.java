package com.example.personal_finance_tracker.app.config;

import com.example.personal_finance_tracker.app.models.Category;
import com.example.personal_finance_tracker.app.repository.CategoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;

@Component
@Order(2) // Run after roles are initialized
public class CategoryDataInit implements CommandLineRunner {

    @Autowired
    private CategoryRepository categoryRepository;

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        System.out.println("Initializing default categories...");

        // Define expense categories
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

        // Define income categories
        List<String> incomeCategories = Arrays.asList(
                "Salary",
                "Business",
                "Investments",
                "Gifts",
                "Miscellaneous"
        );

        // Initialize expense categories
        for (String categoryName : expenseCategories) {
            createCategoryIfNotExists(categoryName, "Expense");
        }

        // Initialize income categories
        for (String categoryName : incomeCategories) {
            createCategoryIfNotExists(categoryName, "Income");
        }

        System.out.println("Default categories initialized successfully");
    }

    private void createCategoryIfNotExists(String name, String type) {
        if (!categoryRepository.existsByName(name)) {
            Category category = new Category();
            category.setName(name);
            category.setType(type);
            categoryRepository.save(category);
            System.out.println("Created " + type + " category: " + name);
        } else {
            // Update type if it's different
            Category existingCategory = categoryRepository.findByName(name).orElse(null);
            if (existingCategory != null && !existingCategory.getType().equals(type)) {
                existingCategory.setType(type);
                categoryRepository.save(existingCategory);
                System.out.println("Updated type for category: " + name + " to " + type);
            }
        }
    }
}