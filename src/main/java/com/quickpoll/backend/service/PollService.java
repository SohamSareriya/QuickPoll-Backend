package com.quickpoll.backend.service;

import com.quickpoll.backend.model.Poll;
import com.quickpoll.backend.model.PollOption;
import com.quickpoll.backend.repository.PollOptionRepository;
import com.quickpoll.backend.repository.PollRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PollService {

    private final PollRepository pollRepository;
    private final PollOptionRepository pollOptionRepository;
    private final SseService sseService;
    private final SecureRandom secureRandom = new SecureRandom();

    public Poll createPoll(String question, String optionsDelimited, int expiryHours) {
        Poll poll = new Poll();
        poll.setQuestion(question);
        poll.setExpiresAt(Instant.now().plusSeconds(expiryHours * 3600L));
        poll.setCreatedAt(Instant.now());
        poll.setResultsVisible(false);
        poll.setSecretKey(generateSecretKey());

        Poll savedPoll = pollRepository.save(poll);

        List<String> optionsList = Arrays.stream(optionsDelimited.split("[|,/]"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .limit(4)
                .collect(Collectors.toList());

        List<PollOption> optionEntities = new ArrayList<>();

        for (String optText : optionsList) {
            PollOption option = new PollOption();
            option.setPoll(savedPoll);
            option.setOptionText(optText);
            option.setVotes(0);
            optionEntities.add(option);
        }

        pollOptionRepository.saveAll(optionEntities);
        savedPoll.setOptions(optionEntities);

        return savedPoll;
    }

    private String generateSecretKey() {
        byte[] bytes = new byte[16];
        secureRandom.nextBytes(bytes);
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    public Optional<Poll> findById(Long pollId) {
        return pollRepository.findById(pollId);
    }

    public Optional<Poll> findBySecretKey(String secretKey) {
        return pollRepository.findBySecretKey(secretKey);
    }

    public boolean isExpired(Poll poll) {
        return Instant.now().isAfter(poll.getExpiresAt());
    }

    public Poll toggleResultsVisibility(Poll poll, boolean visible) {
        poll.setResultsVisible(visible);
        Poll savedPoll = pollRepository.save(poll);
        sseService.broadcastResultsVisibilityChange(poll.getId(), visible);
        return savedPoll;
    }

    public Optional<String> computeAutoInsight(Poll poll) {
        List<PollOption> options = poll.getOptions();
        int totalVotes = options.stream().mapToInt(PollOption::getVotes).sum();
        if (totalVotes < 20) {
            return Optional.empty();
        }
        options.sort(Comparator.comparingInt(PollOption::getVotes).reversed());

        PollOption topOption = options.get(0);
        double topPct = (topOption.getVotes() * 100.0) / totalVotes;
        double margin;
        String insight;

        if (options.size() > 1) {
            margin = ((topOption.getVotes() - options.get(1).getVotes()) * 100.0) / totalVotes;
            if (margin <= 10.0) {
                insight = "Results are close between top options.";
            } else {
                insight = String.format("%s leads with %.1f%% votes.", topOption.getOptionText(), topPct);
            }
        } else {
            insight = String.format("%s leads with %.1f%% votes.", topOption.getOptionText(), topPct);
        }
        return Optional.of(insight);
    }
}
