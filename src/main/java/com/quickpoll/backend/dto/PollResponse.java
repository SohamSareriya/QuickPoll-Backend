package com.quickpoll.backend.dto;

import lombok.Builder;
import lombok.Data;
import java.time.Instant;
import java.util.List;

@Data
@Builder
public class PollResponse {
    private Long id;
    private String question;
    private Instant expiresAt;
    private Boolean resultsVisible;
    private String secretKey;
    private Long userVotedOptionId;
    private String insight;
    private List<OptionResponse> options;

    @Data
    @Builder
    public static class OptionResponse {
        private Long id;
        private String optionText;
        private int votes;
    }
}
