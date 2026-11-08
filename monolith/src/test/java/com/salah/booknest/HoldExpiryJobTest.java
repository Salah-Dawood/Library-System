package com.salah.booknest;

import com.salah.booknest.job.HoldExpiryJob;
import com.salah.booknest.model.Book;
import com.salah.booknest.model.WaitlistEntry;
import com.salah.booknest.model.WaitlistStatus;
import com.salah.booknest.support.TestData;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.test.context.ActiveProfiles;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@ActiveProfiles("test")
class HoldExpiryJobTest {

    @Autowired private HoldExpiryJob holdExpiryJob;
    @Autowired private ThreadPoolTaskExecutor notificationExecutor;
    @Autowired private TestData data;

    @Test
    void sixExpiredHolds_jobExpiresAllOfThemAndPutsTheCopiesBack() {
        List<Book> books = new ArrayList<>();
        List<WaitlistEntry> entries = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            Book book = data.newBook(1);
            data.setAvailable(book, 0);
            books.add(book);
            entries.add(data.newHoldEntry(data.newUser(), book, -10));
        }

        holdExpiryJob.expireHolds();

        for (int i = 0; i < 6; i++) {
            assertEquals(WaitlistStatus.EXPIRED, data.reload(entries.get(i)).getStatus());
            assertEquals(1, data.inventory(books.get(i)).getAvailableCopies());
            data.assertStockConsistent(books.get(i));
        }
    }

    @Test
    void holdThatIsNotOverYet_isLeftAlone() {
        Book book = data.newBook(1);
        data.setAvailable(book, 0);
        WaitlistEntry entry = data.newHoldEntry(data.newUser(), book, 60);

        holdExpiryJob.expireHolds();

        assertEquals(WaitlistStatus.HOLD_READY, data.reload(entry).getStatus());
        assertEquals(0, data.inventory(book).getAvailableCopies());
    }

    @Test
    void notificationPoolIsConfiguredAsPlanned() {
        assertEquals(4, notificationExecutor.getCorePoolSize());
        assertEquals(8, notificationExecutor.getMaxPoolSize());
    }
}
