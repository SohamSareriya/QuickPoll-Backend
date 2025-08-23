package com.quickpoll.backend.controller;

import com.quickpoll.backend.service.SseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/stream")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Slf4j
public class SseController {

    private final SseService sseService;

    @GetMapping("/poll/{pollId}")
    public SseEmitter streamPollResults(@PathVariable Long pollId) {
        log.info("New SSE connection for poll: {}", pollId);
        return sseService.createEmitter(pollId);
    }

    @GetMapping("/poll/{pollId}/creator")
    public SseEmitter streamCreatorUpdates(@PathVariable Long pollId,
            @RequestParam String secretKey) {
        log.info("Creator SSE connection for poll: {}", pollId);
        return sseService.createCreatorEmitter(pollId, secretKey);
    }
}
