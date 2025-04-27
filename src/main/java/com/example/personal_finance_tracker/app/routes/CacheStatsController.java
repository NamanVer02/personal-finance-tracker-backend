package com.example.personal_finance_tracker.app.routes;

import com.example.personal_finance_tracker.app.models.User;
import com.example.personal_finance_tracker.app.services.UserService;
import com.example.personal_finance_tracker.app.utils.LogCollector;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/cache")
public class CacheStatsController {

    @Autowired
    private UserService userService;

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getCacheStats() {
        return ResponseEntity.ok(LogCollector.getCacheStats());
    }

    @GetMapping("/logs")
    public ResponseEntity<List<String>> getCacheLogs(@RequestParam(defaultValue = "100") int count) {
        return ResponseEntity.ok(LogCollector.getCacheLogs(count));
    }

    @GetMapping("/reset")
    public ResponseEntity<Void> resetCacheStats() {
        LogCollector.clearCacheLogs();
        return ResponseEntity.ok().build();
    }

    @PostMapping("/demo")
    public ResponseEntity<Map<String, String>> runCacheDemo() {
        // This method will execute various cache operations to demonstrate caching
        try {
            // Get example user (cache miss)
            userService.getUserById(1L);

            // Get the same user again (cache hit)
            userService.getUserById(1L);

            // Get another user (cache miss)
            userService.getUserById(2L);

            // Update a user (cache eviction)
            User user = userService.getUserById(1L);
            if (user != null) {
                userService.save(user);
            }

            // Get updated user (cache miss)
            userService.getUserById(1L);

            return ResponseEntity.ok(Collections.singletonMap("message", "Cache demo executed successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Collections.singletonMap("error", "Failed to execute cache demo: " + e.getMessage()));
        }
    }
}
