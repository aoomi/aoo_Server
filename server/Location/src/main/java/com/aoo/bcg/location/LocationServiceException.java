package com.aoo.bcg.location;

public final class LocationServiceException extends RuntimeException {
    private final LocationErrorCode code;

    public LocationServiceException(LocationErrorCode code, String message) {
        super(message);
        this.code = code;
    }

    public LocationErrorCode code() { return code; }
}
