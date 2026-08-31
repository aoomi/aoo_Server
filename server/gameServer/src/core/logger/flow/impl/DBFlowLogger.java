package core.logger.flow.impl;

import core.db.entity.clarkLog.*;
import core.db.other.DBFlowMgr;
import core.logger.flow.GameFlowLogger;

public class DBFlowLogger extends GameFlowLogger {

    @Override
    public boolean isOpen() {
        return true;
    }


    // Coin Charge 金币消费日志
    @Override
    public void goldChargeLog(long pid, int reason, int num, int cur_remainder, int pre_value,
                              int type, int classificationRegionId) {
        DBFlowMgr.getInstance()
                .add(new GoldChargeLogFlow(pid, reason, num, cur_remainder, pre_value, type, classificationRegionId));
    }

    // Player Online 在线人数
    @Override
    public void playerOnLineChargeLog(int onlineCount) {
        DBFlowMgr.getInstance().add(new PlayerOnLineChargeLogFlow(onlineCount));
    }

    // Room Card 房卡消费日志
    @Override
    public void roomCardChargeLog(long pid, int reason, int num, int cur_remainder, int pre_value, int type,
                                  long familyID, int gameType, int rechargeType, int classificationRegionId) {
        DBFlowMgr.getInstance().add(new RoomCardChargeLogFlow(pid, reason, num, cur_remainder, pre_value, type,
                familyID, gameType, rechargeType, classificationRegionId));
    }

    // Room Card 房卡消费日志
    @Override
    public void clubRoomCardChargeLog(long pid, long clubID, int status, int reason, int num, int cur_remainder,
                                      int pre_value, int type, int gameType, int rechargeType, long agentsID, int level, int classificationRegionId) {
        DBFlowMgr.getInstance().add(new ClubRoomCardChargeLogFlow(pid, clubID, status, reason, num, cur_remainder,
                pre_value, type, gameType, rechargeType, agentsID, level, classificationRegionId));
    }

    // Room Card赛事 房卡消费日志
    @Override
    public void unionRoomCardChargeLog(long pid, long unionId, int status, int reason, int num, int cur_remainder,
                                       int pre_value, int type, int gameType, int rechargeType, long agentsID, int level, int classificationRegionId) {
        DBFlowMgr.getInstance().add(new UnionRoomCardChargeLogFlow(pid, unionId, status, reason, num, cur_remainder,
                pre_value, type, gameType, rechargeType, agentsID, level, classificationRegionId));
    }

    // PlayerDataLogFlow 玩家数据日志
    @Override
    public void playerDataLog(long pid, long accountId, long reg_time, int trial_mark, int sign, int classificationRegionId) {
        DBFlowMgr.getInstance().add(new PlayerDataLogFlow(pid, accountId, reg_time, trial_mark, sign, classificationRegionId));
    }

    // PlayerRoomLog 玩家房间日志
    @Override
    public void playerRoomLog(String date_time, long ownner_id, long room_id, int set_count, String player_list, int sum_count, long club_id, String room_key, int room_card, int createRoomTime, int clubCostType, int gameType, int classificationRegionId, long unionId) {
        DBFlowMgr.getInstance().add(new PlayerRoomLogFlow(date_time, ownner_id, room_id, set_count, player_list, sum_count,
                club_id, room_key, room_card, createRoomTime, clubCostType, gameType, classificationRegionId, unionId));
    }

    // playerLoginLog 玩家登录日志
    @Override
    public void playerLoginLog(long pid, int last_login, int last_logout, String game_list, String ip, String latitude,
                               String longitude, String address) {
        DBFlowMgr.getInstance()
                .add(new PlayerLoginLogFlow(pid, last_login, last_logout, game_list, ip, latitude, longitude, address));
    }

    // playerClubCardLog 玩家圈卡日志
    @Override
    public void playerClubCardLog(long pid, int num, int cur_remainder, int pre_value, int type, int gameType,
                                  int rechargeType, long roomID, long agentsID, int level, long clubID, int reason, int classificationRegionId) {
        DBFlowMgr.getInstance().add(new PlayerClubCardLogFlow(pid, num, cur_remainder, pre_value, type, gameType,
                rechargeType, roomID, agentsID, level, clubID, reason, classificationRegionId));

    }

    // familyCardChargeLog 玩家代理圈卡日志
    @Override
    public void familyCardChargeLog(long familyID, long pid, int num, int cur_remainder, int pre_value, int type,
                                    int sourceType, int classificationRegionId) {
        DBFlowMgr.getInstance()
                .add(new FamilyCardChargeLogFlow(familyID, pid, num, cur_remainder, pre_value, type, sourceType, classificationRegionId));
    }

    // clubCardWinnerRebateLog 圈卡大赢家返利
    @Override
    public void clubCardWinnerRebateLog(long roomID, long agentsID, int level, long clubID, String winnerPid,
                                        int clubLevel, int agentsCard, int officialCard, int actualCard, int money, int paymentRoomCardType,
                                        int gameType, int playerNum, int classificationRegionId) {
        DBFlowMgr.getInstance().add(new ClubCardWinnerRebateLogFlow(roomID, agentsID, level, clubID, winnerPid,
                clubLevel, agentsCard, officialCard, actualCard, money, paymentRoomCardType, gameType, playerNum, classificationRegionId));
    }


    @Override
    public void unionSportsPointProfitLog(long unionId, long clubId, double scorePoint, int sourceType, String roomName, int roomKey, long roomId) {
        DBFlowMgr.getInstance().add(new UnionSportsPointProfitLogFlow(unionId, clubId, scorePoint, sourceType, roomName, roomKey, roomId));
    }

    @Override
    public void sportsPointChargeLog(long pid, long clubId, long unionId, int reason, double num, double curRemainder, double preValue, int type, int gameId, int classificationRegionId, long roomId) {
        DBFlowMgr.getInstance().add(new SportsPointChargeLogFlow(pid, clubId, unionId, reason, num, curRemainder, preValue, type, gameId, classificationRegionId, roomId));
    }

    @Override
    public void casePointChargeLog(long pid, long clubId, long unionId, double num, double curRemainder, double preValue, double caseSportCurRemainder, double caseSportPreValue, int type) {
        DBFlowMgr.getInstance().add(new CasePointChargeLogFlow(pid, clubId, unionId, num, curRemainder, preValue, caseSportCurRemainder, caseSportPreValue, type));
    }

    @Override
    public void promotionMultiChangLog(long pid, long clubId, long exePid, int oldNum, int nowNum, int exeOldNum, int exeNowNum, long doPid, String pidList, String pidListNow, String upPidList, String upPidListNow) {
        DBFlowMgr.getInstance().add(new PromotionMultiChangLogFlow(pid, clubId, exePid, oldNum, nowNum, exeOldNum, exeNowNum, doPid, pidList, pidListNow, upPidList, upPidListNow));
    }

    @Override
    public void sportsPointWarningLog(long pid, long clubId, long unionId, long upPid, double sportsPointWarning, double calcSportsPointWarning, long roomId) {
        DBFlowMgr.getInstance().add(new SportsPointWarningLogFlow(pid, clubId, unionId, upPid, sportsPointWarning, calcSportsPointWarning, roomId));
    }

    @Override
    public void scorePercentChargeLog(long pid, long unionId, long clubId, int curRemainder, int preValue, long exePid) {
        DBFlowMgr.getInstance().add(new ScorePercentChargeLogFlow(pid, unionId, clubId, curRemainder, preValue, exePid));
    }

    @Override
    public void scoreDividedIntoChargeLog(long pid, long unionId, long clubId, double curRemainder, double preValue, long exePid) {
        DBFlowMgr.getInstance().add(new ScoreDividedIntoChargeLogFlow(pid, unionId, clubId, curRemainder, preValue, exePid));
    }

    @Override
    public void luckDrawRecordLog(long pid, String prizeName, int prizeType, int rewardNum, int createTime) {
        DBFlowMgr.getInstance().add(new LuckDrawRecordLogFlow(pid, prizeName, prizeType, rewardNum, createTime));
    }

    @Override
    public void gameServerMaintainLog(long startServerTime, long stopServerTime, int spacing, int httpPost, int clientPort, String name, int pid) {
        DBFlowMgr.getInstance().add(new GameServerMaintainLogFlow(startServerTime, stopServerTime, spacing, httpPost, clientPort, name, pid));
    }

    @Override
    public void unionMatchLog(int rankingId, String name, long pid, int clubSign, String clubName, double sportsPoint, int roundId, long unionId, long clubId) {
        DBFlowMgr.getInstance().add(new UnionMatchLogFlow(rankingId, name, pid, clubSign, clubName, sportsPoint, roundId, unionId, clubId));
    }


    @Override
    public void clubPromotionActiveReportFormLog(long clubId, long partnerPid, double value, String date_time) {
        DBFlowMgr.getInstance().add(new ClubPromotionActiveReportFormLogFlow(clubId, partnerPid, value, date_time));
    }


    @Override
    public void clubPromotionActiveChargeLog(long pid, long clubId, long unionId, int reason, double num, double cur_remainder, double pre_value, int type, long partnerPid) {
        DBFlowMgr.getInstance().add(new ClubPromotionActiveChargeLogFlow(pid, clubId, unionId, reason, num, cur_remainder, pre_value, type, partnerPid));
    }

    @Override
    public void clubPromotionDayActiveChargeLog(long pid, long clubId, long unionId, int reason, double num, double cur_remainder, double pre_value, int type, long partnerPid) {
        DBFlowMgr.getInstance().add(new ClubPromotionDayActiveChargeLogFlow(pid, clubId, unionId, reason, num, cur_remainder, pre_value, type, partnerPid));
    }

    @Override
    public void clubPromotionSumActiveChargeLog(long pid, long clubId, long unionId, int reason, double num, double cur_remainder, double pre_value, int type, long partnerPid) {
        DBFlowMgr.getInstance().add(new ClubPromotionSumActiveChargeLogFlow(pid, clubId, unionId, reason, num, cur_remainder, pre_value, type, partnerPid));
    }


    @Override
    public void clubLevelRoomLog(String date_time, long pid, int winner, int consume, long roomId, long upLevelId, long memberId, int point, double sportsPoint, int setCount, double sportsPointConsume, double roomSportsPointConsume, double roomAvgSportsPointConsume, long clubId, long unionId, double promotionShareValue, long playGamePid) {
        DBFlowMgr.getInstance().add(new ClubLevelRoomLogFlow(date_time, pid, winner, consume, roomId, upLevelId, memberId, point, sportsPoint, setCount, sportsPointConsume, roomSportsPointConsume, roomAvgSportsPointConsume, clubId, unionId, promotionShareValue, playGamePid));
    }

    @Override
    public void clubLevelRoomLogZhongZhi(String date_time, long pid, int winner, int consume, long roomId, long upLevelId, long memberId, int point, double sportsPoint, int setCount, double sportsPointConsume, double roomSportsPointConsume, double roomAvgSportsPointConsume, long clubId, long unionId, double promotionShareValue, long playGamePid) {
        DBFlowMgr.getInstance().add(new ClubLevelRoomLogZhongZhiFlow(date_time, pid, winner, consume, roomId, upLevelId, memberId, point, sportsPoint, setCount, sportsPointConsume, roomSportsPointConsume, roomAvgSportsPointConsume, clubId, unionId, promotionShareValue, playGamePid));
    }

    @Override
    public void clubLevelRoomCountLog(String date_time, int setCount, int winner, int consume, long roomSize, long upLevelId, long memberId, long sumPoint, double sportsPoint, double sportsPointConsume, double roomSportsPointConsume, double roomAvgSportsPointConsume, long clubId, long unionId, double promotionShareValue, int sizePlayer, String shareValue, double scorePoint, double personalSportsPoint) {
        DBFlowMgr.getInstance().add(new ClubLevelRoomCountLogFlow(date_time, setCount, winner, consume, roomSize, upLevelId, memberId, sumPoint, sportsPoint, sportsPointConsume, roomSportsPointConsume, roomAvgSportsPointConsume, clubId, unionId, promotionShareValue, sizePlayer, shareValue, scorePoint, personalSportsPoint));
    }

    @Override
    public void clubLevelRoomCountLogZhongZhi(String date_time, int setCount, int winner, int consume, long roomSize, long upLevelId, long memberId, long sumPoint, double sportsPoint, double sportsPointConsume, double roomSportsPointConsume, double roomAvgSportsPointConsume, long clubId, long unionId, double promotionShareValue, int sizePlayer, String shareValue, double scorePoint, double personalSportsPoint) {
        DBFlowMgr.getInstance().add(new ClubLevelRoomCountLogZhongZhiFlow(date_time, setCount, winner, consume, roomSize, upLevelId, memberId, sumPoint, sportsPoint, sportsPointConsume, roomSportsPointConsume, roomAvgSportsPointConsume, clubId, unionId, promotionShareValue, sizePlayer, shareValue, scorePoint, personalSportsPoint));
    }


    @Override
    public void sportsPointErrorLog(long unionId, double correctValue, double errorValue) {
        DBFlowMgr.getInstance().add(new SportsPointErrorLogFlow(unionId, correctValue, errorValue));
    }


    @Override
    public void clubMemberRemoveLog(long memberId, long pid, long clubId, int reason, int status, int isminister, long exePid, int level, long upLevelId, int creattime, int updatetime, int deletetime) {
        DBFlowMgr.getInstance().add(new ClubMemberRemoveLogFlow(memberId, pid, clubId, reason, status, isminister, exePid, level, upLevelId, creattime, updatetime, deletetime));
    }

    @Override
    public void roomConfigPrizePoolLog(String date_time, long configId, long roomId, int setCount, int roomSize, int value, long unionId, String roomName, String dataJsonCfg, double prizePool, int gameId, double sportPointIncome, double roomSportsPointConsume) {
        DBFlowMgr.getInstance().add(new RoomConfigPrizePoolLogFlow(date_time, configId, roomId, setCount, roomSize, value, unionId, roomName, dataJsonCfg, prizePool, gameId, sportPointIncome, roomSportsPointConsume));
    }

    @Override
    public void roomConfigPrizePoolLogZhongZhi(String date_time, long configId, long roomId, int setCount, int roomSize, int value, long unionId, String roomName, String dataJsonCfg, double prizePool, int gameId, double sportPointIncome, double roomSportsPointConsume) {
        DBFlowMgr.getInstance().add(new RoomConfigPrizePoolLogZhongZhiFlow(date_time, configId, roomId, setCount, roomSize, value, unionId, roomName, dataJsonCfg, prizePool, gameId, sportPointIncome, roomSportsPointConsume));
    }

    @Override
    public void playerChangeCityLog(long pid, int pre_city, int cur_city) {
        DBFlowMgr.getInstance().add(new PlayerChangeCityLogFlow(pid, pre_city, cur_city));
    }

    @Override
    public void playerExceptionLog(long pid, int exceptionType, String content) {
        DBFlowMgr.getInstance().add(new PlayerExceptionLogFlow(pid, exceptionType, content));
    }

    @Override
    public void clubmemberStatusLog(long pid, long clubId, long exePid, long exeClubId, int execType) {
        DBFlowMgr.getInstance().add(new ClubmemberStatusLogFlow(pid, clubId, exePid, exeClubId, execType));
    }

    @Override
    public void roomPromotionPointLog(long pid, String dateTime, long clubId, long unionId, int reason, double num, double curRemainder, double preValue, int type, int gameId, int classificationRegionId, long roomId, long execPid, String roomName, String msg, String roomKey, long reasonPid) {
        DBFlowMgr.getInstance().add(new RoomPromotionPointLogFlow(pid, dateTime, clubId, unionId, reason, num, curRemainder, preValue, type, gameId, classificationRegionId, roomId, execPid, roomName, msg, roomKey, reasonPid));
    }

    @Override
    public void roomPromotionPointCountLog(long pid, String dateTime, long clubId, long unionId, int reason, double num, double curRemainder, double preValue, int type, int gameId, int classificationRegionId, long roomId, long execPid, String roomName, String msg, int execTime, String roomKey) {
        DBFlowMgr.getInstance().add(new RoomPromotionPointCountLogFlow(pid, dateTime, clubId, unionId, reason, num, curRemainder, preValue, type, gameId, classificationRegionId, roomId, execPid, roomName, msg, execTime, roomKey));
    }

    @Override
    public void sportsPointChangeZhongZhiLog(long pid, long clubId, long unionId, int reason, double num, double curRemainder, double preValue, int type, int gameId, int classificationRegionId, long roomId) {
        DBFlowMgr.getInstance().add(new SportsPointChangeZhongZhiLogFlow(pid, clubId, unionId, reason, num, curRemainder, preValue, type, gameId, classificationRegionId, roomId));
    }

    @Override
    public void sportsPointChangeZhongZhiCountLog(long pid, long clubId, long unionId, int reason, double num, double curRemainder, double preValue, int type, int gameId, int classificationRegionId, long roomId) {
        DBFlowMgr.getInstance().add(new SportsPointChangeZhongZhiCountLogFlow(pid, clubId, unionId, reason, num, curRemainder, preValue, type, gameId, classificationRegionId, roomId));
    }
}