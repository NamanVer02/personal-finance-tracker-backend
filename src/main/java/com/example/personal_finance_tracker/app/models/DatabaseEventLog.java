package com.example.personal_finance_tracker.app.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "db_event_logs")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DatabaseEventLog extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDate date;
    private LocalTime time;
    private String username;
    private String method; // CREATE, UPDATE, DELETE
    private String entityName;
    private String entityId;
    private String description;

    @Version
    private Long version;
}
