package com.aoo.bcg.gamespi.fsm;
import java.util.*;
/** Explicit-only room state migration. Active rounds remain pinned unless a converter declares safety. */
public final class StateMigrationRegistry{
 private record Key(String from,String to){}private final Map<Key,StateMigration>migrations=new HashMap<>();
 public synchronized void register(StateMigration migration){var key=new Key(migration.fromPlayVersion(),migration.toPlayVersion());if(key.from().equals(key.to())||migrations.putIfAbsent(key,migration)!=null)throw new IllegalArgumentException("invalid or duplicate migration");}
 public synchronized Map<String,Object> migrate(String from,String to,RoomLifecycleState roomState,Map<String,Object>state){if(from.equals(to))return Map.copyOf(state);StateMigration migration=migrations.get(new Key(from,to));if(migration==null)throw new IllegalStateException("explicit state migration is not registered");boolean inProgress=roomState==RoomLifecycleState.PLAYING||roomState==RoomLifecycleState.SETTLING;if(inProgress&&!migration.supportsInProgressRound())throw new IllegalStateException("in-progress room is pinned to "+from);Map<String,Object>converted=migration.convert(Map.copyOf(state));if(converted==null)throw new IllegalStateException("migration returned null");return Map.copyOf(converted);}
}
