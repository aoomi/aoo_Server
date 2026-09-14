package com.aoo.bcg.common.room;
import java.time.Instant;
public interface RoomCodeStore{boolean reserve(String code,Instant now,Instant activeUntil,Instant reusableAfter);boolean active(String code,Instant now);}
