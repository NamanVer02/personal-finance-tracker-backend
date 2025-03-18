package com.example.personal_finance_tracker.app.routes;

import com.example.personal_finance_tracker.app.models.FinanceEntry;
import com.example.personal_finance_tracker.app.security.UserDetailsImpl;
import com.example.personal_finance_tracker.app.services.CsvImportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.Objects;

@RestController
@RequestMapping("/api")
public class CsvImportController {

    @Autowired
    private CsvImportService csvImportService;

    @PostMapping("/import-csv")
    public ResponseEntity<?> importCsv(@RequestParam("file") MultipartFile file, Authentication authentication) {
        try {
            // Get the current user
            UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();

            if (file.isEmpty() || !Objects.requireNonNull(file.getOriginalFilename()).endsWith(".csv")) {
                return ResponseEntity.badRequest().body(Map.of("error", "Please upload a valid CSV file"));
            }

            // Process the CSV file
            List<FinanceEntry> importedEntries = csvImportService.importCsvEntries(file, userDetails.getId());

            return ResponseEntity.ok(Map.of(
                    "message", "Transactions imported successfully",
                    "count", importedEntries.size()
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to import CSV: " + e.getMessage()));
        }
    }
}