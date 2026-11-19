package jsproto.c2s.cclass.room;

import lombok.Data;

import java.io.Serializable;

/**
 * 房间信息
 * @author
 * * 简化版 2021/11/1傅哥要求
 * @param <T>
 */
@Data
public class RoomInfoShortUnionBanTable<T>  implements Comparable<RoomInfoShortUnionBanTable>, Serializable{

	/**
	 * 房间key
	 */
	private String roomKey;
	/**
	 * 类型Id
	 */
	private Integer gameId;
	/**
	 * 配置Id
	 */
	private int tagId;

	/**
	 * 房间名字
	 */
	private String roomName="";
	/**
	 * 0 未禁用  1禁用
	 */
	private int ban=0;

	public RoomInfoShortUnionBanTable(String roomKey, Integer gameId, int tagId, String roomName) {
		this.roomKey = roomKey;
		this.gameId = gameId;
		this.tagId = tagId;
		this.roomName = roomName;
	}

	@Override
	public int compareTo(RoomInfoShortUnionBanTable o) {
		return 0;
	}
}
