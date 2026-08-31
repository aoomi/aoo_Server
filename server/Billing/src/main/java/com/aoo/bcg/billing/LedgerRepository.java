package com.aoo.bcg.billing;
import java.util.Optional;
public interface LedgerRepository { Optional<LedgerEntry> findByBusinessId(String businessId); LedgerEntry append(LedgerEntry entry); }
