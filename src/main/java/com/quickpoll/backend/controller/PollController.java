package com.quickpoll.backend.controller;

import com.quickpoll.backend.dto.CreatePollRequest;
import com.quickpoll.backend.dto.PollResponse;
import com.quickpoll.backend.model.Poll;
import com.quickpoll.backend.service.PollService;
import com.quickpoll.backend.service.VoteService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/polls")
@RequiredArgsConstructor
public class PollController {

    private final PollService pollService;
    private final VoteService voteService;

    @PostMapping
    public ResponseEntity<?> createPoll(@RequestBody CreatePollRequest request) {
        if (request.getQuestion() == null || request.getQuestion().isBlank()) {
            return ResponseEntity.badRequest().body("Question is required");
        }

        if (request.getOptions() == null || request.getOptions().isBlank()) {
            return ResponseEntity.badRequest().body("At least two options are required");
        }

        int expiry = request.getExpiryHours() > 0 ? request.getExpiryHours() : 24;
        Poll poll = pollService.createPoll(request.getQuestion(), request.getOptions(), expiry);

        return ResponseEntity.ok(
                PollResponse.builder()
                        .id(poll.getId())
                        .question(poll.getQuestion())
                        .expiresAt(poll.getExpiresAt())
                        .resultsVisible(poll.getResultsVisible())
                        .secretKey(poll.getSecretKey())
                        .options(poll.getOptions().stream().map(opt -> PollResponse.OptionResponse.builder()
                                .id(opt.getId())
                                .optionText(opt.getOptionText())
                                .votes(opt.getVotes())
                                .build())
                                .collect(Collectors.toList()))
                        .build());
    }

    @GetMapping("/{pollId}")
    public ResponseEntity<?> getPoll(@PathVariable Long pollId,
            @RequestParam(required = false) String voterToken) {
        return pollService.findById(pollId)
                .map(poll -> {
                    Long userVotedOptionId = null;
                    if (voterToken != null && !voterToken.isBlank()) {
                        userVotedOptionId = voteService.getUserVotedOptionId(poll, voterToken);
                    }
                    Optional<String> insight = pollService.computeAutoInsight(poll);

                    return ResponseEntity.ok(
                            PollResponse.builder()
                                    .id(poll.getId())
                                    .question(poll.getQuestion())
                                    .expiresAt(poll.getExpiresAt())
                                    .resultsVisible(poll.getResultsVisible())
                                    .secretKey(poll.getSecretKey())
                                    .userVotedOptionId(userVotedOptionId)
                                    .insight(insight.orElse(null))
                                    .options(poll.getOptions().stream().map(opt -> PollResponse.OptionResponse.builder()
                                            .id(opt.getId())
                                            .optionText(opt.getOptionText())
                                            .votes(opt.getVotes())
                                            .build())
                                            .collect(Collectors.toList()))
                                    .build());
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{pollId}/toggle-results")
    public ResponseEntity<?> toggleResults(@PathVariable Long pollId,
            @RequestParam String secretKey,
            @RequestParam boolean visible) {
        return pollService.findById(pollId)
                .map(poll -> {
                    if (!poll.getSecretKey().equals(secretKey)) {
                        return ResponseEntity.status(403).body("Invalid secret key");
                    }
                    pollService.toggleResultsVisibility(poll, visible);
                    return ResponseEntity.ok().body("Result visibility updated");
                })
                .orElse(ResponseEntity.notFound().build());
    }
}
