package com.quickpoll.backend.repository;

import com.quickpoll.backend.model.Poll;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface PollRepository extends JpaRepository<Poll, Long> {
    Optional<Poll> findBySecretKey(String secretKey);
}