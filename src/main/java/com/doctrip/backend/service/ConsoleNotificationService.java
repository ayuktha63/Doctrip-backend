package com.doctrip.backend.service;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

/**
 * Dev-only stand-in for a real email/push/in-app delivery provider — same rationale and pattern as
 * {@link ConsoleOtpDeliveryService}: prints to stdout directly (not through the logger) so this
 * stays clearly a temporary local-dev affordance, not an audit record (that's {@link
 * AuditService}'s job — this and the audit trail are deliberately separate: the audit trail is the
 * permanent "what happened" record; this is "who would have been notified, and with what." Not
 * active under the "test" profile — see CapturingNotificationService in the test sources.
 */
@Service
@Profile("!test")
public class ConsoleNotificationService implements NotificationService {

    @Override
    public void notify(String recipientType, String recipientId, String event, String message) {
        System.out.println("[NOTIFY] to=" + recipientType + ":" + recipientId + " event=" + event + " — " + message);
    }
}
