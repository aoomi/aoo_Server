package com.aoo.bcg.gamespi.fsm;
import java.util.Objects;
public record StateTransition<S extends Enum<S>,E extends Enum<E>>(S source,E event,S target,String guardName,String sideEffectName){public StateTransition{Objects.requireNonNull(source);Objects.requireNonNull(event);Objects.requireNonNull(target);if(guardName==null||guardName.isBlank()||sideEffectName==null||sideEffectName.isBlank())throw new IllegalArgumentException("guard and side effect declarations required");}}
