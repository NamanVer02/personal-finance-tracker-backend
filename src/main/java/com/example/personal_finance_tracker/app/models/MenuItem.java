package com.example.personal_finance_tracker.app.models;

import com.example.personal_finance_tracker.app.annotations.Loggable;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "menu_items")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Loggable
public class MenuItem extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String path;

    @Column(nullable = false)
    private String icon;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "menu_item_roles", joinColumns = @JoinColumn(name = "menu_item_id"))
    @Column(name = "role")
    private List<String> allowedRoles = new ArrayList<>();

    private boolean isActive = true;
    private int displayOrder;

    // Constructor with essential fields
    public MenuItem(String name, String path, String icon, List<String> allowedRoles, int displayOrder) {
        this.name = name;
        this.path = path;
        this.icon = icon;
        this.allowedRoles = allowedRoles;
        this.displayOrder = displayOrder;
    }
}