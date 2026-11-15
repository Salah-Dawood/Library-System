package com.salah.booknest.notification.cucumber;

import jakarta.mail.Address;
import jakarta.mail.internet.MimeMessage;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * The emails the service sent during a scenario. Nothing really leaves the test.
 * The mail server can be made to fail, to check what happens to the event then.
 */
public class SentEmails {

    public record Email(String to, String subject) {
    }

    private final List<Email> emails = new CopyOnWriteArrayList<>();
    // how many of the next send attempts fail (-1: every one)
    private final AtomicInteger failuresLeft = new AtomicInteger();
    private final AtomicInteger attempts = new AtomicInteger();

    void send(MimeMessage message) {
        attempts.incrementAndGet();
        if (failuresLeft.get() == -1 || failuresLeft.getAndUpdate(n -> n > 0 ? n - 1 : n) > 0) {
            throw new IllegalStateException("The mail server is not answering");
        }
        try {
            List<String> to = new ArrayList<>();
            for (Address address : message.getAllRecipients()) {
                to.add(address.toString());
            }
            emails.add(new Email(String.join(",", to), message.getSubject()));
        } catch (Exception e) {
            throw new IllegalStateException("Could not read a sent email", e);
        }
    }

    public List<Email> all() {
        return emails;
    }

    public int attempts() {
        return attempts.get();
    }

    /** The next {@code times} sends fail; -1 makes every send fail. */
    public void failNext(int times) {
        failuresLeft.set(times);
    }

    public void clear() {
        emails.clear();
        attempts.set(0);
        failuresLeft.set(0);
    }
}
