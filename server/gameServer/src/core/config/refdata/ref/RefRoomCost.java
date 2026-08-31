package core.config.refdata.ref;

import business.global.shareroom.ShareRoom;
import business.global.shareroom.ShareRoomGameType;
import cenum.room.PaymentRoomCardType;
import com.ddm.server.common.data.RefContainer;
import com.ddm.server.common.data.RefField;
import com.ddm.server.websocket.def.ErrorCode;
import core.config.refdata.RefDataMgr;
import core.network.http.proto.SData_Result;
import jsproto.c2s.cclass.GameType;
import jsproto.c2s.cclass.room.BaseCreateRoom;
import jsproto.c2s.cclass.room.BaseRoomConfigure;
import lombok.Data;

/**
 * @author Clark
 */
@Data
public class RefRoomCost extends RefBaseGame {
    // ID潜规则
    @RefField(iskey = true)
    public long id;
    // 游戏类型
    public String GameType;
    // 局数
    public int SetCount;
    // 最小人数
    public int PeopleMin;
    // 最大人数
    public int PeopleMax;
    // 平分付消耗房卡
    public int AaCostCount;
    // 大赢家付消耗房卡
    public int WinCostCount;
    // 房主付消耗房卡
    public int CostCount;
    // 亲友圈平分付消耗圈卡
    public int ClubAaCostCount;
    // 亲友圈大赢家付消耗圈卡
    public int ClubWinCostCount;
    // 亲友圈房主付消耗房卡
    public int ClubCostCount;
    // 联盟盟主付消耗房卡
    public int UnionCostCount;
    // 子游戏标识
    public int Sign;

    @Override
    public boolean Assert() {
        return true;
    }

    @Override
    public boolean AssertAll(RefContainer<?> all) {
        return true;
    }

    @Override
    public long getId() {
        return id;
    }

    /**
     * 最大人数
     *
     * @param gameName 游戏名称
     * @return
     */
    public static int getPeopleMax(String gameName) {
        return RefDataMgr.getAll(RefRoomCost.class).values().stream().filter(k -> k.getGameType().equals(gameName)).map(k -> k.getPeopleMax()).max(Integer::compare).get();
    }


    /**
     * 共享获取卡消耗
     *
     * @param shareRoom 配置
     * @return
     */
    @SuppressWarnings("rawtypes")
    public static SData_Result GetCostShare(ShareRoom shareRoom) {
        // 房间公共配置
        BaseCreateRoom baseRoomCfg = shareRoom.getBaseRoomConfigure().getBaseCreateRoom();
        // 付费方式
        PaymentRoomCardType paymentRoomCardType = PaymentRoomCardType.valueOf(baseRoomCfg.getPaymentRoomCardType());
        ShareRoomGameType shareGameType = shareRoom.getBaseRoomConfigure().getGameType();
        jsproto.c2s.cclass.GameType gameType = new GameType(shareGameType.getId(), shareGameType.getName(), shareGameType.getType());
        // 获取配置ID
        long costID = GetCostId(gameType, baseRoomCfg.getSetCount(),
                baseRoomCfg.getPlayerNum(),
                baseRoomCfg.getPlayerMinNum(),
                baseRoomCfg.getSign());
        // 获取房卡配置
        RefRoomCost roomCost = RefDataMgr.get(RefRoomCost.class, costID);
        if (null == roomCost) {
            return SData_Result.make(ErrorCode.NotEnough_RoomCost_Error,
                    "null == roomCost Update the latest version :{%d}", costID);
        }
        if (baseRoomCfg.getUnionId() > 0L && baseRoomCfg.getClubId() > 0L) {
            // 赛事房卡消耗
            return SData_Result.make(ErrorCode.Success, roomCost.getUnionCostCount());
        } else if (baseRoomCfg.getClubId() <= 0) {
            if (PaymentRoomCardType.PaymentRoomCardType_AutoPay.equals(paymentRoomCardType)) {
                // 平分支付
                return SData_Result.make(ErrorCode.Success, roomCost.getAaCostCount());
            } else if (PaymentRoomCardType.PaymentRoomCardType_HomeOwerPay.equals(paymentRoomCardType)) {
                // 房主支付
                return SData_Result.make(ErrorCode.Success, roomCost.getCostCount());
            } else if (PaymentRoomCardType.PaymentRoomCardType_WinnerPay.equals(paymentRoomCardType)) {
                // 大赢家付
                return SData_Result.make(ErrorCode.Success, roomCost.getWinCostCount());
            }
        } else {
            if (PaymentRoomCardType.PaymentRoomCardType_AutoPay.equals(paymentRoomCardType)) {
                return SData_Result.make(ErrorCode.Success, roomCost.getClubAaCostCount());
            } else if (PaymentRoomCardType.PaymentRoomCardType_HomeOwerPay.equals(paymentRoomCardType)) {
                // 房主支付
                return SData_Result.make(ErrorCode.Success, roomCost.getClubCostCount());
            } else if (PaymentRoomCardType.PaymentRoomCardType_WinnerPay.equals(paymentRoomCardType)) {
                return SData_Result.make(ErrorCode.Success, roomCost.getClubWinCostCount());
            }
        }
        return SData_Result.make(ErrorCode.NotEnough_RoomCost_Error, "Update the latest version :{%d}", costID);
    }

    /**
     * 获取卡消耗
     *
     * @param baseRoomConfigure 配置
     * @return
     */
    @SuppressWarnings("rawtypes")
    public static SData_Result GetCost(BaseRoomConfigure baseRoomConfigure) {
        // 房间公共配置
        BaseCreateRoom baseRoomCfg = baseRoomConfigure.getBaseCreateRoom();
        // 付费方式
        PaymentRoomCardType paymentRoomCardType = PaymentRoomCardType.valueOf(baseRoomCfg.getPaymentRoomCardType());
        // 获取配置ID
        long costID = GetCostId(baseRoomConfigure.getGameType(), baseRoomCfg.getSetCount(),
                baseRoomConfigure.getBaseCreateRoom().getPlayerNum(),
                baseRoomConfigure.getBaseCreateRoom().getPlayerMinNum(),
                baseRoomConfigure.getBaseCreateRoom().getSign());
        // 获取房卡配置
        RefRoomCost roomCost = RefDataMgr.get(RefRoomCost.class, costID);
        if (null == roomCost) {
            return SData_Result.make(ErrorCode.NotEnough_RoomCost_Error,
                    "null == roomCost Update the latest version :{%d}", costID);
        }
        if (baseRoomCfg.getUnionId() > 0L && baseRoomCfg.getClubId() > 0L) {
            // 赛事房卡消耗
            return SData_Result.make(ErrorCode.Success, roomCost.getUnionCostCount());
        } else if (baseRoomCfg.getClubId() <= 0) {
            if (PaymentRoomCardType.PaymentRoomCardType_AutoPay.equals(paymentRoomCardType)) {
                // 平分支付
                return SData_Result.make(ErrorCode.Success, roomCost.getAaCostCount());
            } else if (PaymentRoomCardType.PaymentRoomCardType_HomeOwerPay.equals(paymentRoomCardType)) {
                // 房主支付
                return SData_Result.make(ErrorCode.Success, roomCost.getCostCount());
            } else if (PaymentRoomCardType.PaymentRoomCardType_WinnerPay.equals(paymentRoomCardType)) {
                // 大赢家付
                return SData_Result.make(ErrorCode.Success, roomCost.getWinCostCount());
            }
        } else {
            if (PaymentRoomCardType.PaymentRoomCardType_AutoPay.equals(paymentRoomCardType)) {
                return SData_Result.make(ErrorCode.Success, roomCost.getClubAaCostCount());
            } else if (PaymentRoomCardType.PaymentRoomCardType_HomeOwerPay.equals(paymentRoomCardType)) {
                // 房主支付
                return SData_Result.make(ErrorCode.Success, roomCost.getClubCostCount());
            } else if (PaymentRoomCardType.PaymentRoomCardType_WinnerPay.equals(paymentRoomCardType)) {
                return SData_Result.make(ErrorCode.Success, roomCost.getClubWinCostCount());
            }
        }
        return SData_Result.make(ErrorCode.NotEnough_RoomCost_Error, "Update the latest version :{%d}", costID);
    }


    /**
     * 计算ID规则10001 004 10001004
     *
     * @param gameType 游戏类型
     * @param setCount 局数
     * @return
     */
    private static long GetCostId(jsproto.c2s.cclass.GameType gameType, int setCount, int peopleMax, int peopleMin, int sign) {
        return Long.parseLong(String.format("0%d%04d%03d%d%02d", sign <= 0 ? 1 : sign,
                (gameType.getId() + 1), setCount, peopleMax == peopleMin ? 0 : peopleMin, peopleMax));
    }
}
