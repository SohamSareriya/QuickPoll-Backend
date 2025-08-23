package com.quickpoll.backend.controller;

import com.quickpoll.backend.dto.VoteRequest;
import com.quickpoll.backend.model.Poll;
import com.quickpoll.backend.model.Vote;
import com.quickpoll.backend.service.DeviceFingerprintService;
import com.quickpoll.backend.service.PollService;
import com.quickpoll.backend.service.RateLimitingService;
import com.quickpoll.backend.service.VoteService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/polls/{pollId}/votes")
@RequiredArgsConstructor
public class VoteController {

    private final PollService pollService;
    private final VoteService voteService;
    private final RateLimitingService rateLimitingService;
    private final DeviceFingerprintService deviceFingerprintService;

    @PostMapping
    public ResponseEntity<?> submitVote(@PathVariable Long pollId,
            @RequestBody VoteRequest request,
            HttpServletRequest httpRequest) {
        if (request.getVoterToken() == null || request.getVoterToken().isBlank()) {
            return ResponseEntity.badRequest().body("Voter token is required");
        }

        var pollOpt = pollService.findById(pollId);
        if (pollOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        Poll poll = pollOpt.get();

        if (pollService.isExpired(poll)) {
            return ResponseEntity.badRequest().body("Poll has expired, voting is closed.");
        }

        String deviceFingerprint = deviceFingerprintService.generateFingerprint(httpRequest);

        if (rateLimitingService.isRateLimited(deviceFingerprint, pollId)) {
            return ResponseEntity.status(429).body("Too many voting attempts from your device. Please try later.");
        }

        if (voteService.hasVoted(poll, request.getVoterToken())) {
            return ResponseEntity.badRequest().body("You have already voted.");
        }

        try {
            Vote vote = voteService.submitVote(poll, request.getOptionId(), request.getVoterToken());
            pollService.toggleResultsVisibility(poll, true);
            return ResponseEntity.ok("Vote recorded successfully.");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
