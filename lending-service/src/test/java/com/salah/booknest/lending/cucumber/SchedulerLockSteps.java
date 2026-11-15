package com.salah.booknest.lending.cucumber;

import com.salah.booknest.lending.job.DueReminderJob;
import com.salah.booknest.lending.job.HoldExpiryJob;
import com.salah.booknest.lending.support.ConcurrentRunner;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import net.javacrumbs.shedlock.core.DefaultLockingTaskExecutor;
import net.javacrumbs.shedlock.core.LockConfiguration;
import net.javacrumbs.shedlock.core.LockingTaskExecutor;
import net.javacrumbs.shedlock.provider.jdbctemplate.JdbcTemplateLockProvider;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Steps of scheduled-jobs.feature. The test profile turns the job lock off (the other scenarios call the jobs
 * directly), so these steps use ShedLock itself: two "instances" share the real shedlock table of this database,
 * with the lock settings read from the jobs' own @SchedulerLock annotations.
 */
public class SchedulerLockSteps {

    @Autowired private DataSource dataSource;

    private final AtomicInteger runs = new AtomicInteger();
    private LockingTaskExecutor instanceOne;
    private LockingTaskExecutor instanceTwo;
    private SchedulerLock lock;
    private String lockName;
    private Duration atLeast;
    private Duration atMost;

    @Given("two lending-service instances share the database")
    public void twoInstances() {
        instanceOne = new DefaultLockingTaskExecutor(lockProvider());
        instanceTwo = new DefaultLockingTaskExecutor(lockProvider());
    }

    @Given("both use the lock of the {string}")
    public void lockOf(String job) {
        lock = annotation(job);
        // a fresh name for each scenario, so a lock left by an earlier scenario cannot interfere
        lockName = lock.name() + "-" + UUID.randomUUID();
        atLeast = duration(lock.lockAtLeastFor());
        atMost = duration(lock.lockAtMostFor());
    }

    @Given("both use a lock that is kept for at least {int} second(s)")
    public void shortLock(int seconds) {
        lockName = "test-job-" + UUID.randomUUID();
        atLeast = Duration.ofSeconds(seconds);
        atMost = Duration.ofMinutes(1);
    }

    @When("both instances start the job at the same moment")
    public void bothStartTogether() throws Exception {
        List<Callable<Object>> starts = List.of(
                () -> { run(instanceOne); return null; },
                () -> { run(instanceTwo); return null; });
        ConcurrentRunner.runTogether(starts);
    }

    @When("the first instance runs the job")
    public void firstRuns() {
        run(instanceOne);
    }

    @When("the second instance starts the same job right after")
    public void secondRunsRightAfter() {
        run(instanceTwo);
    }

    @When("the second instance starts the same job {int} second(s) later")
    public void secondRunsLater(int seconds) throws InterruptedException {
        Thread.sleep(seconds * 1000L);
        run(instanceTwo);
    }

    @Then("the job ran {int} time(s)")
    public void jobRan(int times) {
        assertEquals(times, runs.get());
    }

    @Then("the shedlock table says the job is taken until at least {int} minutes from now")
    public void tableSaysTaken(int minutes) {
        Instant until = new JdbcTemplate(dataSource).queryForObject(
                "select lock_until from shedlock where name = ?", java.sql.Timestamp.class, lockName).toInstant();
        assertTrue(until.isAfter(Instant.now().plus(Duration.ofMinutes(minutes)).minusSeconds(30)), "taken until " + until);
    }

    @Then("the {string} is locked under the name {string} for at least {string} and at most {string}")
    public void declaresLock(String job, String name, String least, String most) {
        SchedulerLock declared = annotation(job);
        assertNotNull(declared, job + " has no @SchedulerLock");
        assertEquals(name, declared.name());
        assertEquals(least, declared.lockAtLeastFor());
        assertEquals(most, declared.lockAtMostFor());
    }

    // ---------- helpers ----------

    /** The job: it takes a moment, like a real run, and counts how often it really ran. */
    private void run(LockingTaskExecutor instance) {
        instance.executeWithLock((Runnable) () -> {
            runs.incrementAndGet();
            try {
                Thread.sleep(300);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, new LockConfiguration(Instant.now(), lockName, atMost, atLeast));
    }

    private JdbcTemplateLockProvider lockProvider() {
        return new JdbcTemplateLockProvider(JdbcTemplateLockProvider.Configuration.builder()
                .withJdbcTemplate(new JdbcTemplate(dataSource))
                .usingDbTime()
                .build());
    }

    private static SchedulerLock annotation(String job) {
        try {
            return switch (job) {
                case "due reminder job" -> DueReminderJob.class.getMethod("sendDueReminders").getAnnotation(SchedulerLock.class);
                case "hold expiry job" -> HoldExpiryJob.class.getMethod("expireHolds").getAnnotation(SchedulerLock.class);
                default -> throw new IllegalArgumentException("unknown job " + job);
            };
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException(e);
        }
    }

    /** "5m", "30s", "2h" as in the annotations. */
    private static Duration duration(String text) {
        long amount = Long.parseLong(text.substring(0, text.length() - 1));
        return switch (text.charAt(text.length() - 1)) {
            case 's' -> Duration.ofSeconds(amount);
            case 'm' -> Duration.ofMinutes(amount);
            case 'h' -> Duration.ofHours(amount);
            default -> Duration.parse(text);
        };
    }
}
