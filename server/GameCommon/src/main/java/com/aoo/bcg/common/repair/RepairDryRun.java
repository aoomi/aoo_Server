package com.aoo.bcg.common.repair;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;

/** Mandatory non-mutating analysis produced before a repair can be approved. */
public final class RepairDryRun<T> {
    public record Invariant<T>(String name, Predicate<T> check) {
        public Invariant { if (name == null || name.isBlank()) throw new IllegalArgumentException("invariant name required"); Objects.requireNonNull(check); }
    }
    public record Report<T>(long scanned, long matched, long estimatedChanges, List<T> samples,
                            Map<String, Long> invariantViolations, boolean mutationAttempted) { }

    public Report<T> analyze(Iterable<T> source, Predicate<T> selector, Function<T,T> preview,
                             List<Invariant<T>> invariants, int sampleLimit) {
        Objects.requireNonNull(source); Objects.requireNonNull(selector); Objects.requireNonNull(preview); Objects.requireNonNull(invariants);
        if (sampleLimit < 1 || sampleLimit > 100) throw new IllegalArgumentException("sampleLimit must be 1..100");
        long scanned=0, matched=0, changed=0; List<T> samples=new ArrayList<>();
        var violations = new java.util.LinkedHashMap<String,Long>(); invariants.forEach(i -> violations.put(i.name(),0L));
        for (T current : source) {
            scanned++; if (!selector.test(current)) continue; matched++;
            T proposed=preview.apply(current); if (!Objects.equals(current,proposed)) changed++;
            for (Invariant<T> invariant : invariants) if (!invariant.check().test(proposed)) violations.compute(invariant.name(),(k,v)->v+1);
            if (samples.size()<sampleLimit) samples.add(proposed);
        }
        return new Report<>(scanned,matched,changed,List.copyOf(samples),Map.copyOf(violations),false);
    }
}
