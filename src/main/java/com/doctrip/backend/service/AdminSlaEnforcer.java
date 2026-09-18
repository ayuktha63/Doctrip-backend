package com.doctrip.backend.service;

import com.doctrip.backend.entity.AppointmentRequest;
import com.doctrip.backend.entity.AppointmentRequestStatus;
import com.doctrip.backend.repository.AppointmentRequestRepository;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * FR-03/BR-02 — Hospital Admin has a 1-hour SLA (see {@code AppointmentRequest.adminSlaDeadline},
 * set at FR-01 intake) to initiate Doctor coordination. This poller finds every request still
 * {@code REQUESTED} past that deadline and has the System take over coordination on its behalf
 * (BR-04 — same rules as {@link AppointmentRequestService#startCoordination}), so the Patient never
 * waits on additional Admin action once the SLA lapses (FR-03's explicit requirement).
 *
 * <p>{@code @Profile("!test")} — mirrors {@code ConsoleOtpDeliveryService}'s pattern: a real timer
 * firing during test runs would race fixed test data/clocks, so it's disabled under the test
 * profile entirely rather than trying to make it deterministic there.
 *
 * <p>Polling, not a per-request scheduled job, because nothing in this backend currently runs a
 * proper delayed-job queue (see OTP/rate-limiter code, which is all Redis-TTL-based, not a job
 * scheduler) — a 30s poll is simple, correct, and cheap at this data volume. Revisit if request
 * volume ever makes a full table scan every 30s a real cost.
 */
@Component
@Profile("!test")
public class AdminSlaEnforcer {

    private static final Logger log = LoggerFactory.getLogger(AdminSlaEnforcer.class);

    private final AppointmentRequestRepository appointmentRequestRepository;
    private final AppointmentRequestService appointmentRequestService;

    public AdminSlaEnforcer(
            AppointmentRequestRepository appointmentRequestRepository, AppointmentRequestService appointmentRequestService) {
        this.appointmentRequestRepository = appointmentRequestRepository;
        this.appointmentRequestService = appointmentRequestService;
    }

    @Scheduled(fixedDelayString = "PT30S", initialDelayString = "PT10S")
    public void takeOverBreachedRequests() {
        List<AppointmentRequest> breached = appointmentRequestRepository.findByStatusAndAdminSlaDeadlineBefore(
                AppointmentRequestStatus.REQUESTED, Instant.now());

        for (AppointmentRequest request : breached) {
            try {
                appointmentRequestService.systemTakeOverIfStillRequested(request.getId());
            } catch (Exception e) {
                // One request's failure (e.g. a transient DB hiccup) must never stop the rest of this
                // tick's batch from being processed — the next tick will simply retry it.
                log.error("System SLA takeover failed for appointment request {}", request.getId(), e);
            }
        }
    }
}
