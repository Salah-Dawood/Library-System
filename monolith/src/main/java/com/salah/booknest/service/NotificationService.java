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

    /**
     * Opens a live notification stream (Server-Sent Events) for a user and sends a CONNECTED event.
     * A user can have several open streams, for example in multiple browser tabs. Librarians are
     * remembered so they can receive librarian-wide events.
     */
    public SseEmitter subscribe(String username, boolean librarian) {
        SseEmitter emitter = new SseEmitter(TIMEOUT_MS);
        // one atomic step, so the new stream cannot be lost while this user's last stream is being removed
        emitters.compute(username, (key, streams) -> {
            if (streams == null) {
                streams = new CopyOnWriteArrayList<>();
            }
            streams.add(emitter);
            if (librarian) {
                librarians.add(username);
            }
            return streams;
        });
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

    /**
     * Sends an event to every librarian who currently has an open stream.
     */
    public void notifyLibrarians(NotificationEvent event) {
        librarians.forEach(username -> notifyUser(username, event));
    }

    private void send(String username, SseEmitter emitter, NotificationEvent event) {
        try {
            synchronized (emitter) {
                emitter.send(SseEmitter.event().name(event.type()).data(event));
            }
        } catch (IOException | IllegalStateException e) {
            // The client went away drop this connection
            remove(username, emitter);
        }
    }

    /**
     * Forgets a closed stream; when the user has no streams left they are also removed from the librarian list.
     */
    private void remove(String username, SseEmitter emitter) {
        emitters.computeIfPresent(username, (key, streams) -> {
            streams.remove(emitter);
            if (streams.isEmpty()) {
                librarians.remove(username);
                return null; // returning null removes the user from the map
            }
            return streams;
        });
    }
}