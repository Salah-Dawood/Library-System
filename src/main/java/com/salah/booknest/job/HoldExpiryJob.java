package com.salah.booknest.job;

import com.salah.booknest.service.WaitlistService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class HoldExpiryJob {

    private final WaitlistService waitlistService;

    @Scheduled(fixedDelay = 60000)
    public void expireHolds() {
        log.info("Starting expiry check.");
        List<Long> ids = waitlistService.getExpiredHoldIds();
        for (Long id : ids) {
            try {
                waitlistService.expireHold(id);
            } catch (Exception e) {
                log.error("Could not expire hold {}", id, e);
            }
        }
        if (!ids.isEmpty()) {
            log.info("Hold expiry job checked {} hold(s)", ids.size());
        }
    }
}
