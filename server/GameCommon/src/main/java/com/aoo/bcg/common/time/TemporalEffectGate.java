package com.aoo.bcg.common.time;
import com.aoo.bcg.gamespi.time.AuthoritativeTimeSource;import java.time.Duration;import java.time.Instant;import java.util.Objects;import java.util.concurrent.ConcurrentHashMap;
/** At-most-once gate for token, room and scheduled effects, independent of wall-clock direction. */
public final class TemporalEffectGate{
 private final ConcurrentHashMap<String,Instant>effects=new ConcurrentHashMap<>();private final AuthoritativeTimeSource time;
 public TemporalEffectGate(AuthoritativeTimeSource time){this.time=Objects.requireNonNull(time);}
 public boolean acquire(String effectId,Duration retention){if(effectId==null||effectId.isBlank()||retention.isNegative()||retention.isZero())throw new IllegalArgumentException("invalid effect");Instant now=time.now();Instant until=now.plus(retention);var acquired=new java.util.concurrent.atomic.AtomicBoolean();effects.compute(effectId,(key,old)->{if(old==null||!old.isAfter(now)){acquired.set(true);return until;}return old;});return acquired.get();}
}
