package com.salah.booknest.job;

import com.salah.booknest.model.response.DueReminder;
import com.salah.booknest.service.AsyncEmailService;
import com.salah.booknest.service.ReminderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Component
@RequiredArgsConstructor
public class DueReminderJob {

    private final ReminderService reminderService;
    private final AsyncEmailService asyncEmailService;

    //remind members whose book is due tomorrow
    @Scheduled(cron = "${app.reminders.cron}")
    public void sendDueReminders() {
        List<DueReminder> reminders = reminderService.findLoansDueOn(LocalDate.now().plusDays(1));
        if (reminders.isEmpty()) {
            return;
        }
        long started = System.currentTimeMillis();
        log.info("Due reminders started for {} loan(s)", reminders.size());

        // all emails are sent at the same time on the notification pool
        List<CompletableFuture<Boolean>> sending = new ArrayList<>();
        for (DueReminder reminder : reminders) {
            String message = "\"" + reminder.bookTitle() + "\" is due tomorrow (" + reminder.dueDate()
                    + "). Please return it on time to avoid a late fine.";
            sending.add(asyncEmailService.sendNotificationEmail(
                    reminder.username(), reminder.email(), "BookNest - Book due tomorrow", message));
        }
        CompletableFuture.allOf(sending.toArray(new CompletableFuture[0])).join();

        int sent = 0;
        for (CompletableFuture<Boolean> result : sending) {
            if (result.join()) {
                sent++;
            }
        }
        log.info("Due reminders finished in {} ms: {} sent, {} failed",
                System.currentTimeMillis() - started, sent, reminders.size() - sent);
    }
}