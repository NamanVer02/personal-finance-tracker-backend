package com.example.personal_finance_tracker.app.models;

import com.example.personal_finance_tracker.app.annotations.Encode;
import com.example.personal_finance_tracker.app.annotations.Loggable;
import com.example.personal_finance_tracker.app.config.StringEncodeConverter;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "categories")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Loggable
public class Category {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Encode
    @Convert(converter = StringEncodeConverter.class)
    @Column(name = "name", nullable = false)
    private String name;

    @Column(nullable = false)
    private String type;

    @Version
    private Long version;
}