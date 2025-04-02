package com.example.personal_finance_tracker.app.routes;

import com.example.personal_finance_tracker.app.services.DemoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.stream.IntStream;

@RestController
@RequestMapping("/api/test")
public class TestController {

    @Autowired
    private DemoService demoService;

    @GetMapping("/all")
    public String allAccess() {
        return "Public Content.";
    }

    @GetMapping("/user")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public String userAccess() {
        return "User Content.";
    }

    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public String adminAccess() {
        return "Admin Board.";
    }

    @GetMapping("/accountant")
    @PreAuthorize("hasRole('ACCOUNTANT')")
    public String accountantAccess() {
        return "Accountant Board.";
    }

    @GetMapping("/pooling")
    public ResponseEntity<String> testConnectionPooling() {
        IntStream.range(0, 20).parallel().forEach(i -> demoService.performDatabaseOperation(i));
        return ResponseEntity.ok("Connection pool test completed!");
    }
}
