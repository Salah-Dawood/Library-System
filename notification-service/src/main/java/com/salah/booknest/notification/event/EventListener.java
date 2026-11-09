package com.salah.booknest.notification.event;

import com.salah.booknest.notification.model.NotificationEvent;
import com.salah.booknest.notification.model.Recipient;
import com.salah.booknest.notification.repository.RecipientRepository;
import com.salah.booknest.notification.service.AsyncEmailService;
import com.salah.booknest.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

/**
 * Reacts to events from the other services: remembers email addresses and sends emails and live notifications.
 * The events arrive as JSON text and are read into this service's own records.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EventListener {

    public static final String USER_EVENTS = "user-events";
    public static final String LOAN_EVENTS = "loan-events";
    public static final String WAITLIST_EVENTS = "waitlist-events";

    private final JsonMapper jsonMapper;
    private final RecipientRepository recipientRepository;
    private final NotificationService notificationService;
    private final AsyncEmailService asyncEmailService;

    @KafkaListener(topics = USER_EVENTS)
    public void onUserEvent(String json) {
        UserRegisteredEvent event = jsonMapper.readValue(json, UserRegisteredEvent.class);
        recipientRepository.save(new Recipient(event.userId(), event.username(), event.email()));
        log.info("Recipient {} saved from UserRegistered event {}", event.username(), event.eventId());
    }

    @KafkaListener(topics = {LOAN_EVENTS, WAITLIST_EVENTS})
    public void onLendingEvent(String json) {
        LendingEvent event = jsonMapper.readValue(json, LendingEvent.class);
        NotificationEvent live = new NotificationEvent(event.type(), event.message(), event.loanId(), event.status(), event.occurredAt());
        log.info("Event {} ({}) for {}", event.eventId(), event.type(), event.forLibrarians() ? "librarians" : event.username());

        if (event.forLibrarians()) {
            notificationService.notifyLibrarians(live);
            return;
        }
        notificationService.notifyUser(event.username(), live);
        String email = recipientRepository.findById(event.userId()).map(Recipient::getEmail).orElse(null);
        asyncEmailService.sendNotificationEmail(event.username(), email, emailSubject(event.type()), event.message());
    }

    private String emailSubject(String type) {
        return switch (type) {
            case "LOAN_CANCELLED" -> "BookNest - Your loan was cancelled";
            case "LOAN_RETURNED" -> "BookNest - We received your return";
            case "RETURN_REVIEWED" -> "BookNest - Your return was reviewed";
            case "HOLD_READY" -> "BookNest - A copy is ready for you";
            case "HOLD_EXPIRED" -> "BookNest - Your hold expired";
            case "DUE_TOMORROW" -> "BookNest - Book due tomorrow";
            default -> "BookNest - Update about your loan";
        };
    }
}
