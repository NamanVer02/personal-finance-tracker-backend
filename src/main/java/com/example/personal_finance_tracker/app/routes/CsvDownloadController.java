package com.example.personal_finance_tracker.app.routes;

import com.example.personal_finance_tracker.app.models.FinanceEntry;
import com.example.personal_finance_tracker.app.services.FinanceEntryService;
import com.example.personal_finance_tracker.app.services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.List;

@RestController
@RequestMapping("/api")
public class CsvDownloadController {

    @Autowired
    private FinanceEntryService financeEntryService;

    @Autowired
    private UserService userService;

    @GetMapping("/download/{userId}")
    public ResponseEntity<byte[]> downloadCsv(@PathVariable Long userId) {
        List<FinanceEntry> transactions = financeEntryService.findByUserId(userId);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PrintWriter writer = getPrintWriter(outputStream, transactions);
        writer.close();

        byte[] csvBytes = outputStream.toByteArray();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentDispositionFormData("attachment", "Transactions.csv");

        return ResponseEntity.ok()
                .headers(headers)
                .body(csvBytes);
    }

    @GetMapping("/download/admin")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<byte[]> adminDownloadCsv() {
        List<FinanceEntry> transactions = financeEntryService.findAll();

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PrintWriter writer = getPrintWriter(outputStream, transactions);
        writer.close();

        byte[] csvBytes = outputStream.toByteArray();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentDispositionFormData("attachment", "Transactions.csv");

        return ResponseEntity.ok()
                .headers(headers)
                .body(csvBytes);
    }

    private PrintWriter getPrintWriter(ByteArrayOutputStream outputStream, List<FinanceEntry> transactions) {
        PrintWriter writer = new PrintWriter(outputStream);
        SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd");

        // Write CSV header, adding a column for the username
        writer.println("ID,Label,Amount,Type,Category,Date,Username");

        // Write CSV data, including the username
        for (FinanceEntry transaction : transactions) {
            String username = userService.getUsernameByUserId(transaction.getUserId());
            writer.println(
                    transaction.getId() + "," +
                            transaction.getLabel() + "," +
                            transaction.getAmount() + "," +
                            transaction.getType() + "," +
                            transaction.getCategory() + "," +
                            dateFormatter.format(transaction.getDate()) + "," +
                            username
            );
        }

        writer.flush();
        return writer;
    }
}
