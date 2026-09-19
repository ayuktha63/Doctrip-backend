package com.doctrip.backend.service;

import com.doctrip.backend.entity.AppointmentRequest;
import com.doctrip.backend.entity.DoctorAcknowledgementRequest;

/**
 * Story 7 — the doctor's entire acknowledgement channel is email: one-click Accept/Reject links,
 * and reply-to-email for Suggest Alternative Slot (see {@link DoctorReplyImapPoller}). Deliberately
 * separate from {@link NotificationService} — that interface's 4-string signature has no room for
 * a reply-to-able message, action links, or the {@code Message-ID} this needs to persist for
 * correlating the doctor's reply back to this acknowledgement request.
 */
public interface DoctorAcknowledgementMailService {

    /**
     * Sends the acknowledgement email to {@code doctorEmail} and persists the outbound message's
     * {@code Message-ID} onto {@code ack} (via {@code DoctorAcknowledgementRequestRepository}) so
     * {@link DoctorReplyImapPoller} can match a reply back to it later. No-ops (logs a warning)
     * if {@code doctorEmail} is blank — coordination must not fail just because this placeholder
     * doctor-data row has no email on file yet.
     */
    void send(DoctorAcknowledgementRequest ack, AppointmentRequest request, String doctorEmail);
}
