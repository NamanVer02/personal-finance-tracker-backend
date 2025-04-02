package com.example.personal_finance_tracker.app.services;

import com.example.personal_finance_tracker.app.models.DemoEntity;
import com.example.personal_finance_tracker.app.repository.DemoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class DemoService {

    @Autowired
    private DemoRepository demoRepository;

    public void performDatabaseOperation(int index) {
        DemoEntity entity = new DemoEntity();
        entity.setName("Test " + index);
        demoRepository.save(entity); // Save operation to test connection usage
    }

}
