package com.example.personal_finance_tracker.app.routes;

import com.example.personal_finance_tracker.app.models.FinanceEntry;
import com.example.personal_finance_tracker.app.security.UserDetailsImpl;
import com.example.personal_finance_tracker.app.services.FinanceEntryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@CrossOrigin(origins = "http://localhost:3000")  // set global CORS policy
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class FinanceEntryController {
    private final FinanceEntryService financeEntryService;

    @GetMapping("/get")
    public List<FinanceEntry> getAll() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        
        // Check if user has ADMIN role
        boolean isAdmin = authentication.getAuthorities().contains(new SimpleGrantedAuthority("ADMIN"));
        
        if (isAdmin) {
            // Admin can see all entries
            return financeEntryService.findAll();
        } else {
            // Regular users can only see their own entries
            return financeEntryService.findByUserId(userDetails.getId());
        }
    }

    @GetMapping("/get/{type}")
    public List<FinanceEntry> getByType(@PathVariable String type) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        
        // Check if user has ADMIN role
        boolean isAdmin = authentication.getAuthorities().contains(new SimpleGrantedAuthority("ADMIN"));
        
        if (isAdmin) {
            // Admin can see all entries of a specific type
            return financeEntryService.findByType(type);
        } else {
            // Regular users can only see their own entries of a specific type
            return financeEntryService.findByTypeAndUserId(type, userDetails.getId());
        }
    }

    @PostMapping("/post")
    public FinanceEntry create(@RequestBody FinanceEntry financeEntry) {
        // Set the current user as the owner of the finance entry
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        financeEntry.setUserId(userDetails.getId());
        
        return financeEntryService.create(financeEntry);
    }

    @PutMapping("/put/{id}")
    public FinanceEntry update(@PathVariable Long id, @RequestBody FinanceEntry financeEntry) throws Exception {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        boolean isAdmin = authentication.getAuthorities().contains(new SimpleGrantedAuthority("ADMIN"));
        
        // Get the existing entry to check ownership
        List<FinanceEntry> userEntries = financeEntryService.findByUserId(userDetails.getId());
        boolean isOwner = userEntries.stream().anyMatch(entry -> entry.getId().equals(id));
        
        // Only allow update if user is admin or the owner of the entry
        if (isAdmin || isOwner) {
            // Preserve the original user ID - don't allow changing ownership
            financeEntry.setUserId(isAdmin && !isOwner ? 
                    financeEntryService.findAll().stream()
                            .filter(e -> e.getId().equals(id))
                            .findFirst()
                            .map(FinanceEntry::getUserId)
                            .orElse(null) : 
                    userDetails.getId());
            
            return financeEntryService.update(id, financeEntry);
        } else {
            throw new Exception("You don't have permission to update this entry");
        }
    }

    @DeleteMapping("delete/{id}")
    public void delete(@PathVariable Long id) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        boolean isAdmin = authentication.getAuthorities().contains(new SimpleGrantedAuthority("ADMIN"));

        List<FinanceEntry> userEntries = financeEntryService.findByUserId(userDetails.getId());
        boolean isOwner = userEntries.stream().anyMatch(entry -> entry.getId().equals(id));

        if(isAdmin || isOwner) { financeEntryService.deleteById(id); }
        financeEntryService.deleteById(id);
    }


    @GetMapping("/get/summary/expense/{userId}")
    public ResponseEntity<Map<String, Double>> getCategoryWiseSpendingForCurrentMonth(@PathVariable Long userId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();

        if (!userDetails.getId().equals(userId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        Map<String, Double> categoryWiseSpending = financeEntryService.getCategoryWiseSpendingForCurrentMonth(userId);
        return ResponseEntity.ok(categoryWiseSpending);
    }

    @GetMapping("/get/summary/income/{userId}")
    public ResponseEntity<Map<String, Double>> getCategoryWiseIncomeForCurrentMonth(@PathVariable Long userId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();

        if (!userDetails.getId().equals(userId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        Map<String, Double> categoryWiseIncome = financeEntryService.getCategoryWiseIncomeForCurrentMonth(userId);
        return ResponseEntity.ok(categoryWiseIncome);
    }
}
