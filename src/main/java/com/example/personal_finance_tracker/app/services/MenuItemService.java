package com.example.personal_finance_tracker.app.services;

import com.example.personal_finance_tracker.app.models.MenuItem;
import com.example.personal_finance_tracker.app.repository.MenuItemRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

@Service
public class MenuItemService {

    @Autowired
    private MenuItemRepository menuItemRepository;

    public List<MenuItem> getAllMenuItems() {
        return menuItemRepository.findAll();
    }

    public List<MenuItem> getActiveMenuItems() {
        return menuItemRepository.findAllByIsActiveTrueOrderByDisplayOrderAsc();
    }

    public List<MenuItem> getMenuItemsForRoles(List<String> roles) {
        List<MenuItem> allMenuItems = getActiveMenuItems();
        return allMenuItems.stream()
                .filter(menuItem -> menuItem.getAllowedRoles().stream()
                        .anyMatch(roles::contains))
                .toList();
    }

    public MenuItem saveMenuItem(MenuItem menuItem) {
        return menuItemRepository.save(menuItem);
    }

    public List<MenuItem> saveAllMenuItems(List<MenuItem> menuItems) {
        return menuItemRepository.saveAll(menuItems);
    }

    public void deleteMenuItem(Long id) {
        menuItemRepository.deleteById(id);
    }

    @PostConstruct
    public void initializeDefaultMenuItems() {
        // Only initialize if no menu items exist
        if (menuItemRepository.count() == 0) {
            List<MenuItem> defaultMenuItems = Arrays.asList(
                new MenuItem("Dashboard", "/dashboard", "BarChart3", 
                    Arrays.asList("ROLE_USER", "ROLE_ADMIN", "ROLE_ACCOUNTANT"), 1),
                new MenuItem("User Dashboard", "/user-dashboard", "User", 
                    Arrays.asList("ROLE_USER", "ROLE_ADMIN", "ROLE_ACCOUNTANT"), 2),
                new MenuItem("Role Management", "/user-role-management", "Users", 
                    Arrays.asList("ROLE_ADMIN"), 3),
                new MenuItem("User Transactions", "/user-transactions", "BadgeDollarSign", 
                    Arrays.asList("ROLE_ADMIN"), 4),
                new MenuItem("Categories", "/categories", "Tags", 
                    Arrays.asList("ROLE_ADMIN"), 5),
                new MenuItem("Accountant Dashboard", "/accountant-dashboard", "BarChart3", 
                    Arrays.asList("ROLE_ACCOUNTANT"), 6),
                new MenuItem("AI Assistant", "/ai-assistant", "MessageCircle", 
                    Arrays.asList("ROLE_USER", "ROLE_ADMIN", "ROLE_ACCOUNTANT"), 7),
                new MenuItem("Menu Management", "/menu-management", "Menu", 
                    Arrays.asList("ROLE_ADMIN"), 8)
            );
            menuItemRepository.saveAll(defaultMenuItems);
        }
    }
}