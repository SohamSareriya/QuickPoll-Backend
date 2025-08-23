package com.quickpoll.backend.repository;

import com.quickpoll.backend.model.PollOption;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PollOptionRepository extends JpaRepository<PollOption, Long> {
}
