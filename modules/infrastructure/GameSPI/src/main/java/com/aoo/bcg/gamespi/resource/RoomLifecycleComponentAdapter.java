package com.aoo.bcg.gamespi.resource;

import com.aoo.bcg.gamespi.RoomLifecycleExtension;import com.aoo.bcg.gamespi.RoomReadView;

/** Brings retained gameplay lifecycle extensions under the same managed cleanup contract. */
public final class RoomLifecycleComponentAdapter implements ManagedPlayComponent{
 private final String id;private final RoomLifecycleExtension extension;private final RoomReadView room;
 public RoomLifecycleComponentAdapter(String id,RoomLifecycleExtension extension,RoomReadView room){this.id=id;this.extension=extension;this.room=room;}
 public String componentId(){return id;}public void initialize(){extension.onCreated(room);}public void start(){extension.onStarted(room);}public void resetBetweenRounds(){extension.onRoundEnded(room);}public void destroy(){extension.onDissolved(room);}
}
