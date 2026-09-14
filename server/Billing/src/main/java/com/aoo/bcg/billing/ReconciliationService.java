package com.aoo.bcg.billing;

import java.time.LocalDate;
import java.util.List;

public interface ReconciliationService {
    Report reconcile(LocalDate businessDate);
    record Difference(String type, String businessId, String expected, String actual) {}
    record Report(LocalDate businessDate, List<Difference> differences) {
        public Report { differences = List.copyOf(differences); }
        public boolean balanced() { return differences.isEmpty(); }
    }
}
