package com.aoo.bcg.common.cache;

import java.util.ArrayList;import java.util.List;

/** One invalidation contract spanning local, distributed and derived caches. */
public final class CacheInvalidationCoordinator{
 public enum Scope{ROOM,PLAY_VERSION,ACCOUNT}
 public record Invalidation(Scope scope,String identity,String reason) { public Invalidation{if(scope==null||identity==null||identity.isBlank()||reason==null||reason.isBlank())throw new IllegalArgumentException("invalid cache invalidation");} }
 @FunctionalInterface public interface CacheBackend{void invalidate(Invalidation invalidation)throws Exception;}
 private final List<CacheBackend> backends;
 public CacheInvalidationCoordinator(List<CacheBackend> backends){if(backends==null||backends.isEmpty())throw new IllegalArgumentException("cache backends required");this.backends=List.copyOf(backends);}
 public void roomEnded(long roomId){if(roomId<=0)throw new IllegalArgumentException("roomId must be positive");invalidate(new Invalidation(Scope.ROOM,Long.toString(roomId),"room-ended"));}
 public void playVersionUnpublished(int gameId,String playVersion){if(gameId<=0||playVersion==null||playVersion.isBlank())throw new IllegalArgumentException("invalid play version");invalidate(new Invalidation(Scope.PLAY_VERSION,gameId+":"+playVersion,"play-version-unpublished"));}
 public void accountLoggedOut(long accountId){if(accountId<=0)throw new IllegalArgumentException("accountId must be positive");invalidate(new Invalidation(Scope.ACCOUNT,Long.toString(accountId),"account-logout"));}
 private void invalidate(Invalidation value){var failures=new ArrayList<Throwable>();for(var backend:backends)try{backend.invalidate(value);}catch(Throwable failure){failures.add(failure);}if(!failures.isEmpty()){var failure=new IllegalStateException("cache invalidation incomplete: "+value.scope()+":"+value.identity());failures.forEach(failure::addSuppressed);throw failure;}}
}
