package com.doctrip.backend.service;

/** OTP delivery channel abstraction (FR-02: email and/or mobile). Swap the implementation for a real provider later. */
public interface OtpDeliveryService {

    /** channel is "EMAIL" or "MOBILE"; target is the email address or phone number. */
    void send(String channel, String target, String otp);
}
