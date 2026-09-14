package jsproto.c2s.cclass.pk;

import java.util.ArrayList;
import java.util.List;

//房间战绩
public class PKRoom_Record {

    public long roomID;//房间ID
    public int endSec;//房间结束的秒
    public int setCnt;//局数
    public String roomKey;//房间key
    public List<PKRoom_RecordPosInfo> recordPosInfosList = new ArrayList<>();//胜利次数
}
