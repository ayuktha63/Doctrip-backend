package com.doctrip.backend.service;

import com.doctrip.backend.entity.AppointmentRequest;
import com.doctrip.backend.entity.DoctorAcknowledgementRequest;
import com.doctrip.backend.repository.DoctorAcknowledgementRequestRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

/** Test-only doctor-acknowledgement "email" that records every call so tests can assert on it,
 *  mirroring {@link CapturingNotificationService}. Still assigns a synthetic Message-ID and
 *  persists it onto the ack, same as the real SMTP implementation, so
 *  {@link com.doctrip.backend.service.DoctorReplyImapPoller}-adjacent correlation logic can be
 *  tested without a real mailbox. */
@Service
@Profile("test")
public class CapturingDoctorAcknowledgementMailService implements DoctorAcknowledgementMailService {

    public record SentMail(UUID ackId, UUID appointmentRequestId, String doctorEmail, String messageId) {}

    private final DoctorAcknowledgementRequestRepository doctorAcknowledgementRequestRepository;
    private final List<SentMail> sent = new ArrayList<>();

    public CapturingDoctorAcknowledgementMailService(
            DoctorAcknowledgementRequestRepository doctorAcknowledgementRequestRepository) {
        this.doctorAcknowledgementRequestRepository = doctorAcknowledgementRequestRepository;
    }

    @Override
    public synchronized void send(DoctorAcknowledgementRequest ack, AppointmentRequest request, String doctorEmail) {
        if (doctorEmail == null || doctorEmail.isBlank()) {
            return;
        }
        String messageId = "<test-" + ack.getId() + "@doctrip.test>";
        ack.setEmailMessageId(messageId);
        doctorAcknowledgementRequestRepository.save(ack);
        sent.add(new SentMail(ack.getId(), request.getId(), doctorEmail, messageId));
    }

    public synchronized List<SentMail> sent() {
        return List.copyOf(sent);
    }
}
