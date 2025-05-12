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
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
public class CsvImportService {

    private final FinanceEntryRepoInterface financeEntryRepository;
    private final UserRepo userRepo;

    public CsvImportService (FinanceEntryRepoInterface financeEntryRepository, UserRepo userRepo) {
        this.financeEntryRepository = financeEntryRepository;
        this.userRepo = userRepo;
    }

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
                    .withIgnoreHeaderCase(true)
                    .withTrim());

            validateCsvHeaders(csvParser);

            for (CSVRecord csvRecord : csvParser) {
                try {
                    FinanceEntry entry = parseRecord(csvRecord, user);
                    batch.add(entry);

                    if (batch.size() % BATCH_SIZE == 0) {
                        log.info("Processing batch of {} entries", BATCH_SIZE);
                        result.addAll(financeEntryRepository.saveAll(batch));
                        batch.clear();
                    }
                } catch (Exception e) {
                    log.warn("Error processing record {}: {}", csvRecord.getRecordNumber(), e.getMessage());
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

    private FinanceEntry parseRecord(CSVRecord csvRecord, User user) {
        log.debug("Parsing record {}", csvRecord.getRecordNumber());
        FinanceEntry entry = new FinanceEntry();
        
        // Use case insensitive lookup
        try {
            // We won't use the ID from the CSV as that's for display only
            entry.setLabel(getValueCaseInsensitive(csvRecord, "Label"));
            entry.setType(getValueCaseInsensitive(csvRecord, "Type"));
            entry.setAmount(parseDoubleSafe(getValueCaseInsensitive(csvRecord, "Amount")));
            entry.setCategory(getValueCaseInsensitive(csvRecord, "Category"));
            entry.setDate(parseDateSafe(getValueCaseInsensitive(csvRecord, "Date")));
            entry.setUser(user);
            
            log.info("Successfully parsed record: label={}, type={}, amount={}, category={}, date={}",
                     entry.getLabel(), entry.getType(), entry.getAmount(), entry.getCategory(), entry.getDate());
                     
            return entry;
        } catch (Exception e) {
            log.error("Error parsing CSV record: {}", e.getMessage());
            throw new IllegalArgumentException("Error parsing CSV record: " + e.getMessage());
        }
    }
    
    private String getValueCaseInsensitive(CSVRecord record, String header) {
        String value = null;
        // Try exact match first
        if (record.isMapped(header)) {
            value = record.get(header);
        } else {
            // Try case-insensitive match
            for (String recordHeader : record.getParser().getHeaderMap().keySet()) {
                if (recordHeader.equalsIgnoreCase(header)) {
                    value = record.get(recordHeader);
                    break;
                }
            }
        }
        
        if (value == null) {
            throw new IllegalArgumentException("Could not find column: " + header);
        }
        
        return value;
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
        log.info("Validating uploaded file with content type: {}", file.getContentType());
        
        // More lenient content type checking
        String contentType = file.getContentType();
        if (contentType != null && !contentType.equals("text/csv") && 
            !contentType.equals("application/csv") && 
            !contentType.equals("application/vnd.ms-excel") &&
            !contentType.contains("csv") &&
            !contentType.equals("application/octet-stream")) {
            
            log.error("Invalid file type: {}", contentType);
            throw new IllegalArgumentException("Only CSV files are allowed. Current content type: " + contentType);
        }

        // Check file extension as a backup validation
        String fileName = file.getOriginalFilename();
        if (fileName != null && !fileName.toLowerCase().endsWith(".csv")) {
            log.error("Invalid file extension: {}", fileName);
            throw new IllegalArgumentException("File must have .csv extension");
        }

        if (file.getSize() > 100 * 1024 * 1024) {
            log.error("File size exceeded: {} bytes", file.getSize());
            throw new IllegalArgumentException("File size exceeds 100MB limit");
        }
        
        log.info("File validation passed for {}", file.getOriginalFilename());
    }

    private void validateCsvHeaders(CSVParser csvParser) {
        log.info("Validating CSV headers: {}", csvParser.getHeaderMap().keySet());
        
        Set<String> actualHeadersLowerCase = csvParser.getHeaderMap().keySet().stream()
                .map(String::toLowerCase)
                .collect(Collectors.toSet());
                
        List<String> missingHeaders = Arrays.stream(REQUIRED_HEADERS)
                .map(String::toLowerCase)
                .filter(header -> !actualHeadersLowerCase.contains(header))
                .collect(Collectors.toList());
                
        if (!missingHeaders.isEmpty()) {
            log.error("Missing required headers: {}", missingHeaders);
            throw new IllegalArgumentException("CSV header does not match the required format. " +
                    "Expected headers: " + String.join(",", REQUIRED_HEADERS) + 
                    ". Missing: " + String.join(",", missingHeaders));
        }
    }
}
