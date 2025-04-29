package com.example.personal_finance_tracker.app.services;

import com.example.personal_finance_tracker.app.interfaces.FinanceEntryRepoInterface;
import com.example.personal_finance_tracker.app.models.FinanceEntry;
import com.example.personal_finance_tracker.app.models.User;
import com.example.personal_finance_tracker.app.repository.UserRepo;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
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

@Slf4j
@Service
public class CsvImportService {

    @Autowired
    private FinanceEntryRepoInterface financeEntryRepository;

    @Autowired
    private UserRepo userRepo;

    private static final int BATCH_SIZE = 1000;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final String[] REQUIRED_HEADERS = {
            "ID", "Label", "Amount", "Type", "Category", "Date", "Username"
    };

    @Transactional
    public List<FinanceEntry> importCsvEntries(MultipartFile file, Long userId) throws IOException {
        log.info("Starting CSV import for user ID: {}", userId);
        validateFile(file);

        User user = userRepo.findById(userId)
                .orElseThrow(() -> {
                    log.error("User not found: {}", userId);
                    return new IllegalArgumentException("User not found");
                });

        List<FinanceEntry> batch = new ArrayList<>(BATCH_SIZE);
        List<FinanceEntry> result = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {

            CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT
                    .withFirstRecordAsHeader()
                    .withIgnoreHeaderCase()
                    .withTrim());

            validateCsvHeaders(csvParser);

            Iterable<CSVRecord> records = csvParser;
            for (CSVRecord record : records) {
                try {
                    FinanceEntry entry = parseRecord(record, user);
                    batch.add(entry);

                    if (batch.size() % BATCH_SIZE == 0) {
                        log.info("Processing batch of {} entries", BATCH_SIZE);
                        result.addAll(financeEntryRepository.saveAll(batch));
                        batch.clear();
                    }
                } catch (Exception e) {
                    log.warn("Error processing record {}: {}", record.getRecordNumber(), e.getMessage());
                }
            }

            if (!batch.isEmpty()) {
                log.info("Processing final batch of {} entries", batch.size());
                result.addAll(financeEntryRepository.saveAll(batch));
            }
        }
        log.info("Successfully imported {} entries for user ID: {}", result.size(), userId);
        return result;
    }

    private FinanceEntry parseRecord(CSVRecord record, User user) {
        log.debug("Parsing record {}", record.getRecordNumber());
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
            log.warn("Invalid number format: {}", value);
            return 0.0;
        }
    }

    private LocalDate parseDateSafe(String dateString) {
        try {
            return LocalDate.parse(dateString, DATE_FORMATTER);
        } catch (DateTimeParseException e) {
            log.warn("Invalid date format: {}, using current date", dateString);
            return LocalDate.now();
        }
    }

    private void validateFile(MultipartFile file) {
        log.info("Validating uploaded file");
        if (!"text/csv".equals(file.getContentType())) {
            log.error("Invalid file type: {}", file.getContentType());
            throw new IllegalArgumentException("Only CSV files are allowed");
        }

        if (file.getSize() > 100 * 1024 * 1024) {
            log.error("File size exceeded: {} bytes", file.getSize());
            throw new IllegalArgumentException("File size exceeds 100MB limit");
        }
    }

    private void validateCsvHeaders(CSVParser csvParser) {
        List<String> actualHeaders = csvParser.getHeaderNames();
        if (actualHeaders.size() != REQUIRED_HEADERS.length) {
            throw new IllegalArgumentException("CSV header does not match the required format. " +
                    "Expected headers: " + String.join(",", REQUIRED_HEADERS));
        }
        for (int i = 0; i < REQUIRED_HEADERS.length; i++) {
            if (!REQUIRED_HEADERS[i].equalsIgnoreCase(actualHeaders.get(i))) {
                throw new IllegalArgumentException("CSV header does not match the required format. " +
                        "Expected headers: " + String.join(",", REQUIRED_HEADERS));
            }
        }
    }
}
