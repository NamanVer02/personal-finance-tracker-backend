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
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@Service
public class CsvImportService {

    @Autowired
    private FinanceEntryRepoInterface financeEntryRepository;

    @Autowired
    private UserRepo userRepo;

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");

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

                // Parse the date
                try {
                    Date date = DATE_FORMAT.parse(record.get("Date"));
                    entry.setDate(date);
                } catch (ParseException e) {
                    // Default to current date if parsing fails
                    entry.setDate(new Date());
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