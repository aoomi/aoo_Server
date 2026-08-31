package core.logger.flow;

import core.db.other.FlowLoggerBase;

public abstract class GameFlowLogger extends FlowLoggerBase {


    // Coin Charge 金币消费日志
    public void goldChargeLog(long pid, int reason, int num, int cur_remainder, int pre_value,
                              int type, int classificationRegionId) {
    }

    // Player Online 在线人数
    public void playerOnLineChargeLog(int onlineCount) {
    }

    // Room Card 房卡消费日志
    public void roomCardChargeLog(long pid, int reason, int num, int cur_remainder, int pre_value, int type,
                                  long familyID, int gameType, int rechargeType, int classificationRegionId) {
    }

    // Room Card 房卡消费日志
    public void clubRoomCardChargeLog(long pid, long clubID, int status, int reason, int num, int cur_remainder,
                                      int pre_value, int type, int gameType, int rechargeType, long agentsID, int level, int classificationRegionId) {

    }

    // Room Card赛事 房卡消费日志
    public void unionRoomCardChargeLog(long pid, long unionId, int status, int reason, int num, int cur_remainder,
                                       int pre_value, int type, int gameType, int rechargeType, long agentsID, int level, int classificationRegionId) {

    }

    // PlayerDataLogFlow 玩家数据日志
    public void playerDataLog(long pid, long accountId, long reg_time, int trial_mark, int sign, int classificationRegionId) {
    }

    // PlayerRoomLogFlow 玩家房间日志
    public void playerRoomLog(String date_time, long ownner_id, long room_id, int set_count, String player_list, int sum_count,
                              long club_id, String room_key, int room_card, int createRoomTime, int paymentRoomCardType, int gameType, int classificationRegionId, long unionId) {
    }

    // PlayerLoginLogFlow 玩家登录日志
    public void playerLoginLog(long pid, int last_login, int last_logout, String game_list, String ip, String latitude,
                               String longitude, String address) {
    }

    // playerClubCardLog 玩家圈卡日志
    public void playerClubCardLog(long pid, int num, int cur_remainder, int pre_value, int type, int gameType,
                                  int rechargeType, long roomID, long agentsID, int level, long clubID, int reason, int classificationRegionId) {

    }

    // familyCardChargeLog 代理圈卡记录
    public void familyCardChargeLog(long familyID, long pid, int num, int cur_remainder, int pre_value, int type,
                                    int sourceType, int classificationRegionId) {

    }

    // clubCardWinnerRebateLog 圈卡大赢家返利
    public void clubCardWinnerRebateLog(long roomID, long agentsID, int level, long clubID, String winnerPid,
                                        int clubLevel, int agentsCard, int officialCard, int actualCard, int money, int paymentRoomCardType,
                                        int gameType, int playerNum, int classificationRegionId) {

    }


    public void unionSportsPointProfitLog(long unionId, long clubId, double scorePoint, int sourceType, String roomName, int roomKey, long roomId) {

    }

    public void sportsPointChargeLog(long pid, long clubId, long unionId, int reason, double num, double curRemainder, double preValue, int type, int gameId, int classificationRegionId, long roomId) {

    }

    public void casePointChargeLog(long pid, long clubId, long unionId, double num, double curRemainder, double preValue, double caseSportCurRemainder, double caseSportPreValue, int type) {

    }

    public void promotionMultiChangLog(long pid, long clubId, long exePid, int oldNum, int nowNum, int exeOldNum, int exeNowNum, long doPid, String pidList, String pidListNow, String upPidList, String upPidListNow) {

    }

    public void sportsPointWarningLog(long pid, long clubId, long unionId, long upPid, double sportsPointWarning, double calcSportsPointWarning, long roomId) {
    }

    public void scorePercentChargeLog(long pid, long unionId, long clubId, int curRemainder, int preValue, long exePid) {
    }

    public void scoreDividedIntoChargeLog(long pid, long unionId, long clubId, double curRemainder, double preValue, long exePid) {
    }

    public void luckDrawRecordLog(long pid, String prizeName, int prizeType, int rewardNum, int createTime) {

    }

    public void gameServerMaintainLog(long startServerTime, long stopServerTime, int spacing, int httpPost, int clientPort, String name, int pid) {

    }

    public void unionMatchLog(int rankingId, String name, long pid, int clubSign, String clubName, double sportsPoint, int roundId, long unionId, long clubId) {

    }

    public void clubPromotionActiveReportFormLog(long clubId, long partnerPid, double value, String date_time) {

    }

    public void clubPromotionActiveChargeLog(long pid, long clubId, long unionId, int reason, double num, double cur_remainder, double pre_value, int type, long partnerPid) {
    }

    public void clubPromotionDayActiveChargeLog(long pid, long clubId, long unionId, int reason, double num, double cur_remainder, double pre_value, int type, long partnerPid) {
    }

    public void clubPromotionSumActiveChargeLog(long pid, long clubId, long unionId, int reason, double num, double cur_remainder, double pre_value, int type, long partnerPid) {
    }

    public void clubLevelRoomLog(String date_time, long pid, int winner, int consume, long roomId, long upLevelId, long memberId, int point, double sportsPoint, int setCount, double sportsPointConsume, double roomSportsPointConsume, double roomAvgSportsPointConsume, long clubId, long unionId, double promotionShareValue, long playGamePid) {
    }

    public void clubLevelRoomLogZhongZhi(String date_time, long pid, int winner, int consume, long roomId, long upLevelId, long memberId, int point, double sportsPoint, int setCount, double sportsPointConsume, double roomSportsPointConsume, double roomAvgSportsPointConsume, long clubId, long unionId, double promotionShareValue, long playGamePid) {
    }

    public void clubLevelRoomCountLog(String date_time, int setCount, int winner, int consume, long roomSize, long upLevelId, long memberId, long sumPoint, double sportsPoint, double sportsPointConsume, double roomSportsPointConsume, double roomAvgSportsPointConsume, long clubId, long unionId, double promotionShareValue, int sizePlayer, String shareValue, double scorePoint, double personalSportsPoint) {
    }

    public void clubLevelRoomCountLogZhongZhi(String date_time, int setCount, int winner, int consume, long roomSize, long upLevelId, long memberId, long sumPoint, double sportsPoint, double sportsPointConsume, double roomSportsPointConsume, double roomAvgSportsPointConsume, long clubId, long unionId, double promotionShareValue, int sizePlayer, String shareValue, double scorePoint, double personalSportsPoint) {
    }

    public void sportsPointErrorLog(long unionId, double correctValue, double errorValue) {
    }

    public void clubMemberRemoveLog(long memberId, long pid, long clubId, int reason, int status, int isminister, long exePid, int level, long upLevelId, int creattime, int updatetime, int deletetime) {
    }

    public void roomConfigPrizePoolLog(String date_time, long configId, long roomId, int setCount, int roomSize, int value, long unionId, String roomName, String dataJsonCfg, double prizePool, int gameId, double sportPointIncome, double roomSportsPointConsume) {
    }

    public void roomConfigPrizePoolLogZhongZhi(String date_time, long configId, long roomId, int setCount, int roomSize, int value, long unionId, String roomName, String dataJsonCfg, double prizePool, int gameId, double sportPointIncome, double roomSportsPointConsume) {
    }

    public void playerChangeCityLog(long pid, int pre_city, int cur_city) {

    }

    public void playerExceptionLog(long pid, int exceptionType, String content) {
    }

    public void clubmemberStatusLog(long pid, long clubId, long exePid, long exeClubId, int execType) {

    }

    public void roomPromotionPointLog(long pid, String dateTime, long clubId, long unionId, int reason, double num, double curRemainder, double preValue, int type, int gameId, int classificationRegionId, long roomId, long execPid, String roomName, String msg, String roomKey, long reasonPid) {
    }

    public void roomPromotionPointCountLog(long pid, String dateTime, long clubId, long unionId, int reason, double num, double curRemainder, double preValue, int type, int gameId, int classificationRegionId, long roomId, long execPid, String roomName, String msg, int exexTime, String roomKey) {
    }

    public void sportsPointChangeZhongZhiLog(long pid, long clubId, long unionId, int reason, double num, double curRemainder, double preValue, int type, int gameId, int classificationRegionId, long roomId) {
    }

    public void sportsPointChangeZhongZhiCountLog(long pid, long clubId, long unionId, int reason, double num, double curRemainder, double preValue, int type, int gameId, int classificationRegionId, long roomId) {

    }
}