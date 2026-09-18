package com.doctrip.backend.exception;

/** The authenticated principal is not allowed to act on this resource (e.g. a different patient's
 * appointment request) — distinct from {@link UnauthorizedException}, which means "not authenticated
 * at all." */
public class ForbiddenException extends RuntimeException {
    public ForbiddenException(String message) {
        super(message);
    }
}
