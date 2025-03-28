package com.example.personal_finance_tracker.app.routes;

import com.example.personal_finance_tracker.app.models.FinanceEntry;
import com.example.personal_finance_tracker.app.models.User;
import com.example.personal_finance_tracker.app.repository.FinanceEntryQueryRepository;
import com.example.personal_finance_tracker.app.security.UserDetailsImpl;
import com.example.personal_finance_tracker.app.services.FinanceEntryService;
import com.example.personal_finance_tracker.app.services.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class FinanceEntryController {
    private final FinanceEntryService financeEntryService;
    private final UserService userService;
    private final FinanceEntryQueryRepository financeEntryRepository;

    @PostMapping("/get")
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

    @PostMapping("/get/{type}")
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


    @PostMapping("/get/summary/expense/{userId}")
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

    @PostMapping("/get/summary/income/{userId}")
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

    @PostMapping("/get/admin/transactions")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public List<FinanceEntry> getAllFinanceEntries() {
        return financeEntryService.findAll();
    }

    @PostMapping("/get/admin/users")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public List<User> getAllUsers() {
        return userService.getAllUsers();
    }

    @GetMapping("/search")
    public ResponseEntity<Page<FinanceEntry>> searchFinanceEntries(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) BigDecimal minAmount,
            @RequestParam(required = false) BigDecimal maxAmount,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String searchTerm,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "date,desc") String[] sort) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(createSortOrder(sort)));

        Page<FinanceEntry> entries = financeEntryRepository.findFinanceEntriesWithFilters(
                getCurrentUserId(),
                type,
                category,
                minAmount,
                maxAmount,
                startDate,
                endDate,
                searchTerm,
                pageable
        );

        return ResponseEntity.ok(entries);
    }

    private Sort.Order[] createSortOrder(String[] sort) {
        List<Sort.Order> orders = new ArrayList<>();
        
        if (sort[0].contains(",")) {
            // Multiple sort criteria
            for (String sortOrder : sort) {
                String[] parts = sortOrder.split(",");
                orders.add(new Sort.Order(
                        parts.length > 1 && parts[1].equalsIgnoreCase("desc") ? 
                        Sort.Direction.DESC : Sort.Direction.ASC,
                        parts[0]
                ));
            }
        } else {
            // Single sort criteria
            orders.add(new Sort.Order(
                    sort.length > 1 && sort[1].equalsIgnoreCase("desc") ? 
                    Sort.Direction.DESC : Sort.Direction.ASC,
                    sort[0]
            ));
        }
        
        return orders.toArray(new Sort.Order[0]);
    }
    
    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
            return userDetails.getId();
        }
        throw new IllegalStateException("User not authenticated");
    }
}
