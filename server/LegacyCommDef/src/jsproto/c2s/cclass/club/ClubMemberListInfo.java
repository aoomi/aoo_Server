package jsproto.c2s.cclass.club;

import lombok.Data;

/**
 * @author FengZhnag
 * @date 2023/1/12 10:44
 * @description 成员列表查询需要的信息
 */
@Data
public class ClubMemberListInfo {
    private long id;// id
    private long playerID;//玩家游戏短ID
    private long upLevelId;//上级id
    private int level;//"等级(0:默认普通成员)
    private long clubID;//俱乐部ID
    private int banGame;// 禁止游戏
    private int promotion;//推广员状态(0不是推广员,1任命,2卸任)
    private double sportsPoint;//比赛分
    private int lastGameTime=0;//上次游戏时间
    private int blockNum=0;//拉黑次数
    private int deletetime;// 踢出时间
    private int promotionManage;//推广员管理（0:不是,1:是）
    private int isminister;// 职务 0普通会员 1管理 2创建者 3赛事管理员
    private int gameTicket=0;//比赛券
    private double eliminatePoint=0d;//个人淘汰分
    private int status;//状态
   private int creattime;// 申请时间
    private int updatetime;// 处理时间
    private String name;//玩家名字
    public static String getItemsName() {
        return "a.id,a.playerID,a.upLevelId,a.level,a.clubID,a.banGame,a.promotion,a.sportsPoint,a.lastGameTime,a.blockNum,a.deletetime,a.promotionManage,a.isminister,a.gameTicket,a.eliminatePoint,a.status,a.creattime,a.updatetime,b.name";
    }
}
