package com.salah.booknest.lending.job;

import com.salah.booknest.lending.event.LendingEvent;
import com.salah.booknest.lending.event.LendingEventPublisher;
import com.salah.booknest.lending.model.Loan;
import com.salah.booknest.lending.model.LoanStatus;
import com.salah.booknest.lending.repository.LoanRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
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

    private final LoanRepository loanRepository;
    private final LendingEventPublisher eventPublisher;

    // remind members whose book is due tomorrow; notification-service sends the emails.
    // only one instance sends them: the others find the lock taken and skip, so nobody gets the reminder twice.
    // lockAtLeastFor keeps it taken for a while, even when the run is quick, so an instance whose clock is
    // a little behind cannot start the same 09:00 run again.
    @Scheduled(cron = "${app.reminders.cron}")
    @SchedulerLock(name = "dueReminders", lockAtMostFor = "10m", lockAtLeastFor = "5m")
    public void sendDueReminders() {
        List<Loan> loans = loanRepository.findAllByStatusAndDueDate(LoanStatus.BORROWED, LocalDate.now().plusDays(1));
        if (loans.isEmpty()) {
            return;
        }
        long started = System.currentTimeMillis();
        log.info("Due reminders started for {} loan(s)", loans.size());

        // all events are sent at the same time; wait until Kafka has confirmed or refused every one of them
        List<CompletableFuture<?>> sending = new ArrayList<>();
        for (Loan loan : loans) {
            String message = "\"" + loan.getBookTitle() + "\" is due tomorrow (" + loan.getDueDate()
                    + "). Please return it on time to avoid a late fine.";
            sending.add(eventPublisher.send(LendingEventPublisher.LOAN_EVENTS, LendingEvent.forMember(
                    "DUE_TOMORROW", loan.getUserId(), loan.getUsername(), loan.getId(), loan.getStatus().name(), message)));
        }
        CompletableFuture.allOf(sending.toArray(new CompletableFuture[0]))
                .exceptionally(error -> null)
                .join();
        long failed = sending.stream().filter(CompletableFuture::isCompletedExceptionally).count();
        log.info("Due reminders finished in {} ms: {} sent, {} failed",
                System.currentTimeMillis() - started, loans.size() - failed, failed);
    }
}
