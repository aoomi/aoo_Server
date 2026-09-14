package com.aoo.bcg.gateway;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantLock;
/** Serializes operations per room while allowing different rooms to proceed independently. */
public final class RoomOrderedExecutor {
 private static final class Holder { final ReentrantLock lock=new ReentrantLock(true); int references; }
 private final ConcurrentHashMap<Long,Holder> locks=new ConcurrentHashMap<>();
 public <T> T execute(long roomId,Callable<T> task){
  if(roomId<=0)throw new IllegalArgumentException("roomId required");
  Holder holder=locks.compute(roomId,(ignored,current)->{Holder value=current==null?new Holder():current;value.references++;return value;});
  boolean acquired=false;
  try{
   acquired=holder.lock.tryLock(5,TimeUnit.SECONDS);
   if(!acquired)throw new IllegalStateException("room lock acquisition timed out: "+roomId);
   return task.call();
  }catch(InterruptedException e){Thread.currentThread().interrupt();throw new IllegalStateException("room lock acquisition interrupted: "+roomId,e);}
  catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalStateException(e);}
  finally{if(acquired)holder.lock.unlock();locks.compute(roomId,(ignored,current)->{if(current!=holder)throw new IllegalStateException("room lock ownership corrupted");return --holder.references==0?null:holder;});}
 }
}
