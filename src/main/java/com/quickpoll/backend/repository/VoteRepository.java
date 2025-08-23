package com.quickpoll.backend.repository;

import com.quickpoll.backend.model.Vote;
import com.quickpoll.backend.model.Poll;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface VoteRepository extends JpaRepository<Vote, Long> {
    boolean existsByPollAndVoterToken(Poll poll, String voterToken);

    Optional<Vote> findByPollAndVoterToken(Poll poll, String voterToken);
}
