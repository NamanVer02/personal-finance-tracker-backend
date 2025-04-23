package com.example.personal_finance_tracker.app.routes;

import com.example.personal_finance_tracker.app.models.MenuItem;
import com.example.personal_finance_tracker.app.models.User;
import com.example.personal_finance_tracker.app.services.MenuItemService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
public class MenuItemController {

    @Autowired
    private MenuItemService menuItemService;

    @GetMapping("/menu-items")
    public ResponseEntity<List<MenuItem>> getUserMenuItems(Authentication authentication) {
        // Extract roles from the authenticated user
        List<String> roles = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());
        
        // Get menu items filtered by user roles
        List<MenuItem> menuItems = menuItemService.getMenuItemsForRoles(roles);
        return ResponseEntity.ok(menuItems);
    }

    @GetMapping("/admin/menu-items")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<List<MenuItem>> getAllMenuItems() {
        return ResponseEntity.ok(menuItemService.getAllMenuItems());
    }

    @PutMapping("/admin/menu-items")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<List<MenuItem>> updateMenuItems(@RequestBody List<MenuItem> menuItems) {
        List<MenuItem> updatedMenuItems = menuItemService.saveAllMenuItems(menuItems);
        return ResponseEntity.ok(updatedMenuItems);
    }

    @PostMapping("/admin/menu-items")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<MenuItem> createMenuItem(@RequestBody MenuItem menuItem) {
        MenuItem savedMenuItem = menuItemService.saveMenuItem(menuItem);
        return ResponseEntity.ok(savedMenuItem);
    }

    @DeleteMapping("/admin/menu-items/{id}")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<Void> deleteMenuItem(@PathVariable Long id) {
        menuItemService.deleteMenuItem(id);
        return ResponseEntity.ok().build();
    }
}