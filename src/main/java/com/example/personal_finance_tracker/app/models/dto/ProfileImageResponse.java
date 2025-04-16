package com.example.personal_finance_tracker.app.models.dto;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class ProfileImageResponse {
    private String profileImage;

    public ProfileImageResponse(String profileImage) {
        this.profileImage = profileImage;
    }

}

