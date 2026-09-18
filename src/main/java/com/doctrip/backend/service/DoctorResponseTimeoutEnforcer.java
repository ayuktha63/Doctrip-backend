package com.doctrip.backend.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * EC-12 — a doctor who never responds within their acknowledgement window shouldn't leave a
 * request stuck in {@code DOCTOR_COORDINATION} forever. This poller finds every acknowledgement
 * request still {@code PENDING} past its {@code responseDeadline} and has {@link
 * AppointmentRequestService#expireOverdueAcknowledgements()} close it out, escalating the parent
 * request if that was the last thing still open (FR-10).
 *
 * <p>Same posture as {@link AdminSlaEnforcer}: {@code @Profile("!test")} so a real timer never
 * races fixed test data/clocks, and polling (not a per-ack scheduled job) because this backend has
 * no delayed-job queue to hang a precise per-deadline callback off of.
 */
@Component
@Profile("!test")
public class DoctorResponseTimeoutEnforcer {

    private static final Logger log = LoggerFactory.getLogger(DoctorResponseTimeoutEnforcer.class);

    private final AppointmentRequestService appointmentRequestService;

    public DoctorResponseTimeoutEnforcer(AppointmentRequestService appointmentRequestService) {
        this.appointmentRequestService = appointmentRequestService;
    }

    @Scheduled(fixedDelayString = "PT30S", initialDelayString = "PT20S")
    public void expireOverdueAcknowledgements() {
        try {
            appointmentRequestService.expireOverdueAcknowledgements();
        } catch (Exception e) {
            // Never let one bad tick kill future scheduling — the next tick will simply retry.
            log.error("Doctor response-window timeout sweep failed", e);
        }
    }
}
