package com.aoo.bcg.families;

import java.util.Collection;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;

/** Produces a deterministic migration gap list between legacy actions and a new capability set. */
public final class ActionCapabilityAudit {
    public record Report(Set<String> supported, Set<String> unsupportedLegacy, Set<String> newOnly) {}
    private ActionCapabilityAudit() {}
    public static Report compare(Collection<String> legacy, Collection<String> current) {
        TreeSet<String> old=normalize(legacy), now=normalize(current), unsupported=new TreeSet<>(old), added=new TreeSet<>(now);
        unsupported.removeAll(now); added.removeAll(old);
        return new Report(Set.copyOf(now),Set.copyOf(unsupported),Set.copyOf(added));
    }
    private static TreeSet<String> normalize(Collection<String> values){if(values==null)throw new IllegalArgumentException("actions required");TreeSet<String> result=new TreeSet<>();for(String value:values){if(value==null||value.isBlank())throw new IllegalArgumentException("blank action");result.add(value.strip().toUpperCase(Locale.ROOT).replace('-','_'));}return result;}
}
