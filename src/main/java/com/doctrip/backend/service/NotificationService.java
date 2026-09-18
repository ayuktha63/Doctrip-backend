package com.doctrip.backend.service;

/**
 * FR-09 and the story's Notifications table — delivery abstraction for coordination events
 * (doctor accepted, alternative available, coordination failed, patient cancelled, etc). Mirrors
 * {@link OtpDeliveryService}'s shape: swap the implementation for a real email/push/in-app provider
 * later; nothing else in this service's callers needs to change.
 */
public interface NotificationService {

    /**
     * @param recipientType "PATIENT", "DOCTOR", or "ADMIN" (hospital coordination desk)
     * @param recipientId   patientId, doctorId, or hospitalId depending on recipientType
     * @param event         short event code, matching the story's Notifications table rows (e.g.
     *                      "doctor_accepted", "alternative_available", "coordination_failed")
     * @param message       human-readable summary for the notification body
     */
    void notify(String recipientType, String recipientId, String event, String message);
}
