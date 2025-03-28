package com.example.personal_finance_tracker.app.services;

import com.example.personal_finance_tracker.app.interfaces.FinanceEntryRepoInterface;
import com.example.personal_finance_tracker.app.models.FinanceEntry;
import com.example.personal_finance_tracker.app.models.User;
import com.example.personal_finance_tracker.app.repository.UserRepo;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class CsvImportService {

    @Autowired
    private FinanceEntryRepoInterface financeEntryRepository;

    @Autowired
    private UserRepo userRepo;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public List<FinanceEntry> importCsvEntries(MultipartFile file, Long userId) throws IOException {
        List<FinanceEntry> result = new ArrayList<>();

        // Get the user
        Optional<User> userOptional = userRepo.findById(userId);
        if (userOptional.isEmpty()) {
            throw new IllegalArgumentException("User not found");
        }
        User user = userOptional.get();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream()));
             CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT.withFirstRecordAsHeader())) {

            for (CSVRecord record : csvParser) {
                // Parse the CSV record
                FinanceEntry entry = new FinanceEntry();
                entry.setLabel(record.get("Label"));
                entry.setType(record.get("Type"));
                entry.setAmount(Double.parseDouble(record.get("Amount")));
                entry.setCategory(record.get("Category"));

                // Parse the date using LocalDate
                try {
                    LocalDate date = LocalDate.parse(record.get("Date"), DATE_FORMATTER);
                    entry.setDate(date);
                } catch (DateTimeParseException e) {
                    // Default to current date if parsing fails
                    entry.setDate(LocalDate.now());
                }

                // Set the user
                entry.setUser(user);

                // Save and add to result list
                result.add(financeEntryRepository.save(entry));
            }
        }

        return result;
    }
}