package jsproto.c2s.cclass.club;

import jsproto.c2s.cclass.BaseSendMsg;
import jsproto.c2s.cclass.Player;
import jsproto.c2s.cclass.union.UnionDynamicItemZhongZhiRecord;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 个人积分变化记录
 * @author zaf
 *
 */

@Data
@NoArgsConstructor
public class ClubCompetitionRecordSelf extends BaseSendMsg {

	/**
	 * 变换记录
	 */
	public List<UnionDynamicItemZhongZhiRecord> unionDynamicItemList;
	/**
	 * 排名
	 */
	private int id;
	/**
	 * 成员积分
	 */
	private  double sportsPoint;;
	/**
	 * 淘汰分
	 */
	private double eliminatePoint;
	/***
	 * 大赢家总和
 	 */
	private int bigWinnerSum;
	/**
	 * 有效耗卡总和
	 */
	private double consumeSum;
	/**
	 * 联赛结束时间
	 */
	private int endRoundTime;
}