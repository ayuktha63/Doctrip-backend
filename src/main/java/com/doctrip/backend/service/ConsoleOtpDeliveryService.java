package com.doctrip.backend.service;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

/**
 * Dev-only stand-in for a real email/SMS provider (SES/Twilio etc., to be wired later per the
 * user's plan). Prints to stdout directly rather than through the logger, so the OTP value never
 * lands in the rolling application log file — only real console output during local development.
 * Not active under the "test" profile — see CapturingOtpDeliveryService in the test sources.
 */
@Service
@Profile("!test")
public class ConsoleOtpDeliveryService implements OtpDeliveryService {

    @Override
    public void send(String channel, String target, String otp) {
        System.out.println("[DEV OTP] channel=" + channel + " target=" + target + " otp=" + otp);
    }
}
