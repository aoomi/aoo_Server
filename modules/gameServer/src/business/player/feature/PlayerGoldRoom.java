package business.player.feature;

import business.global.room.GoldRoomMgr;
import business.global.room.RoomMgr;
import business.global.room.base.AbsBaseRoom;
import business.player.Player;
import com.ddm.server.websocket.def.ErrorCode;
import core.network.http.proto.SData_Result;
import jsproto.c2s.cclass.GameType;
import jsproto.c2s.cclass.room.BaseRoomConfigure;
import jsproto.c2s.iclass.room.SRoom_EnterRoom;
import lombok.Data;

import java.util.Objects;

/**
 * 不是你的模块，请咨询作者，弄清楚逻辑再动
 *
 * @date 2016年1月21日
 */
@Data
public class PlayerGoldRoom extends Feature {

    public PlayerGoldRoom(Player data) {
        super(data);
    }

    @Override
    public void loadDB() {
    }

    /**
     * 练习场ID
     */
    private long practiceId = -1;


    /**
     * 创建房间并查询房间。
     *
     * @param baseRoomConfigure 公共配置
     * @return
     */
    @SuppressWarnings("rawtypes")
    public SData_Result createAndQuery(BaseRoomConfigure baseRoomConfigure) {
        this.initBaseRoomConfigure(baseRoomConfigure);
        // 检查玩家身上是否存在房间
        SData_Result result = this.checkExistRoom(baseRoomConfigure.getGameType());
        if (ErrorCode.Success.equals(result.getCode()) || ErrorCode.Exist_OtherRoom.equals(result.getCode())) {
            return result;
        }
        // 检查房卡配置和玩家房卡
        result = this.checkRefRoomCost(baseRoomConfigure);
        if (!ErrorCode.Success.equals(result.getCode())) {
            return result;
        }
        // 查询存在空位置的房间。
        result = GoldRoomMgr.getInstance().queryExistEmptyPos(baseRoomConfigure.getGameType(), this.getPid(), baseRoomConfigure.getRobotRoomCfg().getPracticeId());
        if (!ErrorCode.Success.equals(result.getCode())) {
            // 没有空房间，创建房间
            result = GoldRoomMgr.getInstance().createRoom(baseRoomConfigure, this.getPid());
            if (ErrorCode.Success.equals(result.getCode())) {
                this.setRoomID(((SRoom_EnterRoom) result.getData()).getRoomID());
                this.setPracticeId(baseRoomConfigure.getRobotRoomCfg().getPracticeId());
            }
            return result;
        }
        // 设置玩家。
        this.setRoomID(((SRoom_EnterRoom) result.getData()).getRoomID());
        this.setPracticeId(baseRoomConfigure.getRobotRoomCfg().getPracticeId());
        return result;
    }

    /**
     * 初始配置
     *
     * @param baseRoomConfigure 公共
     */
    private void initBaseRoomConfigure(BaseRoomConfigure baseRoomConfigure) {
        baseRoomConfigure.getBaseCreateRoom().setRoomName(null);
        baseRoomConfigure.getBaseCreateRoom().setIsClubMemberCreateRoom(null);
        baseRoomConfigure.getBaseCreateRoom().setClubWinnerPayConsume(null);
        baseRoomConfigure.getBaseCreateRoom().setClubCostType(null);
        baseRoomConfigure.getBaseCreateRoom().setRoomName(null);
        baseRoomConfigure.getBaseCreateRoom().setSportsDouble(null);
        baseRoomConfigure.getBaseCreateRoom().setAutoDismiss(null);
        baseRoomConfigure.getBaseCreateRoom().setRoomSportsThreshold(null);
        baseRoomConfigure.getBaseCreateRoom().setRoomSportsType(null);
        baseRoomConfigure.getBaseCreateRoom().setRoomSportsEveryoneConsume(null);
        baseRoomConfigure.getBaseCreateRoom().setRoomSportsBigWinnerConsume(null);
        baseRoomConfigure.getBaseCreateRoom().setPrizePool(null);
        baseRoomConfigure.getBaseCreateRoom().setPassword(null);
        baseRoomConfigure.setClubRoomCfg(null);
        baseRoomConfigure.setUnionRoomConfig(null);
        baseRoomConfigure.setArenaRoomCfg(null);
    }


    /**
     * 检查玩家身上是否存在房间
     *
     * @return
     */
    @SuppressWarnings("rawtypes")
    private SData_Result checkExistRoom(GameType gameType) {
        if (this.getRoomID() <= 0L) {
            return SData_Result.make(ErrorCode.Not);
        }
        AbsBaseRoom room = RoomMgr.getInstance().getRoom(this.getRoomID());
        if (Objects.isNull(room)) {
            this.setRoomID(0);
            return SData_Result.make(ErrorCode.Not);
        }
        if (gameType.getId() == room.getBaseRoomConfigure().getGameType().getId()) {
            return SData_Result.make(ErrorCode.Success, room.getEnterRoomInfo());
        } else {
            return SData_Result.make(ErrorCode.Exist_OtherRoom, gameType.getName());
        }
    }


    /**
     * 检查房卡配置和玩家房卡
     *
     * @return
     */
    @SuppressWarnings({"rawtypes"})
    private SData_Result checkRefRoomCost(BaseRoomConfigure baseRoomConfigure) {
        ErrorCode errorCode = this.player.getFeature(PlayerCurrency.class).checkGold(baseRoomConfigure.getRobotRoomCfg().getMin(), baseRoomConfigure.getRobotRoomCfg().getMax());
        // 检查玩家的金币，是否满足条件。
        if (!ErrorCode.Success.equals(errorCode)) {
            return SData_Result.make(errorCode, "Check player the player's gold coins meet the requirements errorCode:{%s}", errorCode);
        }
        return SData_Result.make(ErrorCode.Success);
    }


}