package com.aoo.bcg.account;

/** Read-only account projection; all mutations are owned by the Billing ledger. */
@FunctionalInterface
public interface AccountAssetView {
    long balance(long accountId, String currency);
}
