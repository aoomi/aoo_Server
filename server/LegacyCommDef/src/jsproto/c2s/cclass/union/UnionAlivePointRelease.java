package jsproto.c2s.cclass.union;

import jsproto.c2s.cclass.Player.ShortPlayer;
import lombok.Data;

import java.io.Serializable;

/**
 *  中至 生存任务积分显示
 * 俱乐部玩家信息
 *
 * @author
 */
@Data
public class UnionAlivePointRelease implements Serializable{
	/**
	 * 亲友圈id
	 */
	private long clubID;
	/**
	 * 玩家游戏短ID
	 */
	private long playerID;
	/**
	 * 比赛券数量
	 */
	private int gameTicketCirculation=0;
	/**
	 * 生存积分要求
	 */
	private double alivePoint=0d;
	/**
	 * 生存积分状态（0:不开启,1:开启）
	 */
	private int alivePointStatus=0;
}
