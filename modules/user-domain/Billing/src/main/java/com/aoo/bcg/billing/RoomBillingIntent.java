package com.aoo.bcg.billing;

import java.time.Instant;
import java.util.Objects;

/** Stable pre-charge metadata, never evidence that the business or payment succeeded. */
public record RoomBillingIntent(RoomChargeOperation operation,Instant createdAt){
    public RoomBillingIntent{
        Objects.requireNonNull(operation,"operation");Objects.requireNonNull(createdAt,"createdAt");
        if(createdAt.getNano()%1_000_000!=0)throw new IllegalArgumentException("intent time requires millisecond precision");
    }
}
