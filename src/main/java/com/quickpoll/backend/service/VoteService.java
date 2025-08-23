package com.quickpoll.backend.service;

import com.quickpoll.backend.model.Poll;
import com.quickpoll.backend.model.PollOption;
import com.quickpoll.backend.model.Vote;
import com.quickpoll.backend.repository.PollOptionRepository;
import com.quickpoll.backend.repository.VoteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class VoteService {

    private final VoteRepository voteRepository;
    private final PollOptionRepository pollOptionRepository;
    private final SseService sseService;
    private final PollService pollService;

    public boolean hasVoted(Poll poll, String voterToken) {
        return voteRepository.existsByPollAndVoterToken(poll, voterToken);
    }

    public Vote submitVote(Poll poll, Long optionId, String voterToken) throws IllegalArgumentException {
        if (hasVoted(poll, voterToken)) {
            throw new IllegalArgumentException("User has already voted");
        }

        PollOption option = pollOptionRepository.findById(optionId)
                .orElseThrow(() -> new IllegalArgumentException("Option not found"));

        option.setVotes(option.getVotes() + 1);
        pollOptionRepository.save(option);

        Vote vote = new Vote();
        vote.setPoll(poll);
        vote.setOption(option);
        vote.setVoterToken(voterToken);
        vote.setCreatedAt(Instant.now());

        Vote savedVote = voteRepository.save(vote);

        broadcastVoteUpdate(poll);

        pollService.computeAutoInsight(poll)
                .ifPresent(insight -> sseService.broadcastAutoInsight(poll.getId(), insight));

        return savedVote;
    }

    private void broadcastVoteUpdate(Poll poll) {
        int totalVotes = poll.getOptions().stream().mapToInt(PollOption::getVotes).sum();

        Map<String, Object> voteData = Map.of(
                "pollId", poll.getId(),
                "totalVotes", totalVotes,
                "options", poll.getOptions().stream().collect(Collectors.toMap(
                        opt -> opt.getId().toString(),
                        opt -> Map.of(
                                "votes", opt.getVotes(),
                                "percentage", totalVotes > 0 ? (opt.getVotes() * 100.0 / totalVotes) : 0.0))));

        sseService.broadcastVoteUpdate(poll.getId(), voteData);
    }

    public Long getUserVotedOptionId(Poll poll, String voterToken) {
        Optional<Vote> existingVote = voteRepository.findByPollAndVoterToken(poll, voterToken);
        return existingVote.map(vote -> vote.getOption().getId()).orElse(null);
    }

}
