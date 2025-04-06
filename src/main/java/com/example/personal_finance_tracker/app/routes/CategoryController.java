package com.example.personal_finance_tracker.app.routes;

import com.example.personal_finance_tracker.app.models.Category;
import com.example.personal_finance_tracker.app.services.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @GetMapping
    public ResponseEntity<List<Category>> getAllCategories() {
        return ResponseEntity.ok(categoryService.findAll());
    }

    @GetMapping("/expense")
    public ResponseEntity<List<Category>> getExpenseCategories() {
        List<Category> expenseCategories = categoryService.findAll().stream()
                .filter(category -> "Expense".equals(category.getType()))
                .collect(Collectors.toList());
        return ResponseEntity.ok(expenseCategories);
    }

    @GetMapping("/income")
    public ResponseEntity<List<Category>> getIncomeCategories() {
        List<Category> incomeCategories = categoryService.findAll().stream()
                .filter(category -> "Income".equals(category.getType()))
                .collect(Collectors.toList());
        return ResponseEntity.ok(incomeCategories);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Category> getCategoryById(@PathVariable Long id) {
        Optional<Category> category = categoryService.findById(id);
        return category.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Category> createCategory(@RequestBody Category category) {
        if (categoryService.existsByName(category.getName())) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
        Category savedCategory = categoryService.create(category);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedCategory);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Category> updateCategory(@PathVariable Long id, @RequestBody Category category) {
        try {
            Category updatedCategory = categoryService.update(id, category);
            return ResponseEntity.ok(updatedCategory);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCategory(@PathVariable Long id) {
        categoryService.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}