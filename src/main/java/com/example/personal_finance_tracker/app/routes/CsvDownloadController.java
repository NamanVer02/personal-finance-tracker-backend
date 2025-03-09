package com.example.personal_finance_tracker.app.routes;

import com.example.personal_finance_tracker.app.models.FinanceEntry;
import com.example.personal_finance_tracker.app.services.FinanceEntryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.List;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api")
public class CsvDownloadController {

    @Autowired
    private FinanceEntryService financeEntryService;


    @GetMapping("/download")
    public ResponseEntity<byte[]> downloadCsv() {
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

    private static PrintWriter getPrintWriter(ByteArrayOutputStream outputStream, List<FinanceEntry> transactions) {
        PrintWriter writer = new PrintWriter(outputStream);
        SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd");

        // Write CSV header
        writer.println("ID,Label,Amount,Type,Category,Date");

        // Write CSV data
        for (FinanceEntry transaction : transactions) {
            writer.println(
                    transaction.getId() + "," +
                            transaction.getLabel() + "," +
                            transaction.getAmount() + "," +
                            transaction.getType() + "," +
                            transaction.getCategory() + "," +
                            dateFormatter.format(transaction.getDate())
            );
        }

        writer.flush();
        return writer;
    }

    // send this to services print writer
}
