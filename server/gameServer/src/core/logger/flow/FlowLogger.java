package core.logger.flow;

import core.db.other.FlowLoggerMgrBase;

public class FlowLogger extends FlowLoggerMgrBase<GameFlowLogger> {

    private static FlowLogger instance = new FlowLogger();

    public static FlowLogger getInstance() {
        return instance;
    }


    // Coin Charge 金币消费日志
    public static void goldChargeLog(long pid, int reason, int num, int cur_remainder, int pre_value, int type, int classificationRegionId) {
        instance.log("goldChargeLog", pid, reason, num, cur_remainder, pre_value, type, classificationRegionId);
    }

    // Player Online 在线人数
    public static void playerOnLineChargeLog(int onlineCount) {
        instance.log("playerOnLineChargeLog", onlineCount);
    }

    // Room Card 房卡消费日志
    public static void roomCardChargeLog(long pid, int reason, int num, int cur_remainder, int pre_value, int type, long familyID, int gameType, int rechargeType, int classificationRegionId) {
        instance.log("roomCardChargeLog", pid, reason, num, cur_remainder, pre_value, type, familyID, gameType,
                rechargeType, classificationRegionId);
    }

    // Room Card 亲友圈房卡消费日志
    public static void clubRoomCardChargeLog(long pid, long clubID, int status, int reason, int num, int cur_remainder,
                                             int pre_value, int type, int gameType, int rechargeType, long agentsID, int level, int classificationRegionId) {
        instance.log("clubRoomCardChargeLog", pid, clubID, status, reason, num, cur_remainder, pre_value, type,
                gameType, rechargeType, agentsID, level, classificationRegionId);
    }

    // Room Card 赛事房卡消费日志
    public static void unionRoomCardChargeLog(long pid, long unionId, int status, int reason, int num, int cur_remainder,
                                              int pre_value, int type, int gameType, int rechargeType, long agentsID, int level, int classificationRegionId) {
        instance.log("unionRoomCardChargeLog", pid, unionId, status, reason, num, cur_remainder, pre_value, type,
                gameType, rechargeType, agentsID, level, classificationRegionId);
    }


    // PlayerDataLogFlow 玩家数据日志
    public static void playerDataLog(long pid, long accountId, long reg_time, int trial_mark, int sign, int classificationRegionId) {
        instance.log("playerDataLog", pid, accountId, reg_time, trial_mark, sign, classificationRegionId);
    }

    // PlayerRoomLogFlow 玩家房间日志
    public static void playerRoomLog(String date_time, long ownner_id, long room_id, int set_count, String player_list, int sum_count,
                                     long club_id, String room_key, int room_card, int createRoomTime, int clubCostType, int gameType,
                                     int classificationRegionId, long unionId) {
        instance.log("playerRoomLog", date_time, ownner_id, room_id, set_count, player_list, sum_count, club_id, room_key,
                room_card, createRoomTime, clubCostType, gameType, classificationRegionId, unionId);
    }

    // PlayerLoginLogFlow 玩家房间日志
    public static void playerLoginLog(long pid, int last_login, int last_logout, String game_list, String ip,
                                      String latitude, String longitude, String address) {
        instance.log("playerLoginLog", pid, last_login, last_logout, game_list, ip, latitude, longitude, address);
    }

    public static void playerClubCardLog(long pid, int num, int cur_remainder, int pre_value, int type, int gameType,
                                         int rechargeType, long roomID, long agentsID, int level, long clubID, int reason, int classificationRegionId) {
        instance.log("playerClubCardLog", pid, num, cur_remainder, pre_value, type, gameType, rechargeType, roomID,
                agentsID, level, clubID, reason, classificationRegionId);

    }

    public static void familyCardChargeLog(long familyID, long pid, int num, int cur_remainder, int pre_value, int type,
                                           int sourceType, int classificationRegionId) {
        instance.log("familyCardChargeLog", familyID, pid, num, cur_remainder, pre_value, type, sourceType, classificationRegionId);

    }

    // clubCardWinnerRebateLog 圈卡大赢家返利
    @Deprecated
    public static void clubCardWinnerRebateLog(long roomID, long agentsID, int level, long clubID, String winnerPid,
                                               int clubLevel, int agentsCard, int officialCard, int actualCard, int money, int paymentRoomCardType,
                                               int gameType, int playerNum, int classificationRegionId) {
        instance.log("clubCardWinnerRebateLog", roomID, agentsID, level, clubID, winnerPid, clubLevel, agentsCard,
                officialCard, actualCard, money, paymentRoomCardType, gameType, playerNum, classificationRegionId);
    }


    public static void unionSportsPointProfitLog(long unionId, long clubId, double scorePoint, int sourceType, String roomName, int roomKey, long roomId) {
        instance.log("unionSportsPointProfitLog", unionId, clubId, scorePoint, sourceType, roomName, roomKey, roomId);
    }

    public static void roomPromotionPointLog(long pid, String dateTime, long clubId, long unionId, int reason, double num, double curRemainder, double preValue, int type, int gameId, int classificationRegionId, long roomId, long execPid, String roomName, String msg, String roomKey, long reasonPid) {
        instance.log("roomPromotionPointLog", pid, dateTime, clubId, unionId, reason, num, curRemainder, preValue, type, gameId, classificationRegionId, roomId, execPid, roomName, msg, roomKey, reasonPid);
    }

    public static void roomPromotionPointCountLog(long pid, String dateTime, long clubId, long unionId, int reason, double num, double curRemainder, double preValue, int type, int gameId, int classificationRegionId, long roomId, long execPid, String roomName, String msg, int execTime, String roomKey) {
        instance.log("roomPromotionPointCountLog", pid, dateTime, clubId, unionId, reason, num, curRemainder, preValue, type, gameId, classificationRegionId, roomId, execPid, roomName, msg, execTime, roomKey);
    }

    public static void sportsPointChargeLog(long pid, long clubId, long unionId, int reason, double num, double curRemainder, double preValue, int type, int gameId, int classificationRegionId, long roomId) {
        instance.log("sportsPointChargeLog", pid, clubId, unionId, reason, num, curRemainder, preValue, type, gameId, classificationRegionId, roomId);
    }

    public static void casePointChargeLog(long pid, long clubId, long unionId, double num, double curRemainder, double preValue, double caseSportCurRemainder, double caseSportPreValue, int type) {
        instance.log("casePointChargeLog", pid, clubId, unionId, num, curRemainder, preValue, caseSportCurRemainder, caseSportPreValue, type);
    }

    public static void promotionMultiChangLog(long pid, long clubId, long exePid, int oldNum, int nowNum, int exeOldNum, int exeNowNum, long doPid, String pidList, String pidListNow, String upPidList, String upPidListNow) {
        instance.log("promotionMultiChangLog", pid, clubId, exePid, oldNum, nowNum, exeOldNum, exeNowNum, doPid, pidList, pidListNow, upPidList, upPidListNow);
    }


    public static void scorePercentChargeLog(long pid, long unionId, long clubId, int curRemainder, int preValue, long exePid) {
        instance.log("scorePercentChargeLog", pid, unionId, clubId, curRemainder, preValue, exePid);
    }

    public static void sportsPointWarningLog(long pid, long clubId, long unionId, long upPid, double sportsPointWarning, double calcSportsPointWarning, long roomId) {
        instance.log("sportsPointWarningLog", pid, clubId, unionId, upPid, sportsPointWarning, calcSportsPointWarning, roomId);
    }

    public static void scoreDividedIntoChargeLog(long pid, long unionId, long clubId, double curRemainder, double preValue, long exePid) {
        instance.log("scoreDividedIntoChargeLog", pid, unionId, clubId, curRemainder, preValue, exePid);
    }

    public static void luckDrawRecordLog(long pid, String prizeName, int prizeType, int rewardNum, int createTime) {
        instance.log("luckDrawRecordLog", pid, prizeName, prizeType, rewardNum, createTime);
    }

    public static void gameServerMaintainLog(long startServerTime, long stopServerTime, int spacing, int httpPost, int clientPort, String name, int pid) {
        instance.log("gameServerMaintainLog", startServerTime, stopServerTime, spacing, httpPost, clientPort, name, pid);
    }

    public static void unionMatchLog(int rankingId, String name, long pid, int clubSign, String clubName, double sportsPoint, int roundId, long unionId, long clubId) {
        instance.log("unionMatchLog", rankingId, name, pid, clubSign, clubName, sportsPoint, roundId, unionId, clubId);
    }

    public static void clubPromotionActiveReportFormLog(long clubId, long partnerPid, double value, String date_time) {
        instance.log("clubPromotionActiveReportFormLog", clubId, partnerPid, value, date_time);
    }

    public static void clubPromotionActiveChargeLog(long pid, long clubId, long unionId, int reason, double num, double cur_remainder, double pre_value, int type, long partnerPid) {
        instance.log("clubPromotionActiveChargeLog", pid, clubId, unionId, reason, num, cur_remainder, pre_value, type, partnerPid);
    }

    public static void clubPromotionDayActiveChargeLog(long pid, long clubId, long unionId, int reason, double num, double cur_remainder, double pre_value, int type, long partnerPid) {
        instance.log("clubPromotionDayActiveChargeLog", pid, clubId, unionId, reason, num, cur_remainder, pre_value, type, partnerPid);
    }

    public static void clubPromotionSumActiveChargeLog(long pid, long clubId, long unionId, int reason, double num, double cur_remainder, double pre_value, int type, long partnerPid) {
        instance.log("clubPromotionSumActiveChargeLog", pid, clubId, unionId, reason, num, cur_remainder, pre_value, type, partnerPid);
    }

    public static void clubLevelRoomLog(String date_time, long pid, int winner, int consume, long roomId, long upLevelId, long memberId, int point, double sportsPoint, int setCount, double sportsPointConsume, double roomSportsPointConsume, double roomAvgSportsPointConsume, long clubId, long unionId, double promotionShareValue, long playGamePid) {
        instance.log("clubLevelRoomLog", date_time, pid, winner, consume, roomId, upLevelId, memberId, point, sportsPoint, setCount, sportsPointConsume, roomSportsPointConsume, roomAvgSportsPointConsume, clubId, unionId, promotionShareValue, playGamePid);
    }

    public static void clubLevelRoomLogShareValue(String date_time, long pid, int winner, int consume, long roomId, long upLevelId, long memberId, int point, double sportsPoint, int setCount, double sportsPointConsume, double roomSportsPointConsume, double roomAvgSportsPointConsume, long clubId, long unionId, double promotionShareValue, long playGamePid) {
        instance.log("clubLevelRoomLog", date_time, pid, winner, consume, roomId, upLevelId, memberId, point, sportsPoint, setCount, sportsPointConsume, roomSportsPointConsume, roomAvgSportsPointConsume, clubId, unionId, promotionShareValue, playGamePid);
    }

    public static void clubLevelRoomLogShareValueZhongZhi(String date_time, long pid, int winner, int consume, long roomId, long upLevelId, long memberId, int point, double sportsPoint, int setCount, double sportsPointConsume, double roomSportsPointConsume, double roomAvgSportsPointConsume, long clubId, long unionId, double promotionShareValue, long playGamePid) {
        instance.log("clubLevelRoomLogZhongZhi", date_time, pid, winner, consume, roomId, upLevelId, memberId, point, sportsPoint, setCount, sportsPointConsume, roomSportsPointConsume, roomAvgSportsPointConsume, clubId, unionId, promotionShareValue, playGamePid);
    }

    public static void clubLevelRoomCountLog(String date_time, int setCount, int winner, int consume, long roomSize, long upLevelId, long memberId, long sumPoint, double sportsPoint, double sportsPointConsume, double roomSportsPointConsume, double roomAvgSportsPointConsume, long clubId, long unionId, double promotionShareValue) {
        instance.log("clubLevelRoomCountLog", date_time, setCount, winner, consume, roomSize, upLevelId, memberId, sumPoint, sportsPoint, sportsPointConsume, roomSportsPointConsume, roomAvgSportsPointConsume, clubId, unionId, promotionShareValue, 0, "", 0D, 0D);
    }

    public static void clubLevelRoomCountLogZhongZhi(String date_time, int setCount, int winner, int consume, long roomSize, long upLevelId, long memberId, long sumPoint, double sportsPoint, double sportsPointConsume, double roomSportsPointConsume, double roomAvgSportsPointConsume, long clubId, long unionId, double promotionShareValue) {
        instance.log("clubLevelRoomCountLogZhongZhi", date_time, setCount, winner, consume, roomSize, upLevelId, memberId, sumPoint, sportsPoint, sportsPointConsume, roomSportsPointConsume, roomAvgSportsPointConsume, clubId, unionId, promotionShareValue, 0, "", 0D, 0D);
    }

    public static void unionLevelRoomCountLog(String date_time, long clubId, long unionID, int sizePlayer, String shareValue, double scorePoint, double personalSportsPoint) {
        instance.log("clubLevelRoomCountLog", date_time, 0, 0, 0, 0, 0, 0, 0, 0D, 0D, 0D, 0D, clubId, unionID, 0D, sizePlayer, shareValue, scorePoint, personalSportsPoint);
    }

    public static void sportsPointErrorLog(long unionId, double correctValue, double errorValue) {
        instance.log("sportsPointErrorLog", unionId, correctValue, errorValue);
    }

    public static void clubMemberRemoveLog(long memberId, long pid, long clubId, int reason, int status, int isminister, long exePid, int level, long upLevelId, int creattime, int updatetime, int deletetime) {
        instance.log("clubMemberRemoveLog", memberId, pid, clubId, reason, status, isminister, exePid, level, upLevelId, creattime, updatetime, deletetime);
    }

    public static void roomConfigPrizePoolLog(String date_time, long configId, long roomId, int setCount, int roomSize, int value, long unionId, String roomName, String dataJsonCfg, double prizePool, int gameId, double sportPointIncome, double roomSportsPointConsume) {
        instance.log("roomConfigPrizePoolLog", date_time, configId, roomId, setCount, roomSize, value, unionId, roomName, dataJsonCfg, prizePool, gameId, sportPointIncome, roomSportsPointConsume);

    }

    public static void roomConfigPrizePoolLogZhongZhi(String date_time, long configId, long roomId, int setCount, int roomSize, int value, long unionId, String roomName, String dataJsonCfg, double prizePool, int gameId, double sportPointIncome, double roomSportsPointConsume) {
        instance.log("roomConfigPrizePoolLogZhongZhi", date_time, configId, roomId, setCount, roomSize, value, unionId, roomName, dataJsonCfg, prizePool, gameId, sportPointIncome, roomSportsPointConsume);

    }

    public static void playerChangeCityLog(long pid, int pre_city, int cur_city) {
        instance.log("playerChangeCityLog", pid, pre_city, cur_city);
    }

    public static void playerExceptionLog(long pid, int exceptionType, String content) {
        instance.log("playerExceptionLog", pid, exceptionType, content);
    }

    public static void clubmemberStatusLog(long pid, long clubId, long exePid, long exeClubId, int execType) {
        instance.log("clubmemberStatusLog", pid, clubId, exePid, exeClubId, execType);
    }

    public static void sportsPointChangeZhongZhiLog(long pid, long clubId, long unionId, int reason, double num, double curRemainder, double preValue, int type, int gameId, int classificationRegionId, long roomId) {
        instance.log("sportsPointChangeZhongZhiLog", pid, clubId, unionId, reason, num, curRemainder, preValue, type, gameId, classificationRegionId, roomId);
    }

    public static void sportsPointChangeZhongZhiCountLog(long pid, long clubId, long unionId, int reason, double num, double curRemainder, double preValue, int type, int gameId, int classificationRegionId, long roomId) {
        instance.log("sportsPointChangeZhongZhiCountLog", pid, clubId, unionId, reason, num, curRemainder, preValue, type, gameId, classificationRegionId, roomId);
    }
}