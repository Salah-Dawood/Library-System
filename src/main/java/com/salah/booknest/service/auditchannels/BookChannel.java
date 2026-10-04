package com.salah.booknest.service.auditchannels;

import com.salah.booknest.model.request.AuditTriggerInfo;

public class BookChannel implements AuditChannel{
    @Override
    public boolean supports(String type) {
        return false;
    }

    @Override
    public void processAndSave(AuditTriggerInfo info) {

    }

    @Override
    public String getReadableLog(AuditTriggerInfo info) {
        return "";
    }
}
