package com.example.personal_finance_tracker.app.services;

import com.example.personal_finance_tracker.app.models.Category;
import com.example.personal_finance_tracker.app.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;

    public List<Category> findAll() {
        log.info("Retrieving all categories");
        return categoryRepository.findAll();
    }

    public Optional<Category> findById(Long id) {
        log.info("Looking up category by ID: {}", id);
        return categoryRepository.findById(id);
    }

    public Optional<Category> findByName(String name) {
        log.info("Searching for category by name: {}", name);
        return categoryRepository.findByName(name);
    }

    public Category create(Category category) {
        log.info("Creating new category: {}", category.getName());
        return categoryRepository.save(category);
    }

    public Category update(Long id, Category category) throws Exception {
        log.info("Attempting to update category ID: {}", id);
        return categoryRepository.findById(id)
                .map(existingCategory -> {
                    log.info("Updating category {}: new name - {}", id, category.getName());
                    existingCategory.setName(category.getName());
                    return categoryRepository.save(existingCategory);
                })
                .orElseThrow(() -> {
                    log.error("Category not found for update: ID {}", id);
                    return new Exception("Category not found with id: " + id);
                });
    }

    public void deleteById(Long id) {
        log.info("Deleting category ID: {}", id);
        categoryRepository.deleteById(id);
    }

    public boolean existsByName(String name) {
        log.info("Checking existence of category name: {}", name);
        return categoryRepository.existsByName(name);
    }
}
