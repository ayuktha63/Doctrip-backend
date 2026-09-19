package com.doctrip.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalTime;
import java.util.regex.Matcher;
import org.junit.jupiter.api.Test;

/**
 * Story 7 — the doctor's "Alternative Time" reply is free-typed text, and the instructed format
 * ("HH:MM AM/PM") is easy to get wrong. Covers the bug this replaced: a bare 24-hour-looking
 * "02:00" with no AM/PM used to always parse as 2 AM regardless of what the doctor meant, and a
 * "02:00 PM" reply had its "PM" silently dropped by the old regex, landing on 2 AM as well.
 */
class DoctorReplyImapPollerTimeParsingTests {

    private LocalTime parse(String text) {
        Matcher m = DoctorReplyImapPoller.TIME_PATTERN.matcher(text);
        assertThat(m.find()).as("pattern should match: " + text).isTrue();
        return DoctorReplyImapPoller.parseTime(m);
    }

    @Test
    void explicitPmIsConvertedTo24Hour() {
        assertThat(parse("Alternative Time: 02:00 PM")).isEqualTo(LocalTime.of(14, 0));
    }

    @Test
    void explicitAmStaysAsIs() {
        assertThat(parse("Alternative Time: 02:00 AM")).isEqualTo(LocalTime.of(2, 0));
    }

    @Test
    void twelvePmIsNoon() {
        assertThat(parse("Alternative Time: 12:00 PM")).isEqualTo(LocalTime.of(12, 0));
    }

    @Test
    void twelveAmIsMidnight() {
        assertThat(parse("Alternative Time: 12:00 AM")).isEqualTo(LocalTime.of(0, 0));
    }

    @Test
    void unambiguous24HourInputWithNoSuffixIsAccepted() {
        assertThat(parse("Alternative Time: 14:00")).isEqualTo(LocalTime.of(14, 0));
    }

    @Test
    void ambiguousHourWithNoSuffixIsRejectedRatherThanGuessed() {
        Matcher m = DoctorReplyImapPoller.TIME_PATTERN.matcher("Alternative Time: 02:00");
        assertThat(m.find()).isTrue();
        assertThatThrownBy(() -> DoctorReplyImapPoller.parseTime(m)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void caseInsensitiveSuffixIsAccepted() {
        assertThat(parse("Alternative Time: 09:30 pm")).isEqualTo(LocalTime.of(21, 30));
    }
}
