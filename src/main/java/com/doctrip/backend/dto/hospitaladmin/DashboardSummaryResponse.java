package com.doctrip.backend.dto.hospitaladmin;

import java.time.LocalDateTime;
import java.util.List;

public record DashboardSummaryResponse(
        Kpis kpis,
        Pipeline pipeline,
        List<NeedsAttentionItem> needsAttention,
        List<SlaAlertItem> slaAlerts,
        List<UpcomingAppointmentItem> upcomingAppointments) {

    public record Kpis(
            long newRequests,
            long inCoordination,
            long awaitingDoctor,
            long awaitingPatient,
            long upcomingAppointments,
            long slaBreaches) {}

    public record Pipeline(
            long newRequests,
            long inCoordination,
            long awaitingDoctor,
            long awaitingPatient,
            long confirmed,
            long cancelled) {}

    public record NeedsAttentionItem(
            String id,
            String patientName,
            String department,
            LocalDateTime requestedAt,
            String status,
            long slaMinutesRemaining) {}

    public record SlaAlertItem(String id, String patientName, String message) {}

    public record UpcomingAppointmentItem(
            String id, int day, String month, String patientName, String doctorName, String time) {}
}
