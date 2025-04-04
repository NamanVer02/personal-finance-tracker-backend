package com.example.personal_finance_tracker.app.models;

import com.example.personal_finance_tracker.app.annotations.Encode;
import com.example.personal_finance_tracker.app.annotations.Loggable;
import com.example.personal_finance_tracker.app.config.StringEncodeConverter;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Entity
@Table(name = "blacklisted_tokens")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Loggable
public class BlacklistedToken {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Encode
    @Convert(converter = StringEncodeConverter.class)
    @Column(unique = true, nullable = false, length = 500)
    private String token;


    @Column(nullable = false)
    private Date expiryDate;

    @Version
    private Long version;
}
