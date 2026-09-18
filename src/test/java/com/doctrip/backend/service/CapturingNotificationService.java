package com.doctrip.backend.service;

import java.util.ArrayList;
import java.util.List;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

/** Test-only notification "delivery" that records every call so tests can assert on it, mirroring
 * {@link CapturingOtpDeliveryService}. */
@Service
@Profile("test")
public class CapturingNotificationService implements NotificationService {

    public record Notification(String recipientType, String recipientId, String event, String message) {}

    private final List<Notification> sent = new ArrayList<>();

    @Override
    public synchronized void notify(String recipientType, String recipientId, String event, String message) {
        sent.add(new Notification(recipientType, recipientId, event, message));
    }

    public synchronized List<Notification> sent() {
        return List.copyOf(sent);
    }

    public synchronized boolean hasEventFor(String recipientType, String recipientId, String event) {
        return sent.stream().anyMatch(n ->
                n.recipientType().equals(recipientType) && n.recipientId().equals(recipientId) && n.event().equals(event));
    }
}
