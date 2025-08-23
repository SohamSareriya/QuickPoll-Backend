package com.quickpoll.backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
@RequiredArgsConstructor
@Slf4j
public class SseService {

    private final ObjectMapper objectMapper;
    private final Map<Long, CopyOnWriteArrayList<SseEmitter>> pollEmitters = new ConcurrentHashMap<>();
    private final Map<Long, CopyOnWriteArrayList<SseEmitter>> creatorEmitters = new ConcurrentHashMap<>();

    public SseEmitter createEmitter(Long pollId) {
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);
        pollEmitters.computeIfAbsent(pollId, k -> new CopyOnWriteArrayList<>()).add(emitter);
        emitter.onCompletion(() -> removeEmitter(pollId, emitter));
        emitter.onTimeout(() -> removeEmitter(pollId, emitter));
        emitter.onError((ex) -> {
            log.error("SSE error for poll {}: {}", pollId, ex.getMessage());
            removeEmitter(pollId, emitter);
        });
        sendInitialMessage(emitter, pollId);
        return emitter;
    }

    public SseEmitter createCreatorEmitter(Long pollId, String secretKey) {
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);
        creatorEmitters.computeIfAbsent(pollId, k -> new CopyOnWriteArrayList<>()).add(emitter);
        emitter.onCompletion(() -> removeCreatorEmitter(pollId, emitter));
        emitter.onTimeout(() -> removeCreatorEmitter(pollId, emitter));
        emitter.onError((ex) -> {
            log.error("Creator SSE error for poll {}: {}", pollId, ex.getMessage());
            removeCreatorEmitter(pollId, emitter);
        });
        sendInitialMessage(emitter, pollId);
        return emitter;
    }

    public void broadcastVoteUpdate(Long pollId, Object voteData) {
        broadcastToEmitters(pollEmitters.get(pollId), "vote-update", voteData);
        broadcastToEmitters(creatorEmitters.get(pollId), "vote-update", voteData);
    }

    public void broadcastAutoInsight(Long pollId, String insight) {
        Map<String, String> insightData = Map.of("insight", insight);
        broadcastToEmitters(pollEmitters.get(pollId), "auto-insight", insightData);
        broadcastToEmitters(creatorEmitters.get(pollId), "auto-insight", insightData);
    }

    public void broadcastResultsVisibilityChange(Long pollId, boolean visible) {
        Map<String, Boolean> visibilityData = Map.of("resultsVisible", visible);
        broadcastToEmitters(pollEmitters.get(pollId), "visibility-change", visibilityData);
        broadcastToEmitters(creatorEmitters.get(pollId), "visibility-change", visibilityData);
    }

    private void broadcastToEmitters(CopyOnWriteArrayList<SseEmitter> emitters,
            String eventName, Object data) {
        if (emitters != null) {
            emitters.removeIf(emitter -> {
                try {
                    emitter.send(SseEmitter.event()
                            .name(eventName)
                            .data(objectMapper.writeValueAsString(data)));
                    return false;
                } catch (IOException e) {
                    log.error("Error sending SSE message: {}", e.getMessage());
                    return true;
                }
            });
        }
    }

    private void sendInitialMessage(SseEmitter emitter, Long pollId) {
        try {
            emitter.send(SseEmitter.event()
                    .name("connected")
                    .data("Connected to poll " + pollId));
        } catch (IOException e) {
            log.error("Error sending initial SSE message", e);
        }
    }

    private void removeEmitter(Long pollId, SseEmitter emitter) {
        CopyOnWriteArrayList<SseEmitter> emitters = pollEmitters.get(pollId);
        if (emitters != null) {
            emitters.remove(emitter);
            if (emitters.isEmpty()) {
                pollEmitters.remove(pollId);
            }
        }
    }

    private void removeCreatorEmitter(Long pollId, SseEmitter emitter) {
        CopyOnWriteArrayList<SseEmitter> emitters = creatorEmitters.get(pollId);
        if (emitters != null) {
            emitters.remove(emitter);
            if (emitters.isEmpty()) {
                creatorEmitters.remove(pollId);
            }
        }
    }
}
