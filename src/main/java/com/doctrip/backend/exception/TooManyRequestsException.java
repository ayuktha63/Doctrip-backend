package com.doctrip.backend.exception;

/** OTP attempt limit or resend/rate-limit rules exceeded (BR-06, AC-09). */
public class TooManyRequestsException extends RuntimeException {
    public TooManyRequestsException(String message) {
        super(message);
    }
}
