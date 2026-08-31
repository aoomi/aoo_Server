package com.aoo.bcg.common.concurrency;
import java.util.ArrayDeque;import java.util.Objects;
/** Enforces the global nested lock order: global, club, room, player, asset, database. */
public final class LockOrderGuard{
 public enum Level{GLOBAL,CLUB,ROOM,PLAYER,ASSET,DATABASE}
 private record Claim(Level level,String key){}
 private static final ThreadLocal<ArrayDeque<Claim>>HELD=ThreadLocal.withInitial(ArrayDeque::new);
 private LockOrderGuard(){}
 public static Scope enter(Level level,String key){Objects.requireNonNull(level);if(key==null||key.isBlank())throw new IllegalArgumentException("lock key required");var held=HELD.get();var current=held.peekLast();if(current!=null&&(level.ordinal()<current.level().ordinal()||(level==current.level()&&key.compareTo(current.key())<=0)))throw new IllegalStateException("lock order violation: held "+current+", requested "+level+":"+key);var claim=new Claim(level,key);held.addLast(claim);return new Scope(claim);}
 public static final class Scope implements AutoCloseable{private final Claim claim;private boolean closed;private Scope(Claim claim){this.claim=claim;}@Override public void close(){if(closed)return;var held=HELD.get();if(!claim.equals(held.peekLast()))throw new IllegalStateException("lock scopes must close in reverse order");held.removeLast();closed=true;if(held.isEmpty())HELD.remove();}}
}
