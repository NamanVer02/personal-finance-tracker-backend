package com.example.personal_finance_tracker.app.services;

import com.example.personal_finance_tracker.app.models.Category;
import com.example.personal_finance_tracker.app.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.EmptyResultDataAccessException;
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
        try {
            return categoryRepository.findAll();
        } catch (DataAccessException e) {
            log.error("Error retrieving all categories", e);
            throw new RuntimeException("Failed to retrieve categories", e);
        }
    }

    public Optional<Category> findById(Long id) {
        log.info("Looking up category by ID: {}", id);
        try {
            return categoryRepository.findById(id);
        } catch (DataAccessException e) {
            log.error("Error finding category by ID: {}", id, e);
            throw new RuntimeException("Failed to find category by ID", e);
        }
    }

    public Optional<Category> findByName(String name) {
        log.info("Searching for category by name: {}", name);
        try {
            return categoryRepository.findByName(name);
        } catch (DataAccessException e) {
            log.error("Error finding category by name: {}", name, e);
            throw new RuntimeException("Failed to find category by name", e);
        }
    }

    public Category create(Category category) {
        log.info("Creating new category: {}", category.getName());
        try {
            return categoryRepository.save(category);
        } catch (DataAccessException e) {
            log.error("Error creating category: {}", category.getName(), e);
            throw new RuntimeException("Failed to create category", e);
        }
    }

    public Category update(Long id, Category category) throws Exception {
        log.info("Attempting to update category ID: {}", id);
        try {
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
        } catch (DataAccessException e) {
            log.error("Error updating category ID: {}", id, e);
            throw new RuntimeException("Failed to update category", e);
        }
    }

    public void deleteById(Long id) {
        log.info("Deleting category ID: {}", id);
        try {
            categoryRepository.deleteById(id);
        } catch (EmptyResultDataAccessException e) {
            log.error("Category not found for deletion: ID {}", id, e);
            throw new RuntimeException("Category not found with id: " + id, e);
        } catch (DataAccessException e) {
            log.error("Error deleting category ID: {}", id, e);
            throw new RuntimeException("Failed to delete category", e);
        }
    }

    public boolean existsByName(String name) {
        log.info("Checking existence of category name: {}", name);
        try {
            return categoryRepository.existsByName(name);
        } catch (DataAccessException e) {
            log.error("Error checking if category name exists: {}", name, e);
            throw new RuntimeException("Failed to check if category exists by name", e);
        }
    }
}
