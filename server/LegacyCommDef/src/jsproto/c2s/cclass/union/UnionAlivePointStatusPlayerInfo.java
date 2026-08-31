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
public class UnionAlivePointStatusPlayerInfo implements Serializable{
	/**
	 *      * 序数id
	 * 客户端排序用
	 */
	private long id;
	/**
	 * 玩家信息
	 */
	private ShortPlayer shortPlayer;

	/**
	 * 亲友圈id
	 */
	private long clubId;

	/**
	 * 状态
	 */
	private int status;
	/**
	 * 时间
	 */
	private int time;

	/**
	 * 生存积分
	 */
	private double alivePoint=0d;

	/**
	 * 生存状态
	 */
	private int alivePointStatus;

	/**
	 * 个人淘汰分
	 */
	private double eliminatePoint=0d;
	/**
	 * 比赛券数
	 */
	private int gameTicket;

	/**
	 * 最终积分
	 */
	private double zhongZhiTotalPoint;
	/**
	 * 发布时间
	 */
	private int releaseTime;
	/**
	 * 亲友圈标识
	 */
	private int clubSign;
	/**
	 * 俱乐部名称
	 */
	private String clubName = "";


	public UnionAlivePointStatusPlayerInfo(long id, ShortPlayer shortPlayer, int status, int time, double alivePoint, double eliminatePoint, int gameTicket, double zhongZhiTotalPoint, int alivePointStatus) {
		this.id = id;
		this.shortPlayer = shortPlayer;
		this.status = status;
		this.time = time;
		this.alivePoint = alivePoint;
		this.eliminatePoint = eliminatePoint;
		this.gameTicket = gameTicket;
		this.zhongZhiTotalPoint = zhongZhiTotalPoint;
		this.alivePointStatus = alivePointStatus;
	}
	public UnionAlivePointStatusPlayerInfo( ShortPlayer shortPlayer, long clubId,int status, int time, double alivePoint, double eliminatePoint, int gameTicket, double zhongZhiTotalPoint,int alivePointStatus,int clubSign,String clubName) {
		this.shortPlayer = shortPlayer;
		this.clubId = clubId;
		this.status = status;
		this.time = time;
		this.alivePoint = alivePoint;
		this.eliminatePoint = eliminatePoint;
		this.gameTicket = gameTicket;
		this.zhongZhiTotalPoint = zhongZhiTotalPoint;
		this.status = status;
		this.alivePointStatus = alivePointStatus;
		this.clubSign = clubSign;
		this.clubName = clubName;
	}
	public UnionAlivePointStatusPlayerInfo(  double alivePoint , int gameTicket, int releaseTime,int alivePointStatus) {
		this.alivePoint = alivePoint;
		this.gameTicket = gameTicket;
		this.releaseTime = releaseTime;
		this.alivePointStatus = alivePointStatus;
	}
}
