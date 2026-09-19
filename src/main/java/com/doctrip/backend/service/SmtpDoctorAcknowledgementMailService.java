package com.doctrip.backend.service;

import com.doctrip.backend.entity.AppointmentRequest;
import com.doctrip.backend.entity.DoctorAcknowledgementRequest;
import com.doctrip.backend.entity.Hospital;
import com.doctrip.backend.repository.DoctorAcknowledgementRequestRepository;
import com.doctrip.backend.repository.HospitalRepository;
import jakarta.mail.internet.MimeMessage;
import java.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
@Profile("!test")
public class SmtpDoctorAcknowledgementMailService implements DoctorAcknowledgementMailService {

    private static final Logger log = LoggerFactory.getLogger(SmtpDoctorAcknowledgementMailService.class);
    private static final DateTimeFormatter DEADLINE_FORMAT =
            DateTimeFormatter.ofPattern("d MMM, h:mm a").withZone(java.time.ZoneId.systemDefault());

    private final JavaMailSender mailSender;
    private final HospitalRepository hospitalRepository;
    private final DoctorAcknowledgementRequestRepository doctorAcknowledgementRequestRepository;

    @Value("${doctrip.mail.from}")
    private String fromAddress;

    @Value("${doctrip.mail.api-base-url}")
    private String apiBaseUrl;

    public SmtpDoctorAcknowledgementMailService(
            JavaMailSender mailSender,
            HospitalRepository hospitalRepository,
            DoctorAcknowledgementRequestRepository doctorAcknowledgementRequestRepository) {
        this.mailSender = mailSender;
        this.hospitalRepository = hospitalRepository;
        this.doctorAcknowledgementRequestRepository = doctorAcknowledgementRequestRepository;
    }

    @Override
    public void send(DoctorAcknowledgementRequest ack, AppointmentRequest request, String doctorEmail) {
        if (doctorEmail == null || doctorEmail.isBlank()) {
            log.warn(
                    "Skipping doctor acknowledgement email for ack {} — doctor {} has no email on file.",
                    ack.getId(), ack.getDoctorName());
            return;
        }

        String hospitalName = hospitalRepository
                .findById(request.getHospitalId())
                .map(Hospital::getName)
                .orElse("your hospital");

        String acceptUrl = respondLink(ack.getId(), "ACCEPT");
        String rejectUrl = respondLink(ack.getId(), "REJECT");
        String deadline = DEADLINE_FORMAT.format(ack.getResponseDeadline());

        String subject = "Appointment acknowledgement needed — " + request.getDepartment() + ", "
                + request.getRequestedDate();

        String html = """
                <p>Dear %s,</p>
                <p>%s has an appointment request that needs your acknowledgement.</p>
                <table cellpadding="4">
                  <tr><td><b>Patient</b></td><td>%s</td></tr>
                  <tr><td><b>Department</b></td><td>%s</td></tr>
                  <tr><td><b>Reason</b></td><td>%s</td></tr>
                  <tr><td><b>Requested date</b></td><td>%s</td></tr>
                  <tr><td><b>Requested time</b></td><td>%s</td></tr>
                  <tr><td><b>Please respond by</b></td><td>%s</td></tr>
                </table>
                <p>
                  <a href="%s" style="padding:10px 18px;background:#1a7f37;color:#fff;text-decoration:none;border-radius:6px;margin-right:8px">Accept</a>
                  <a href="%s" style="padding:10px 18px;background:#b42318;color:#fff;text-decoration:none;border-radius:6px">Reject / Unavailable</a>
                </p>
                <p>Can't make this exact time, but could do another? Reply to this email with exactly these two lines:</p>
                <pre>Alternative Date: YYYY-MM-DD
                Alternative Time: HH:MM AM/PM (e.g. 02:00 PM)</pre>
                <p>This request will expire if not answered by the deadline above.</p>
                """
                .formatted(
                        ack.getDoctorName(),
                        hospitalName,
                        request.getPatientName(),
                        request.getDepartment(),
                        request.getHealthcareRequirement(),
                        request.getRequestedDate(),
                        request.getRequestedTime(),
                        deadline,
                        acceptUrl,
                        rejectUrl);

        try {
            MimeMessage message = mailSender.createMimeMessage();
            // true = multipart mode, required for the two-arg setText(plain, html) below.
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromAddress);
            helper.setTo(doctorEmail);
            helper.setSubject(subject);
            helper.setText(stripHtml(html), html);
            mailSender.send(message);

            // JavaMail auto-generates the Message-ID from the sending host's local IP as an RFC
            // 5322 domain-literal ("@[10.0.0.5]"), but Gmail strips the brackets when it builds
            // the reply's In-Reply-To header ("@10.0.0.5") — DoctorReplyImapPoller's exact-string
            // match against this stored value would then silently never hit. Normalizing away the
            // brackets here (and doing the same on the read side) makes the match
            // bracket-format-independent regardless of what generated the id.
            String messageId = normalizeMessageId(message.getMessageID());
            ack.setEmailMessageId(messageId);
            doctorAcknowledgementRequestRepository.save(ack);
        } catch (Exception e) {
            // A failed send must not roll back coordination itself — the acknowledgement request
            // row and its response-window deadline still stand; this is logged so it can be
            // investigated/resent, same posture as any other best-effort notification in this
            // codebase (ConsoleNotificationService's callers don't roll back on notify() either).
            log.error("Failed to send doctor acknowledgement email for ack {}", ack.getId(), e);
        }
    }

    static String normalizeMessageId(String messageId) {
        return messageId == null ? null : messageId.replace("[", "").replace("]", "");
    }

    private String respondLink(java.util.UUID ackId, String action) {
        return apiBaseUrl + "/api/doctor/acknowledgements/" + ackId + "/respond-link?action=" + action;
    }

    private String stripHtml(String html) {
        return html.replaceAll("<[^>]+>", "").trim();
    }
}
