package com.salah.booknest.lending.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Configuration
public class AsyncConfig {

    // expires several holds at the same time
    @Bean(destroyMethod = "shutdown")
    public ThreadPoolExecutor holdExpiryExecutor() {
        AtomicInteger counter = new AtomicInteger(1);
        return new ThreadPoolExecutor(4, 4, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>(),
                task -> new Thread(task, "hold-expiry-" + counter.getAndIncrement()));
    }
}
