package com.salah.booknest.job;

import com.salah.booknest.service.WaitlistService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class HoldExpiryJob {

    private static final int TIMEOUT_SECONDS = 30;

    private final WaitlistService waitlistService;
    private final ThreadPoolExecutor threadPoolExecutor;

    // every minute: expire the holds that were not claimed
    @Scheduled(fixedDelay = 60000)
    public void expireHolds() {
        List<Long> ids = waitlistService.getExpiredHoldIds();
        if (ids.isEmpty()) {
            return;
        }
        long started = System.currentTimeMillis();
        log.info("Hold expiry started for {} hold(s)", ids.size());

        List<Callable<Boolean>> tasks = new ArrayList<>();
        for (Long id : ids) {
            tasks.add(() -> waitlistService.expireHold(id));
        }

        int expired = 0;
        int skipped = 0;
        int failed = 0;
        try {
            // waits until every task is done
            List<Future<Boolean>> results = threadPoolExecutor.invokeAll(tasks, TIMEOUT_SECONDS, TimeUnit.SECONDS);
            for (int i = 0; i < results.size(); i++) {
                try {
                    if (results.get(i).get()) {
                        expired++;
                    } else {
                        skipped++;
                    }
                } catch (ExecutionException e) {
                    failed++;
                    log.error("Could not expire hold {}", ids.get(i), e.getCause());
                } catch (CancellationException e) {
                    failed++;
                    log.error("Expiring hold {} took longer than {} seconds and was cancelled", ids.get(i), TIMEOUT_SECONDS);
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Hold expiry was interrupted");
            return;
        }
        log.info("Hold expiry finished in {} ms: {} expired, {} skipped, {} failed",
                System.currentTimeMillis() - started, expired, skipped, failed);
    }
}
