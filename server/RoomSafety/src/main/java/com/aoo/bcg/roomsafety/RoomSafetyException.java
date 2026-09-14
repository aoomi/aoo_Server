package com.aoo.bcg.roomsafety;
public final class RoomSafetyException extends RuntimeException{
 public final int status;public final String code;
 public RoomSafetyException(int status,String code,String message){super(message);this.status=status;this.code=code;}
}
