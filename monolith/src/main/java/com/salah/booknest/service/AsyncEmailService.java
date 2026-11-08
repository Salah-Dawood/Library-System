package com.salah.booknest.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.concurrent.CompletableFuture;

/**
 * Sends notification emails on the notificationExecutor pool, so the request that caused the email
 * does not wait for the mail server. The result is true when the email was sent, false when it failed.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AsyncEmailService {

    private final EmailVerificationService emailVerificationService;
    private final TemplateEngine templateEngine;

    @Value("${app.email.gif-url}")
    private String gifUrl;

    @Async("notificationExecutor")
    public CompletableFuture<Boolean> sendNotificationEmail(String username, String email, String subject, String message) {
        String thread = Thread.currentThread().getName();
        if (email == null || email.isBlank()) {
            log.warn("Email '{}' for {} skipped, no email address (thread {})", subject, username, thread);
            return CompletableFuture.completedFuture(false);
        }

        long started = System.currentTimeMillis();
        log.info("Email '{}' for {} started on thread {}", subject, username, thread);
        try {
            Context context = new Context();
            context.setVariable("subject", subject);
            context.setVariable("message", message);
            context.setVariable("Gif", gifUrl);
            String htmlContent = templateEngine.process("notification-email", context);

            emailVerificationService.sendEmail(email, subject, htmlContent);
            log.info("Email '{}' for {} sent in {} ms on thread {}", subject, username, System.currentTimeMillis() - started, thread);
            return CompletableFuture.completedFuture(true);
        } catch (RuntimeException e) {
            log.warn("Email '{}' for {} failed on thread {}: {}", subject, username, thread, e.getMessage());
            return CompletableFuture.completedFuture(false);
        }
    }
}
