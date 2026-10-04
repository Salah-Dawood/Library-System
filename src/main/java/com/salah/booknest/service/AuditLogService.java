package com.salah.booknest.service;

import com.salah.booknest.model.request.AuditTriggerInfo;
import com.salah.booknest.repository.BookRepository;
import com.salah.booknest.repository.UserRepository;
import com.salah.booknest.service.auditchannels.AuditChannel;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.logging.Logger;

@Service
public class AuditLogService {
    private final List<AuditChannel> channels;
    private final UserRepository userRepository;
    private final BookRepository bookRepository;
    private final Logger log;

    public AuditLogService(List<AuditChannel> channels, UserRepository userRepository, BookRepository bookRepository, Logger log) {
        this.channels = channels;
        this.userRepository = userRepository;
        this.bookRepository = bookRepository;
        this.log = log;
    }

    public void routeLog(AuditTriggerInfo info) {
        AuditChannel matchingChannel = channels.stream()
                .filter(channel -> channel.supports(info.type()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No audit channel found for type " + info.type()));

        matchingChannel.processAndSave(info);
        String readableLog = matchingChannel.getReadableLog(info);
        log.info(readableLog);
    }
}