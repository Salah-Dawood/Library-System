package com.salah.booknest.service;

import com.salah.booknest.model.response.NotificationEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;


@Slf4j
@Service
public class NotificationService {

    private static final long TIMEOUT_MS = Duration.ofMinutes(30).toMillis();

    private final Map<String, List<SseEmitter>> emitters = new ConcurrentHashMap<>();
    private final Set<String> librarians = ConcurrentHashMap.newKeySet();

    public SseEmitter subscribe(String username, boolean librarian) {
        SseEmitter emitter = new SseEmitter(TIMEOUT_MS);
        emitters.computeIfAbsent(username, key -> new CopyOnWriteArrayList<>()).add(emitter);
        if (librarian) {
            librarians.add(username);
        }
        emitter.onCompletion(() -> remove(username, emitter));
        emitter.onTimeout(() -> remove(username, emitter));
        emitter.onError(error -> remove(username, emitter));

        send(username, emitter,
                new NotificationEvent("CONNECTED", "Listening for notifications", null, null, LocalDateTime.now()));
        log.info("User {} opened a notification stream", username);
        return emitter;
    }

    public void notifyUser(String username, NotificationEvent event) {
        List<SseEmitter> userEmitters = emitters.get(username);
        if (userEmitters != null) {
            userEmitters.forEach(emitter -> send(username, emitter, event));
        }
    }

    public void notifyLibrarians(NotificationEvent event) {
        librarians.forEach(username -> notifyUser(username, event));
    }

    private void send(String username, SseEmitter emitter, NotificationEvent event) {
        try {
            emitter.send(SseEmitter.event().name(event.type()).data(event));
        } catch (IOException | IllegalStateException e) {
            // The client went away; drop this connection instead of failing the business operation.
            remove(username, emitter);
        }
    }

    private void remove(String username, SseEmitter emitter) {
        List<SseEmitter> userEmitters = emitters.get(username);
        if (userEmitters == null) {
            return;
        }
        userEmitters.remove(emitter);
        if (userEmitters.isEmpty()) {
            emitters.remove(username);
            librarians.remove(username);
        }
    }
}