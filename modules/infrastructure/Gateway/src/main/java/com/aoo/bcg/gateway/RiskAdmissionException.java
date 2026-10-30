package com.aoo.bcg.gateway;

/** Deliberately contains no rule, score, ban, device, or location detail. */
public final class RiskAdmissionException extends SecurityException {
    public RiskAdmissionException() { super(GatewayErrorCode.RISK_ADMISSION_REJECTED.defaultMessage()); }
    public GatewayErrorCode code() { return GatewayErrorCode.RISK_ADMISSION_REJECTED; }
}
