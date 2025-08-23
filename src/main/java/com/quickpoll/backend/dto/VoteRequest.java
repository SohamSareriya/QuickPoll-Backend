package com.quickpoll.backend.dto;

import lombok.Data;

@Data
public class VoteRequest {
    private Long optionId;
    private String voterToken;
}
