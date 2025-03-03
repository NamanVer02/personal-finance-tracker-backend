package com.example.personal_finance_tracker.app.routes;

import com.example.personal_finance_tracker.app.models.FinanceEntry;
import com.example.personal_finance_tracker.app.services.FinanceEntryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("api/")
@RequiredArgsConstructor
public class FinanceEntryController {
    private final FinanceEntryService financeEntryService;

    @GetMapping("/get")
    public List<FinanceEntry> getAll () {
        return financeEntryService.findAll();
    }

    @GetMapping("/get/{type}")
    public List<FinanceEntry> getByType (@PathVariable String type) {
        return financeEntryService.findByType(type);
    }

    @PostMapping("/post")
    public FinanceEntry create (@RequestBody FinanceEntry financeEntry) {
        return financeEntryService.create(financeEntry);
    }

    @PutMapping("/put/{id}")
    public FinanceEntry update(@PathVariable Long id, @RequestBody FinanceEntry financeEntry) throws Exception {
        return financeEntryService.update(id, financeEntry);
    }

    @DeleteMapping("delete/{id}")
    public void delete (@PathVariable Long id) {
        financeEntryService.deleteById(id);
    }
}
