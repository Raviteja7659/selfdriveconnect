package com.selfdriveconnect.bookingservice.realtime;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * The server-push half of "live updates without polling" — the same
 * user-experience goal as Uber's live trip status, built with Server-
 * Sent Events instead of a full WebSocket broker, since this only
 * needs one-way server-to-browser push (a renter/owner never needs to
 * send anything back over this channel; actions still go through the
 * normal REST endpoints).
 *
 * One user can have multiple open tabs/devices, hence a List of
 * emitters per userId rather than a single one.
 */
@Component
@Slf4j
public class RealtimeBroadcaster {

    private final Map<Long, List<SseEmitter>> emittersByUserId = new ConcurrentHashMap<>();

    public SseEmitter register(Long userId) {
        SseEmitter emitter = new SseEmitter(30 * 60 * 1000L); // 30 minute connection lifetime
        emittersByUserId.computeIfAbsent(userId, id -> new CopyOnWriteArrayList<>()).add(emitter);

        emitter.onCompletion(() -> removeEmitter(userId, emitter));
        emitter.onTimeout(() -> removeEmitter(userId, emitter));
        emitter.onError(e -> removeEmitter(userId, emitter));

        return emitter;
    }

    private void removeEmitter(Long userId, SseEmitter emitter) {
        List<SseEmitter> emitters = emittersByUserId.get(userId);
        if (emitters != null) {
            emitters.remove(emitter);
        }
    }

    /**
     * Sends an event to every open connection for a user (if any —
     * broadcasting to a user with no open tab is a harmless no-op,
     * same as a push notification to a signed-out device).
     */
    public void sendToUser(Long userId, String eventName, Object payload) {
        List<SseEmitter> emitters = emittersByUserId.get(userId);
        if (emitters == null || emitters.isEmpty()) {
            return;
        }

        for (SseEmitter emitter : List.copyOf(emitters)) {
            try {
                emitter.send(SseEmitter.event().name(eventName).data(payload));
            } catch (IOException e) {
                log.debug("Dropping stale SSE connection for user {}: {}", userId, e.getMessage());
                removeEmitter(userId, emitter);
            }
        }
    }
}
