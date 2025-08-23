package com.quickpoll.backend.dto;

import lombok.Data;

@Data
public class CreatePollRequest {
    private String question;
    private String options;
    private int expiryHours;
}
