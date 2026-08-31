package business.utils;

import business.global.club.Club;
import business.global.shareclub.ShareClubMemberMgr;
import business.player.Player;
import business.player.feature.PlayerClub;
import com.ddm.server.common.CommLogD;
import com.ddm.server.common.utils.CommMath;
import jsproto.c2s.cclass.club.ClubPlayerInfo;
import jsproto.c2s.cclass.club.ClubPlayerInfoZhongZhi;
import jsproto.c2s.cclass.club.Club_define;
import jsproto.c2s.cclass.union.UnionClubPlayerInfo;
import jsproto.c2s.cclass.union.UnionDefine;

public class ClubMemberUtils {
    /**
     * 成员状态比对
     *
     * @param sourceStatus
     * @param status
     * @return
     */
    public static boolean getSourceStatus(int sourceStatus, int status) {
        if (status == Club_define.Club_Player_Status.PLAYER_TUICHU_WEIPIZHUN.value()) {//查找退出未批准数据时 会筛选到之前无用的邀请数据 这边过滤
            return ((sourceStatus & status) > 0) && sourceStatus != Club_define.Club_Player_Status.PLAYER_YAOQING.value();
        }
        return (sourceStatus & status) > 0;
    }

    public static Integer getArrayValueInteger(String[] array, String fieldName) {
        Integer index = ShareClubMemberMgr.getInstance().getClubMemberIndexByName(fieldName);
        if (index == null) {
            return 0;
        } else {
            if (array.length - 1 < index) {
                return 0;
            } else {
                String str = array[ShareClubMemberMgr.getInstance().getClubMemberIndexByName(fieldName)];
                if (str != null) {
                    return Integer.parseInt(str);
                } else {
                    CommLogD.error("array field no exist [{}]", fieldName);
                    return 0;
                }
            }
        }
    }

    public static Double getArrayValueDouble(String[] array, String fieldName) {
        Integer index = ShareClubMemberMgr.getInstance().getClubMemberIndexByName(fieldName);
        if (index == null) {
            return 0D;
        } else {
            if (array.length - 1 < index) {
                return 0D;
            } else {
                String str = array[ShareClubMemberMgr.getInstance().getClubMemberIndexByName(fieldName)];
                if (str != null) {
                    return Double.parseDouble(str);
                } else {
                    CommLogD.error("array field no exist [{}]", fieldName);
                    return 0D;
                }
            }
        }
    }

    public static Long getArrayValueLong(String[] array, String fieldName) {
        Integer index = ShareClubMemberMgr.getInstance().getClubMemberIndexByName(fieldName);
        if (index == null) {
            return 0L;
        } else {
            if (array.length - 1 < index) {
                return 0L;
            } else {
                String str = array[ShareClubMemberMgr.getInstance().getClubMemberIndexByName(fieldName)];
                if (str != null) {
                    return Long.parseLong(str);
                } else {
                    CommLogD.error("array field no exist [{}]", fieldName);
                    return 0L;
                }
            }
        }
    }

    public static String getArrayValueString(String[] array, String fieldName) {
        Integer index = ShareClubMemberMgr.getInstance().getClubMemberIndexByName(fieldName);
        if (index == null) {
            return null;
        } else {
            if (array.length - 1 < index) {
                return null;
            } else {
                String str = array[ShareClubMemberMgr.getInstance().getClubMemberIndexByName(fieldName)];
                return str;
            }
        }
    }

    public static String[] stringSwitchArray(String value) {
        String[] array = value.split(";");
        return array;
    }

    /**
     * 检查是否存在有申请操作
     *
     * @return
     */
    public static boolean checkExistApply(int unionState) {
        UnionDefine.UNION_MATCH_STATE matchState = UnionDefine.UNION_MATCH_STATE.valueOf(getUnionState(unionState));
        return UnionDefine.UNION_MATCH_STATE.APPLY_REMATCH.equals(matchState) || UnionDefine.UNION_MATCH_STATE.BACK_OFF.equals(matchState);
    }

    public static int getUnionState(int unionState) {
        if (unionState <= UnionDefine.UNION_MATCH_STATE.MATCH_PLAYING.value()) {
            return UnionDefine.UNION_MATCH_STATE.MATCH_PLAYING.value();
        }
        return unionState;
    }

    /**
     * 是否归属推广员
     *
     * @param partnerPid
     * @return
     */
    public static boolean isSubordinate(long partnerPid, long sourcePartnerPid, int promotion) {
        return isNotPromotion(promotion) && sourcePartnerPid == partnerPid;
    }

    /**
     * 不是推广员
     *
     * @return
     */
    public static boolean isNotPromotion(int promotion) {
        return Club_define.Club_PROMOTION.CheckExpectedValue(Club_define.Club_PROMOTION.NOT, promotion);
    }

    /**
     * 获取俱乐部玩家信息
     *
     * @param player
     * @param agentsID
     * @param level
     * @return
     */
    public static ClubPlayerInfo getClubPlayerInfo(Player player, jsproto.c2s.cclass.Player.ShortPlayer upShortPlayer, long agentsID, int level, boolean isBanGame, int partner, double sportsPoint, int status, int isminister, int promotionManage, int deletetime, int creattime, int updatetime) {
        return getClubPlayerInfo(player, upShortPlayer, status, agentsID, level, isBanGame, partner, sportsPoint, isminister, promotionManage, deletetime, creattime, updatetime);
    }

    /**
     * 获取俱乐部玩家信息
     *
     * @param player
     * @param agentsID
     * @param level
     * @return
     */
    public static ClubPlayerInfo getClubPlayerInfoZhongZhi(Player player, jsproto.c2s.cclass.Player.ShortPlayer upShortPlayer, long agentsID, int level, boolean isBanGame, int partner, double sportsPoint, int status, int isminister, int promotionManage, int deletetime, int creattime, int updatetime, boolean onlineFlag, String lastLogin) {
        return getClubPlayerInfo(player, upShortPlayer, status, agentsID, level, isBanGame, partner, sportsPoint, isminister, promotionManage, deletetime, creattime, updatetime);
    }

    /**
     * 获取俱乐部玩家信息
     *
     * @param player
     * @param agentsID
     * @param level
     * @returngetClubPlayerInfoCompetitionZhongZhi
     */
    public static ClubPlayerInfoZhongZhi getClubPlayerInfoCompetitionZhongZhi(Player player, jsproto.c2s.cclass.Player.ShortPlayer upShortPlayer, long agentsID, int level, boolean isBanGame, int partner, double sportsPoint, int status, int isminister, int promotionManage, int deletetime, int creattime, int updatetime, boolean onlineFlag, String lastLogin, double eliminatePoint, double alivePoint) {
        return new ClubPlayerInfoZhongZhi(player.getShortPlayer(), isminister, getTime(deletetime, creattime, updatetime, status),
                sportsPoint, eliminatePoint, alivePoint);
    }
//    /**
//     * 获取俱乐部玩家信息
//     *
//     * @param player
//     * @param agentsID
//     * @param level
//     * @return
//     */
//    public static ClubPlayerInfo getClubPlayerInfo(Player player, long agentsID, int level, boolean isBanGame, int partner, double sportsPoint) {
//        return getClubPlayerInfo(player,null, getStatus(), agentsID, level, isBanGame, partner, sportsPoint);
//    }
//
//
//    /**
//     * 获取俱乐部玩家信息
//     *
//     * @param player
//     * @param agentsID
//     * @param level
//     * @return
//     */
//    public static ClubPlayerInfo getClubPlayerInfo(Player player, int status, long agentsID, int level, boolean isBanGame,
//                                            int partner, double sportsPoint) {
//        return getClubPlayerInfo(player,null,status,agentsID,level,isBanGame,partner,sportsPoint);
//    }

    /**
     * 获取俱乐部玩家信息
     *
     * @param player
     * @param agentsID
     * @param level
     * @return
     */
    public static ClubPlayerInfo getClubPlayerInfo(Player player, jsproto.c2s.cclass.Player.ShortPlayer upShortPlayer, int status, long agentsID, int level, boolean isBanGame,
                                                   int partner, double sportsPoint, int isminister, int promotionManage, int deletetime, int creattime, int updatetime) {
        return new ClubPlayerInfo(player.getShortPlayer(), upShortPlayer, status, isminister, getTime(deletetime, creattime, updatetime, status),
                player.getFeature(PlayerClub.class).getPlayerClubRoomCard(agentsID, level), isBanGame, partner, sportsPoint, promotionManage);
    }

    /**
     * 获取俱乐部玩家信息
     *
     * @param player
     * @param agentsID
     * @param level
     * @return
     */
    public static ClubPlayerInfo getClubPlayerInfoZhongZhi(Player player, jsproto.c2s.cclass.Player.ShortPlayer upShortPlayer, int status, long agentsID, int level, boolean isBanGame,
                                                           int partner, double sportsPoint, int isminister, int promotionManage, int deletetime, int creattime, int updatetime, boolean onlineFlag, String lastLogin) {
        return new ClubPlayerInfo(player.getShortPlayer(), upShortPlayer, status, isminister, getTime(deletetime, creattime, updatetime, status),
                player.getFeature(PlayerClub.class).getPlayerClubRoomCard(agentsID, level), isBanGame, partner, sportsPoint, promotionManage, onlineFlag, lastLogin);
    }

    /**
     * 根据传入的值返回时间
     */
    public static Integer getTime(int deletetime, int creattime, int updatetime, int status) {
        if (Club_define.Club_Player_Status.PLAYER_TUICHU.value() == status
                || Club_define.Club_Player_Status.PLAYER_TICHU.value() == status
                || Club_define.Club_Player_Status.PLAYER_TICHU_CLOSE.value() == status) {
            return deletetime;
        } else if (Club_define.Club_Player_Status.PLAYER_WEIPIZHUN.value() == status) {
            return creattime;
        } else {
            return updatetime;
        }
    }

    /**
     * 获取亲友圈疲劳值
     *
     * @param club 亲友圈信息
     * @return
     */
    public static double getSportsPoint(Club club, double sportsPoint) {
        return club.getClubListBO().getUnionId() > 0L ? sportsPoint : 0D;
    }

    /**
     * 赛事管理员查询亲友圈成员
     *
     * @param player         玩家信息
     * @param isUnionBanGame 是否被赛事管理员禁止游戏
     * @param sportsPoint    竞技点
     * @return
     */
    public static UnionClubPlayerInfo getUnionClubPlayerInfo(Player player, jsproto.c2s.cclass.Player.ShortPlayer upShortPlayer, boolean isUnionBanGame, double sportsPoint, int minister, double eliminatePoint) {
        return new UnionClubPlayerInfo(player.getShortPlayer(), upShortPlayer, isUnionBanGame, sportsPoint, minister, eliminatePoint);
    }

    public static Integer getIsLevel(int level, int isminister) {
        return isLevelPromotion(level) || isClubCreate(isminister) ? level : Integer.MAX_VALUE;
    }

    /**
     * 是推广员
     *
     * @return
     */
    public static boolean isLevelPromotion(int level) {
        return level > 0;
    }

    public static boolean isClubCreate(int isminister) {
        return Club_define.Club_MINISTER.Club_MINISTER_CREATER.value() == isminister;
    }

    public static int zhongZhiGetPosistion(int isminister, int level, int promotionManager) {
        if (Club_define.Club_MINISTER.Club_MINISTER_CREATER.value() == isminister) {
            return Club_define.Club_ZHONGZHI_POSISTION.Club_CREATER.value();
        }
        if (level > 0) {
            return Club_define.Club_ZHONGZHI_POSISTION.Club_PROMOTION.value();
        }
        if (promotionManager > 0) {
            return Club_define.Club_ZHONGZHI_POSISTION.Club_PROMOTION_MANAGER.value();
        }
        return Club_define.Club_ZHONGZHI_POSISTION.Club_MINISTER_GENERAL.value();
    }

    /**
     * 获取个人预警值
     *
     * @return
     */
    public static Double getSportsPointWarningPersonal(int personalWarnStatus, double personalSportsPointWarning) {
        if (UnionDefine.UNION_WARN_STATUS.OPEN.ordinal() == personalWarnStatus) {
            return personalSportsPointWarning;
        }
        return null;
    }

    /**
     * 获取个人预警值
     *
     * @return
     */
    public static Double getAlivePointZhongZhi(int alivePointStatus, double alivePoint) {
        if (UnionDefine.UNION_WARN_STATUS.OPEN.ordinal() == alivePointStatus) {
            return alivePoint;
        }
        return null;
    }

    /**
     * 获取推广员预警值
     *
     * @return
     */
    public static Double getSportsPointWarning(int level, int warnStatus, double sportsPointWarning) {
        if (isLevelPromotion(level) && UnionDefine.UNION_WARN_STATUS.OPEN.ordinal() == warnStatus) {
            return sportsPointWarning;
        }
        return null;
    }

    /**
     * 获取玩家身上的总竞技点分数 包括保险箱
     *
     * @return
     */
    public static double getTotalSportsPoint(double sportsPoint, double caseSportsPoint) {
        return CommMath.FormatDouble(sportsPoint + caseSportsPoint);
    }
//    public static void main(String[] args) {
//        Map<String,String> map=new HashMap<>();
//        map.put("1","1,2");
//        map.put("2","1,3");
//        map.put("3","2,4");
//        map.put("4","2,5");
//        map.put("5","2,6");
//        map.put("6","3,8");
//        Comparator<String[]> com = Comparator.comparing((String[] h) -> );
//        List<String[]> list=map.values().stream().map(k->k.split(",")).sorted((o1, o2) -> o1[0].compareTo(o2[0])).collect(Collectors.toList());
//        for (String[] s:list){
//            System.out.println(new Gson().toJson(s));
//        }
//
//    }
}
