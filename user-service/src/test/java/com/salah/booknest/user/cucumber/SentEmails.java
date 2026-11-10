package com.salah.booknest.user.cucumber;

import jakarta.mail.Address;
import jakarta.mail.Multipart;
import jakarta.mail.Part;
import jakarta.mail.internet.MimeMessage;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * The emails the service tried to send during a scenario. Nothing really leaves the test; the
 * scenarios read these to check who got which email and to take links or codes out of them.
 */
public class SentEmails {

    public record Email(String to, String subject, String html) {
    }

    private final List<Email> emails = new CopyOnWriteArrayList<>();

    void add(MimeMessage message) {
        try {
            // a real send does this; it fills in the content types the parts are read by
            message.saveChanges();
            List<String> to = new ArrayList<>();
            for (Address address : message.getAllRecipients()) {
                to.add(address.toString());
            }
            emails.add(new Email(String.join(",", to), message.getSubject(), html(message)));
        } catch (Exception e) {
            throw new IllegalStateException("Could not read a sent email", e);
        }
    }

    public List<Email> all() {
        return emails;
    }

    public void clear() {
        emails.clear();
    }

    // the HTML body sits somewhere inside the parts of the message
    private String html(Part part) throws Exception {
        if (part.isMimeType("text/html")) {
            return (String) part.getContent();
        }
        if (part.getContent() instanceof Multipart multipart) {
            for (int i = 0; i < multipart.getCount(); i++) {
                String html = html(multipart.getBodyPart(i));
                if (html != null) {
                    return html;
                }
            }
        }
        return null;
    }
}
