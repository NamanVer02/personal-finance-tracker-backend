package com.example.personal_finance_tracker.app.routes;

import com.example.personal_finance_tracker.app.models.Category;
import com.example.personal_finance_tracker.app.services.CategoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
@Slf4j
public class CategoryController {

    private final CategoryService categoryService;

    @GetMapping
    public ResponseEntity<List<Category>> getAllCategories() {
        log.info("Entering getAllCategories method");
        List<Category> categories = categoryService.findAll();
        log.info("Exiting getAllCategories method with {} categories", categories.size());
        return ResponseEntity.ok(categories);
    }

    @GetMapping("/expense")
    public ResponseEntity<List<Category>> getExpenseCategories() {
        log.info("Entering getExpenseCategories method");
        List<Category> expenseCategories = categoryService.findAll().stream()
                .filter(category -> "Expense".equals(category.getType()))
                .collect(Collectors.toList());
        log.info("Exiting getExpenseCategories method with {} categories", expenseCategories.size());
        return ResponseEntity.ok(expenseCategories);
    }

    @GetMapping("/income")
    public ResponseEntity<List<Category>> getIncomeCategories() {
        log.info("Entering getIncomeCategories method");
        List<Category> incomeCategories = categoryService.findAll().stream()
                .filter(category -> "Income".equals(category.getType()))
                .collect(Collectors.toList());
        log.info("Exiting getIncomeCategories method with {} categories", incomeCategories.size());
        return ResponseEntity.ok(incomeCategories);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Category> getCategoryById(@PathVariable Long id) {
        log.info("Entering getCategoryById method for ID: {}", id);
        Optional<Category> category = categoryService.findById(id);
        if (category.isPresent()) {
            log.info("Exiting getCategoryById method with found category ID: {}", id);
            return ResponseEntity.ok(category.get());
        }
        log.warn("Exiting getCategoryById method - category ID {} not found", id);
        return ResponseEntity.notFound().build();
    }

    @PostMapping
    public ResponseEntity<Category> createCategory(@RequestBody Category category) {
        log.info("Entering createCategory method with category: {}", category);
        if (categoryService.existsByName(category.getName())) {
            log.warn("Category name conflict detected: {}", category.getName());
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
        Category savedCategory = categoryService.create(category);
        log.info("Exiting createCategory method with created category ID: {}", savedCategory.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(savedCategory);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Category> updateCategory(@PathVariable Long id, @RequestBody Category category) {
        log.info("Entering updateCategory method for ID: {}", id);
        try {
            Category updatedCategory = categoryService.update(id, category);
            log.info("Exiting updateCategory method with updated category ID: {}", id);
            return ResponseEntity.ok(updatedCategory);
        } catch (Exception e) {
            log.warn("Failed to update category ID {}: {}", id, e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCategory(@PathVariable Long id) {
        log.info("Entering deleteCategory method for ID: {}", id);
        categoryService.deleteById(id);
        log.info("Exiting deleteCategory method - category ID {} deleted", id);
        return ResponseEntity.noContent().build();
    }
}
