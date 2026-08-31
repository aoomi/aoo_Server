package jsproto.c2s.cclass.zjh;

import java.util.ArrayList;
import java.util.List;

import jsproto.c2s.cclass.Player.ShortPlayer;
import jsproto.c2s.cclass.pk.PKRoom_RecordPosInfo;

//房间战绩
public class ZJHRoom_Record {

	public long roomID;//房间ID
	public int endSec;//房间结束的秒
	public int setCnt;//局数

	public List<ShortPlayer> players = new ArrayList<>(); // 玩家信息


	public List<PKRoom_RecordPosInfo> recordPosInfosList = new ArrayList<>();//胜利次数
}
