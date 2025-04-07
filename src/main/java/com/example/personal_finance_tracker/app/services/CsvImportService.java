package com.example.personal_finance_tracker.app.services;

import com.example.personal_finance_tracker.app.interfaces.FinanceEntryRepoInterface;
import com.example.personal_finance_tracker.app.models.FinanceEntry;
import com.example.personal_finance_tracker.app.models.User;
import com.example.personal_finance_tracker.app.repository.UserRepo;
import jakarta.transaction.Transactional;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

@Service
public class CsvImportService {

    @Autowired
    private FinanceEntryRepoInterface financeEntryRepository;

    @Autowired
    private UserRepo userRepo;

    private static final int BATCH_SIZE = 1000; // Process in chunks
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");


    @Transactional
    public List<FinanceEntry> importCsvEntries(MultipartFile file, Long userId) throws IOException {
        // Validate file first
        validateFile(file);

        User user = userRepo.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        List<FinanceEntry> batch = new ArrayList<>(BATCH_SIZE);
        List<FinanceEntry> result = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {

            CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT
                    .withFirstRecordAsHeader()
                    .withIgnoreHeaderCase()
                    .withTrim());

            Iterable<CSVRecord> records = csvParser;
            for (CSVRecord record : records) {
                try {
                    FinanceEntry entry = parseRecord(record, user);
                    batch.add(entry);

                    if (batch.size() % BATCH_SIZE == 0) {
                        result.addAll(financeEntryRepository.saveAll(batch));
                        batch.clear();
                    }
                } catch (Exception e) {
                    // Log and skip bad records
                    System.out.println("Error processing record :" + record.getRecordNumber() + e.getMessage());
                }
            }

            // Save remaining entries
            if (!batch.isEmpty()) {
                result.addAll(financeEntryRepository.saveAll(batch));
            }
        }

        return result;
    }

    private FinanceEntry parseRecord(CSVRecord record, User user) {
        FinanceEntry entry = new FinanceEntry();
        entry.setLabel(record.get("Label"));
        entry.setType(record.get("Type"));
        entry.setAmount(parseDoubleSafe(record.get("Amount")));
        entry.setCategory(record.get("Category"));
        entry.setDate(parseDateSafe(record.get("Date")));
        entry.setUser(user);
        return entry;
    }

    private double parseDoubleSafe(String value) {
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            return 0.0; // or throw custom exception
        }
    }

    private LocalDate parseDateSafe(String dateString) {
        try {
            return LocalDate.parse(dateString, DATE_FORMATTER);
        } catch (DateTimeParseException e) {
            return LocalDate.now();
        }
    }

    private void validateFile(MultipartFile file) {
        if (!"text/csv".equals(file.getContentType())) {
            throw new IllegalArgumentException("Only CSV files are allowed");
        }

        if (file.getSize() > 100 * 1024 * 1024) { // 100MB
            throw new IllegalArgumentException("File size exceeds 100MB limit");
        }
    }
}