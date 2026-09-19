package com.doctrip.backend.service;

import com.doctrip.backend.dto.DoctorResponseRequest;
import com.doctrip.backend.entity.DoctorAcknowledgementRequest;
import com.doctrip.backend.entity.DoctorAcknowledgementStatus;
import com.doctrip.backend.repository.DoctorAcknowledgementRequestRepository;
import jakarta.mail.BodyPart;
import jakarta.mail.Flags;
import jakarta.mail.Folder;
import jakarta.mail.Message;
import jakarta.mail.Multipart;
import jakarta.mail.Session;
import jakarta.mail.Store;
import jakarta.mail.search.FlagTerm;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Story 7 — the other half of the doctor's email-only acknowledgement channel: Suggest Alternative
 * Slot, which the team confirmed is a typed reply to the acknowledgement email (not a web page).
 * Correlates each unread reply back to its {@link DoctorAcknowledgementRequest} via the standard
 * email-threading {@code In-Reply-To}/{@code References} header against the {@code
 * emailMessageId} stored when the original email was sent ({@link
 * SmtpDoctorAcknowledgementMailService}) — not by parsing subject-line text, which many mail
 * clients mangle on reply. Once resolved, hands off to the exact same {@code
 * respondToAcknowledgement} the one-click Accept/Reject links use, so it goes through every
 * existing FR-04/FR-12/FR-14 atomicity/staleness guarantee unchanged.
 *
 * <p>Same posture as {@link AdminSlaEnforcer}/{@link DoctorResponseTimeoutEnforcer}:
 * {@code @Profile("!test")} so a real poll against a real mailbox never runs under tests.
 */
@Component
@Profile("!test")
public class DoctorReplyImapPoller {

    private static final Logger log = LoggerFactory.getLogger(DoctorReplyImapPoller.class);

    // The outbound email instructs the doctor to reply with exactly these two lines — matched
    // case-insensitively, tolerant of the doctor's own reply text sitting above quoted history.
    private static final Pattern DATE_PATTERN =
            Pattern.compile("Alternative Date:\\s*(\\d{4}-\\d{2}-\\d{2})", Pattern.CASE_INSENSITIVE);
    private static final Pattern TIME_PATTERN =
            Pattern.compile("Alternative Time:\\s*(\\d{1,2}:\\d{2})", Pattern.CASE_INSENSITIVE);

    private final DoctorAcknowledgementRequestRepository doctorAcknowledgementRequestRepository;
    private final AppointmentRequestService appointmentRequestService;
    private final NotificationService notificationService;

    @Value("${doctrip.mail.imap-host}")
    private String imapHost;

    @Value("${doctrip.mail.imap-port}")
    private int imapPort;

    @Value("${spring.mail.username}")
    private String username;

    @Value("${spring.mail.password}")
    private String password;

    public DoctorReplyImapPoller(
            DoctorAcknowledgementRequestRepository doctorAcknowledgementRequestRepository,
            AppointmentRequestService appointmentRequestService,
            NotificationService notificationService) {
        this.doctorAcknowledgementRequestRepository = doctorAcknowledgementRequestRepository;
        this.appointmentRequestService = appointmentRequestService;
        this.notificationService = notificationService;
    }

    @Scheduled(fixedDelayString = "PT30S", initialDelayString = "PT25S")
    public void poll() {
        if (username == null || username.isBlank()) {
            return; // no mailbox configured (e.g. local dev without MAIL_USERNAME set) — skip quietly
        }
        try {
            checkInbox();
        } catch (Exception e) {
            // Never let one bad tick kill future scheduling — same posture as the other enforcers.
            log.error("Doctor reply IMAP poll failed", e);
        }
    }

    private void checkInbox() throws Exception {
        Properties props = new Properties();
        props.put("mail.store.protocol", "imaps");
        Session session = Session.getInstance(props);
        try (Store store = session.getStore("imaps")) {
            store.connect(imapHost, imapPort, username, password);
            Folder inbox = store.getFolder("INBOX");
            try {
                inbox.open(Folder.READ_WRITE);
                Message[] unseen = inbox.search(new FlagTerm(new Flags(Flags.Flag.SEEN), false));
                for (Message message : unseen) {
                    try {
                        handle(message);
                    } catch (Exception e) {
                        log.error("Failed to process an inbound doctor reply", e);
                    } finally {
                        // Mark seen regardless of outcome — a message we can't use is flagged once
                        // (via the admin notification below), not re-processed every 30s forever.
                        message.setFlag(Flags.Flag.SEEN, true);
                    }
                }
            } finally {
                if (inbox.isOpen()) inbox.close(false);
            }
        }
    }

    private void handle(Message message) throws Exception {
        String inReplyTo = firstHeader(message, "In-Reply-To");
        if (inReplyTo == null) {
            inReplyTo = lastReference(message);
        }
        if (inReplyTo == null) {
            return; // not a reply to anything — ignore (e.g. an unrelated inbox message)
        }

        Optional<DoctorAcknowledgementRequest> ackOpt =
                doctorAcknowledgementRequestRepository.findByEmailMessageId(inReplyTo.trim());
        if (ackOpt.isEmpty()) {
            return; // not a reply to one of our acknowledgement emails
        }

        DoctorAcknowledgementRequest ack = ackOpt.get();
        if (ack.getStatus() != DoctorAcknowledgementStatus.PENDING) {
            return; // already resolved (accepted/rejected/expired/closed) — nothing to do
        }

        String body = extractText(message);
        Matcher dateMatcher = DATE_PATTERN.matcher(body);
        Matcher timeMatcher = TIME_PATTERN.matcher(body);

        if (!dateMatcher.find() || !timeMatcher.find()) {
            flagUnparseable(ack, "didn't include a recognizable Alternative Date/Time.");
            return;
        }

        try {
            LocalDate date = LocalDate.parse(dateMatcher.group(1));
            LocalTime time = LocalTime.parse(timeMatcher.group(1));
            appointmentRequestService.respondToAcknowledgement(
                    ack.getId(), new DoctorResponseRequest("PROPOSE_ALTERNATIVE", date, time));
        } catch (Exception e) {
            flagUnparseable(ack, "sent a date/time that couldn't be parsed (" + e.getMessage() + ").");
        }
    }

    private void flagUnparseable(DoctorAcknowledgementRequest ack, String reason) {
        notificationService.notify(
                "ADMIN",
                ack.getDoctorId().toString(),
                "doctor_reply_unparseable",
                "Dr. " + ack.getDoctorName() + " replied to an acknowledgement request but " + reason
                        + " Needs manual follow-up (acknowledgement " + ack.getId() + ").");
    }

    private String firstHeader(Message message, String name) throws Exception {
        String[] values = message.getHeader(name);
        return (values == null || values.length == 0) ? null : values[0];
    }

    private String lastReference(Message message) throws Exception {
        String references = firstHeader(message, "References");
        if (references == null || references.isBlank()) {
            return null;
        }
        String[] ids = references.trim().split("\\s+");
        return ids[ids.length - 1];
    }

    private String extractText(Message message) throws Exception {
        Object content = message.getContent();
        if (content instanceof String text) {
            return text;
        }
        if (content instanceof Multipart multipart) {
            StringBuilder sb = new StringBuilder();
            appendTextParts(multipart, sb);
            return sb.toString();
        }
        return "";
    }

    private void appendTextParts(Multipart multipart, StringBuilder sb) throws Exception {
        for (int i = 0; i < multipart.getCount(); i++) {
            BodyPart part = multipart.getBodyPart(i);
            Object partContent = part.getContent();
            if (partContent instanceof String text && part.isMimeType("text/plain")) {
                sb.append(text).append('\n');
            } else if (partContent instanceof Multipart nested) {
                appendTextParts(nested, sb);
            }
        }
    }
}
