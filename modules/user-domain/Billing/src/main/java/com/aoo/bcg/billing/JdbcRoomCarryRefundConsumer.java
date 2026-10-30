package com.aoo.bcg.billing;

import java.sql.*;
import java.util.Objects;
import static com.aoo.bcg.billing.RoomCarryReceipt.*;

/** Consumes proof for an existing original CARRY fee; never reserves a new fee. */
public final class JdbcRoomCarryRefundConsumer {
    private final JdbcRoomCarryReceiptRepository receipts;
    private final JdbcRoomChargeCoordinator charges;
    public JdbcRoomCarryRefundConsumer(JdbcRoomCarryReceiptRepository receipts,JdbcRoomChargeCoordinator charges){this.receipts=Objects.requireNonNull(receipts);this.charges=Objects.requireNonNull(charges);}
    public void consumeOriginal(Connection c,String effectId,Original expected,StoredProofVerifier verifier)throws SQLException {
        JdbcRoomChargeCoordinator.requireCallerTransaction(c);Effect effect=receipts.requireFinalized(c,effectId,verifier);
        Original original=receipts.requireOriginal(c,effect,expected);var charge=charges.findCarry(c,original.operation(),original.subject());
        if(!"ROOM_CARD".equals(charge.currency()))throw new IllegalArgumentException("original effective currency conflict");
        String evidence=RoomChargeOperation.hash("CARRY_ORIGINAL_EFFECT_V1",effect.effectId(),effect.attempt().attemptId(),effect.stage(),effect.ordinal(),effect.stateVersion());
        if(effect.stage()==Stage.USED){charges.confirmCarry(c,charge.requestId(),"carry-used:"+effect.attempt().attemptId(),new JdbcRoomChargeCoordinator.OutcomeEvidence("carry-effect:"+effect.effectId(),evidence));}
        else if(effect.stage()==Stage.TERMINATED_UNUSED){
            if("CONFIRMED".equals(charge.state()))throw new IllegalStateException("used original cannot refund as unused");
            var decision=RoomCardRefundPolicy.evaluate(RoomCardRefundPolicy.POLICY_REF,new RoomCardRefundPolicy.Facts(RoomChargeOperation.Kind.CARRY,RoomCardRefundPolicy.Termination.NORMAL_EXIT,null,RoomCardRefundPolicy.CarryState.APPLIED));
            if(decision.action()!=RoomCardRefundPolicy.Action.REFUND_ORIGINAL_REMAINDER)throw new IllegalStateException("refund policy denied");
            charges.refundCarry(c,charge.requestId(),"carry-unused:"+effect.attempt().attemptId(),new JdbcRoomChargeCoordinator.RefundEvidence(JdbcRoomChargeCoordinator.RefundCause.POLICY_ALLOWED,"carry-effect:"+effect.effectId(),evidence));
        }
    }
}
