package com.salah.booknest.lending.config;

import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.provider.jdbctemplate.JdbcTemplateLockProvider;
import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

/**
 * Several lending-service instances share one database. Before a scheduled job runs, the instance takes the
 * job's row in the shedlock table; the other instances see it is taken and skip that run.
 * The tests turn it off (app.scheduler-lock.enabled=false) because they call the jobs directly.
 */
@Configuration
@EnableSchedulerLock(defaultLockAtMostFor = "10m")
@ConditionalOnProperty(name = "app.scheduler-lock.enabled", havingValue = "true", matchIfMissing = true)
public class SchedulerLockConfig {

    @Bean
    public LockProvider lockProvider(DataSource dataSource) {
        return new JdbcTemplateLockProvider(JdbcTemplateLockProvider.Configuration.builder()
                .withJdbcTemplate(new JdbcTemplate(dataSource))
                // the database clock decides, so instances whose clocks differ still agree
                .usingDbTime()
                .build());
    }
}
