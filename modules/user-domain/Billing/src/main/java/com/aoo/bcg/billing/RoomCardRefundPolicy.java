package com.aoo.bcg.billing;

/**
 * The XQP_R5 refund eligibility policy; never calculates or transfers money.
 * Facts must be established by the server's exact business receipt and lifecycle adapter.
 * In particular, the caller supplies the decision-time round index; this class never
 * converts completed rounds, resets a loop index, or infers a callback's phase.
 */
public final class RoomCardRefundPolicy {
    public static final String POLICY_REF = "XQP_R5";

    private RoomCardRefundPolicy() { }

    public enum Termination { BUSINESS_FAILED, FINAL_SETTLEMENT, NORMAL_EXIT }
    public enum CarryState { NOT_APPLICABLE, RESERVED, APPLIED, USED }
    public enum Action { REFUND_ORIGINAL_REMAINDER, KEEP_PENDING, NO_POLICY_REFUND }
    public enum Reason {
        VERIFIED_BUSINESS_FAILURE, EARLY_FINAL_SETTLEMENT, THRESHOLD_REACHED,
        ORDINARY_EXIT, UNUSED_CARRY, USED_CARRY
    }

    /**
     * BUSINESS_FAILED means this particular operation was proven unsuccessful,
     * never merely timed out or has an unknown receipt. APPLIED is per operation
     * and means its carry amount took effect but has not been used in its game;
     * USED means this operation's carry was actually used, not an earlier carry.
     * The round index is mandatory for FIXED/LOOP and absent for CARRY.
     */
    public record Facts(RoomChargeOperation.Kind kind, Termination termination,
            Integer roundIndexAtRefundDecision, CarryState carryState) {
        public Facts {
            if (kind == null || termination == null || carryState == null) {
                throw new IllegalArgumentException("refund facts must be explicit");
            }
            if (kind == RoomChargeOperation.Kind.CARRY) {
                if (roundIndexAtRefundDecision != null || carryState == CarryState.NOT_APPLICABLE) {
                    throw new IllegalArgumentException("invalid carry refund facts");
                }
                if (carryState == CarryState.USED && termination == Termination.BUSINESS_FAILED) {
                    throw new IllegalArgumentException("used carry contradicts unsuccessful operation");
                }
            } else if (roundIndexAtRefundDecision == null || roundIndexAtRefundDecision <= 0
                    || carryState != CarryState.NOT_APPLICABLE) {
                throw new IllegalArgumentException("invalid fixed or loop refund facts");
            }
        }
    }

    /** Eligibility only. A refund still requires an original charge and its unrefunded cap. */
    public record Decision(Action action, Reason reason) { }

    public static Decision evaluate(String policyRef, Facts facts) {
        if (!POLICY_REF.equals(policyRef) || facts == null) {
            throw new IllegalArgumentException("unknown refund policy or absent facts");
        }
        if (facts.termination() == Termination.BUSINESS_FAILED) {
            return new Decision(Action.REFUND_ORIGINAL_REMAINDER, Reason.VERIFIED_BUSINESS_FAILURE);
        }
        if (facts.kind() == RoomChargeOperation.Kind.CARRY) {
            return facts.carryState() == CarryState.USED
                    ? new Decision(Action.NO_POLICY_REFUND, Reason.USED_CARRY)
                    : new Decision(Action.REFUND_ORIGINAL_REMAINDER, Reason.UNUSED_CARRY);
        }
        if (facts.termination() == Termination.NORMAL_EXIT) {
            return new Decision(Action.KEEP_PENDING, Reason.ORDINARY_EXIT);
        }
        return facts.roundIndexAtRefundDecision() < 3
                ? new Decision(Action.REFUND_ORIGINAL_REMAINDER, Reason.EARLY_FINAL_SETTLEMENT)
                : new Decision(Action.NO_POLICY_REFUND, Reason.THRESHOLD_REACHED);
    }
}
