package com.salah.booknest.support;

import com.salah.booknest.exception.InvalidStateException;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class ConcurrentRunner {

    public record Outcome(Object value, Throwable error) {

        public boolean succeeded() {
            return error == null;
        }

        public String errorCode() {
            if (error instanceof InvalidStateException e) {
                return e.getCode();
            }
            return null;
        }
    }

    // all tasks wait at the gate and start at the same moment
    public static List<Outcome> runTogether(List<Callable<Object>> tasks) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(tasks.size());
        CountDownLatch gate = new CountDownLatch(1);
        List<Future<Outcome>> futures = new ArrayList<>();
        for (Callable<Object> task : tasks) {
            futures.add(pool.submit(() -> {
                gate.await();
                try {
                    return new Outcome(task.call(), null);
                } catch (Throwable e) {
                    return new Outcome(null, e);
                }
            }));
        }
        gate.countDown();
        List<Outcome> outcomes = new ArrayList<>();
        for (Future<Outcome> future : futures) {
            outcomes.add(future.get(60, TimeUnit.SECONDS));
        }
        pool.shutdown();
        return outcomes;
    }

    public static long countSucceeded(List<Outcome> outcomes) {
        return outcomes.stream().filter(Outcome::succeeded).count();
    }

    public static long countErrorCode(List<Outcome> outcomes, String code) {
        return outcomes.stream().filter(o -> code.equals(o.errorCode())).count();
    }
}
