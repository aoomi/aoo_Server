package com.aoo.bcg.billing;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static com.aoo.bcg.billing.RoomCardRefundPolicy.*;
import static com.aoo.bcg.billing.RoomChargeOperation.Kind.*;

class RoomCardRefundPolicyTest {
    private Decision decide(RoomChargeOperation.Kind kind, Termination termination, Integer index,
            CarryState carry) {
        return evaluate(POLICY_REF, new Facts(kind, termination, index, carry));
    }

    @Test void fixedAndLoopUseDecisionTimeIndexThreshold() {
        for (var kind : List.of(FIXED, LOOP)) {
            for (int index : List.of(1, 2)) {
                assertEquals(new Decision(Action.REFUND_ORIGINAL_REMAINDER, Reason.EARLY_FINAL_SETTLEMENT),
                        decide(kind, Termination.FINAL_SETTLEMENT, index, CarryState.NOT_APPLICABLE));
            }
            for (int index : List.of(3, Integer.MAX_VALUE)) {
                assertEquals(new Decision(Action.NO_POLICY_REFUND, Reason.THRESHOLD_REACHED),
                        decide(kind, Termination.FINAL_SETTLEMENT, index, CarryState.NOT_APPLICABLE));
            }
        }
    }

    @Test void sameCallbackBigSettlementAndLaterDissolveHaveDistinctExplicitFacts() {
        // The producer, not this evaluator, supplies 2 before reset and 3 after reset.
        assertEquals(Action.REFUND_ORIGINAL_REMAINDER,
                decide(FIXED, Termination.FINAL_SETTLEMENT, 2, CarryState.NOT_APPLICABLE).action());
        assertEquals(Action.NO_POLICY_REFUND,
                decide(FIXED, Termination.FINAL_SETTLEMENT, 3, CarryState.NOT_APPLICABLE).action());
    }

    @Test void ordinaryExitKeepsOriginalPendingForFinalDecision() {
        for (var kind : List.of(FIXED, LOOP)) {
            for (int index : List.of(1, 2, 3, 21)) {
                assertEquals(new Decision(Action.KEEP_PENDING, Reason.ORDINARY_EXIT),
                        decide(kind, Termination.NORMAL_EXIT, index, CarryState.NOT_APPLICABLE));
            }
            // The departed AA payer's original is still evaluated at final settlement.
            assertEquals(Action.REFUND_ORIGINAL_REMAINDER,
                    decide(kind, Termination.FINAL_SETTLEMENT, 2, CarryState.NOT_APPLICABLE).action());
        }
    }

    @Test void laterLoopUnitDoesNotResetGlobalRefundThreshold() {
        for (int globalIndex : List.of(11, 21)) {
            assertEquals(Action.NO_POLICY_REFUND,
                    decide(LOOP, Termination.FINAL_SETTLEMENT, globalIndex, CarryState.NOT_APPLICABLE).action());
            assertEquals(new Decision(Action.REFUND_ORIGINAL_REMAINDER, Reason.VERIFIED_BUSINESS_FAILURE),
                    decide(LOOP, Termination.BUSINESS_FAILED, globalIndex, CarryState.NOT_APPLICABLE));
        }
    }

    @Test void verifiedCreationAdmissionAndContinuationFailureReleaseOriginalRemainder() {
        for (var kind : List.of(FIXED, LOOP)) {
            for (int index : List.of(1, 2, 3, Integer.MAX_VALUE)) {
                assertEquals(new Decision(Action.REFUND_ORIGINAL_REMAINDER, Reason.VERIFIED_BUSINESS_FAILURE),
                        decide(kind, Termination.BUSINESS_FAILED, index, CarryState.NOT_APPLICABLE));
            }
        }
    }

    @Test void unusedInitialAndAdditionalCarryReleaseOnExitAndFinalSettlement() {
        for (var state : List.of(CarryState.RESERVED, CarryState.APPLIED)) {
            for (var termination : List.of(Termination.NORMAL_EXIT, Termination.FINAL_SETTLEMENT)) {
                assertEquals(new Decision(Action.REFUND_ORIGINAL_REMAINDER, Reason.UNUSED_CARRY),
                        decide(CARRY, termination, null, state));
            }
        }
    }

    @Test void usedCarryDoesNotRefundOnExitOrFinalSettlement() {
        for (var termination : List.of(Termination.NORMAL_EXIT, Termination.FINAL_SETTLEMENT)) {
            assertEquals(new Decision(Action.NO_POLICY_REFUND, Reason.USED_CARRY),
                    decide(CARRY, termination, null, CarryState.USED));
        }
    }

    @Test void oldUsedCarryCannotClearNewUnusedCarryEligibility() {
        assertEquals(Action.NO_POLICY_REFUND,
                decide(CARRY, Termination.NORMAL_EXIT, null, CarryState.USED).action());
        assertEquals(Action.REFUND_ORIGINAL_REMAINDER,
                decide(CARRY, Termination.NORMAL_EXIT, null, CarryState.RESERVED).action());
        assertEquals(Action.REFUND_ORIGINAL_REMAINDER,
                decide(CARRY, Termination.FINAL_SETTLEMENT, null, CarryState.APPLIED).action());
    }

    @Test void unusedCarryFailureCompensatesButUsedFailureIsContradictory() {
        for (var state : List.of(CarryState.RESERVED, CarryState.APPLIED)) {
            assertEquals(Reason.VERIFIED_BUSINESS_FAILURE,
                    decide(CARRY, Termination.BUSINESS_FAILED, null, state).reason());
        }
        assertThrows(IllegalArgumentException.class,
                () -> new Facts(CARRY, Termination.BUSINESS_FAILED, null, CarryState.USED));
    }

    @Test void absentAndUnknownPolicyOrFactsFailClosed() {
        var valid = new Facts(FIXED, Termination.FINAL_SETTLEMENT, 1, CarryState.NOT_APPLICABLE);
        assertThrows(IllegalArgumentException.class, () -> evaluate(null, valid));
        assertThrows(IllegalArgumentException.class, () -> evaluate("XQP_R4", valid));
        assertThrows(IllegalArgumentException.class, () -> evaluate(POLICY_REF, null));
        assertThrows(IllegalArgumentException.class, () -> new Facts(null, Termination.NORMAL_EXIT, 1, CarryState.NOT_APPLICABLE));
        assertThrows(IllegalArgumentException.class, () -> new Facts(FIXED, null, 1, CarryState.NOT_APPLICABLE));
        assertThrows(IllegalArgumentException.class, () -> new Facts(FIXED, Termination.NORMAL_EXIT, 1, null));
    }

    @Test void fixedAndLoopRejectAbsentNonpositiveIndexAndCarryStates() {
        for (var kind : List.of(FIXED, LOOP)) {
            assertThrows(IllegalArgumentException.class, () -> new Facts(kind, Termination.FINAL_SETTLEMENT, null, CarryState.NOT_APPLICABLE));
            for (int invalid : List.of(0, -1, Integer.MIN_VALUE)) {
                assertThrows(IllegalArgumentException.class, () -> new Facts(kind, Termination.FINAL_SETTLEMENT, invalid, CarryState.NOT_APPLICABLE));
            }
            for (var carry : List.of(CarryState.RESERVED, CarryState.APPLIED, CarryState.USED)) {
                assertThrows(IllegalArgumentException.class, () -> new Facts(kind, Termination.NORMAL_EXIT, 1, carry));
            }
        }
    }

    @Test void carryRejectsRoundInferenceAndMissingPerOperationState() {
        for (int invalid : List.of(0, 1, 3)) {
            assertThrows(IllegalArgumentException.class, () -> new Facts(CARRY, Termination.NORMAL_EXIT, invalid, CarryState.APPLIED));
        }
        assertThrows(IllegalArgumentException.class,
                () -> new Facts(CARRY, Termination.NORMAL_EXIT, null, CarryState.NOT_APPLICABLE));
    }

    @Test void repeatedFactsAreStatelessAndIndependentOfOtherOperations() {
        var facts = new Facts(FIXED, Termination.FINAL_SETTLEMENT, 2, CarryState.NOT_APPLICABLE);
        var original = evaluate(POLICY_REF, facts);
        decide(CARRY, Termination.FINAL_SETTLEMENT, null, CarryState.USED);
        decide(LOOP, Termination.FINAL_SETTLEMENT, 21, CarryState.NOT_APPLICABLE);
        assertEquals(original, evaluate(POLICY_REF, facts));
    }
}
