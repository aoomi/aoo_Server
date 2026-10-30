package business.player.feature;

import business.global.room.PlayBackMgr;
import business.player.Player;
import cenum.Page;
import cenum.PrizeType;
import com.ddm.server.common.utils.CommFile;
import com.ddm.server.common.utils.CommTime;
import com.ddm.server.websocket.def.ErrorCode;
import com.google.gson.Gson;
import core.config.server.GameTypeMgr;
import core.db.entity.clarkGame.PlayerPlayBackBO;
import core.db.other.Criteria;
import core.db.other.Restrictions;
import core.db.service.clarkGame.GameRoomBOService;
import core.db.service.clarkGame.GameSetBOService;
import core.db.service.clarkGame.PlayerRoomAloneBOService;
import core.ioc.ContainerMgr;
import core.network.http.proto.SData_Result;
import jsproto.c2s.cclass.GameType;
import jsproto.c2s.cclass.PlayerRoomRecord;
import jsproto.c2s.cclass.PlayerSetRoomRecord;
import jsproto.c2s.cclass.club.ClubPlayerRoomAloneLogPidBO;
import jsproto.c2s.iclass.*;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 不是你的模块，请咨询作者，弄清楚逻辑再动
 * 玩家战绩和回放记录
 *
 * @date 2016年1月21日
 */
public class PlayerRecord extends Feature {

    @Override
    public void loadDB() {
    }

    public PlayerRecord(Player player) {
        super(player);
    }


    /**
     * 玩家房间战绩
     *
     * @param pageNum
     */
    public SData_Result<?> playerRoomRecord(int pageNum, long clubID, int getType, int dese) {
        int page = Page.getPageNum(pageNum, Page.PAGE_SIZE_10);
        Criteria criteria = Restrictions.and(Restrictions.eq("pid", this.player.getPid()), Restrictions.eq("clubID", clubID), Restrictions.eq("dateTime", CommTime.getYesterDayStringYMD(getType)));
        criteria.setPageNum(page);
        criteria.setPageSize(Page.PAGE_SIZE_10);
        if (dese == 1) {
            criteria.desc("endTime");
        } else {
            criteria.asc("endTime");
        }
        return roomRecordList(criteria, page, dese);
    }


    /**
     * 玩家房间战绩
     *
     * @param gameType
     * @param pageNum
     */
    public SData_Result<?> playerRoomRecord(int gameType, int pageNum, long clubID) {
        int page = Page.getPageNum(pageNum, Page.PAGE_SIZE_10);
        Criteria prizeTypeCriteria = Restrictions.and(Restrictions.ge("prizeType", PrizeType.RoomCard.value()), Restrictions.le("prizeType", PrizeType.ClubCard.value()));
        Criteria criteria = Restrictions.and(Restrictions.eq("pid", this.player.getPid()), clubID <= 0L ? null : Restrictions.eq("clubID", clubID), prizeTypeCriteria, gameType >= 0 ? Restrictions.eq("gameType", gameType) : null);
        criteria.setPageNum(page);
        criteria.setPageSize(Page.PAGE_SIZE_10);
        criteria.desc("id");
        return roomRecordList(criteria, page, 1);
    }

    private SData_Result roomRecordList(Criteria criteria, int page, int dese) {
        List<Long> roomList = ContainerMgr.get().getComponent(PlayerRoomAloneBOService.class)
                .findAllE(criteria, ClubPlayerRoomAloneLogPidBO.class, ClubPlayerRoomAloneLogPidBO.getItemsName())
                .stream().filter(k -> Objects.nonNull(k)).map(k -> k.getRoomID()).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(roomList)) {
            if (page <= 0) {
                return SData_Result.make(ErrorCode.RECORD_NOT_EXIST, "RECORD_NOT_EXIST roomList");
            } else {
                return SData_Result.make(ErrorCode.Success, SPlayer_RoomRecord.make(Collections.emptyList()));
            }
        }

        // 查询 endTime > 0 and id RoomID;
        criteria = Restrictions.and(Restrictions.in("id", roomList), Restrictions.gt("endTime", 0), Restrictions.isNotNull("roomKey"), Restrictions.isNotNull("playerList"));
        if (dese == 1) {
            criteria.desc("endTime");
        } else {
            criteria.asc("endTime");
        }
        List<PlayerRoomRecord> playerRoomRecords = ContainerMgr.get().getComponent(GameRoomBOService.class).findAllE(criteria, PlayerRoomRecord.class, PlayerRoomRecord.getItemsName());
        if (CollectionUtils.isEmpty(playerRoomRecords)) {
            if (page <= 0) {
                return SData_Result.make(ErrorCode.RECORD_NOT_EXIST, "RECORD_NOT_EXIST playerRoomRecords");
            } else {
                return SData_Result.make(ErrorCode.Success, SPlayer_RoomRecord.make(Collections.emptyList()));
            }
        }
        playerRoomRecords = playerRoomRecords.stream().filter(k -> Objects.nonNull(k)).collect(Collectors.toList());
        return SData_Result.make(ErrorCode.Success, SPlayer_RoomRecord.make(playerRoomRecords));
    }

    /**
     * 玩家房间战绩
     *
     * @param clubID
     * @param gameType
     * @param pageNum
     */
    public SData_Result<?> playerRoomRecord(long clubID, int gameType, int pageNum) {
        Criteria prizeTypeCriteria = Restrictions.and(Restrictions.ge("prizeType", PrizeType.RoomCard.value()), Restrictions.le("prizeType", PrizeType.ClubCard.value()));
        Criteria criteria = Restrictions.and(Restrictions.eq("clubID", clubID), prizeTypeCriteria, gameType >= 0 ? Restrictions.eq("gameType", gameType) : null, Restrictions.gt("endTime", 0), Restrictions.isNotNull("roomKey"), Restrictions.isNotNull("playerList"));
        criteria.setPageNum(Page.getPageNum(pageNum, Page.PAGE_SIZE_10));
        criteria.setPageSize(Page.PAGE_SIZE_10);
        criteria.desc("id");
        List<PlayerRoomRecord> playerRoomRecords = ContainerMgr.get().getComponent(GameRoomBOService.class).findAllE(criteria, PlayerRoomRecord.class, PlayerRoomRecord.getItemsName());
        if (CollectionUtils.isEmpty(playerRoomRecords)) {
            return SData_Result.make(ErrorCode.NotAllow, "playerRoomRecord null == playerRoomRecords || playerRoomRecords.size() <=0");
        }
        playerRoomRecords = playerRoomRecords.stream().filter(k -> Objects.nonNull(k)).collect(Collectors.toList());
        return SData_Result.make(ErrorCode.Success, SPlayer_RoomRecord.make(playerRoomRecords));
    }

    /**
     * 玩家每局的战绩
     *
     * @param roomId
     */
    public SData_Result<?> playerSetRoomRecord(long roomId) {
        // roomId is client-controlled. Only a participant may request the
        // detailed per-round result; otherwise sequential IDs expose other
        // rooms' player lists and scores.
        if (ContainerMgr.get().getComponent(PlayerRoomAloneBOService.class).findOne(
                Restrictions.and(Restrictions.eq("roomID", roomId),
                        Restrictions.eq("pid", this.player.getPid())), "id") == null) {
            return SData_Result.make(ErrorCode.NotAllow, "player has no access to room record");
        }
        List<PlayerSetRoomRecord> playerSetRoomRecords = ContainerMgr.get().getComponent(GameSetBOService.class).findAllE(Restrictions.eq("roomID", roomId), PlayerSetRoomRecord.class, PlayerSetRoomRecord.getItemsName());
        if (CollectionUtils.isEmpty(playerSetRoomRecords)) {
            return SData_Result.make(ErrorCode.NotAllow, "playerSetRoomRecord null == playerSetRoomRecords || playerSetRoomRecords.size() <= 0");
        }
        playerSetRoomRecords.stream().filter(k -> Objects.nonNull(k) && k.getEndTime() > 0 && StringUtils.isNotEmpty(k.getDataJsonRes())).map(k -> {
            if (CommTime.DaysBetween(k.getEndTime()) > 7) {
                k.setPlaybackCode(0);
            }
            return k;
        }).collect(Collectors.toList());
        return SData_Result.make(ErrorCode.Success, SPlayer_SetRoomRecord.make(playerSetRoomRecords));
    }


    /**
     * 检查回放码是否存在，返回游戏类型
     *
     * @param playBackCode 回放码
     * @return
     */
    private SData_Result chekcPlayBackCodeResult(int playBackCode) {

        String hand = CommFile.getHandFromFile(PlayBackMgr.getInstance().getFile(playBackCode));
        if (StringUtils.isEmpty(hand) || !StringUtils.isNumeric(hand)) {
            return SData_Result.make(ErrorCode.PlayBack_Error, "error playBackCode");
        }
        return SData_Result.make(ErrorCode.Success, GameTypeMgr.getInstance().gameType(Integer.parseInt(hand)));
    }

    /**
     * 最大的数据包长度
     */
    private final static int MAX_DATA_LENGTH = 10000;

    /**
     * 玩家回放记录
     */
    public SData_Result playerPlayBack(int playBackCode, boolean chekcPlayBackCode) {
        if (playBackCode <= 0) {
            return SData_Result.make(ErrorCode.PlayBack_Error, "playBackCode:{%d}", playBackCode);
        }
        if (chekcPlayBackCode) {
            return this.chekcPlayBackCodeResult(playBackCode);
        }
        String content = CommFile.getContentFromFile(PlayBackMgr.getInstance().getFile(playBackCode));
        if (StringUtils.isEmpty(content)) {
            return SData_Result.make(ErrorCode.PlayBack_Error, "content Empty");
        }
        PlayerPlayBackBO playBackBO = new Gson().fromJson(content, PlayerPlayBackBO.class);

        StringBuilder playBack = new StringBuilder(playBackBO.getPlayBackRes().toString());
        if (Objects.nonNull(playBack)) {
            // 获取最大长度
            int maxLength = playBack.length();
            // 可拆得包数量
            int number = (maxLength / MAX_DATA_LENGTH);
            // 共下发多少包
            int playBackNum = maxLength % MAX_DATA_LENGTH == 0 ? number : number + 1;
            subPlayBack(0, 0, maxLength, playBack, playBackNum);
        }
        return SData_Result.make(ErrorCode.Success, SPlayer_PlayBack.make(playBackBO.getRoomID(), playBackBO.getSetID(), playBackBO.getEndTime(), playBackBO.getDPos(), playBackBO.getPlayerList(), playBackBO.getSetCount(), playBackBO.getRoomKey(), playBackBO.getGameType(), playBackBO.getSetID()));
    }


    /**
     * 截取 回放数据分段
     *
     * @param idx
     * @param start
     * @param maxLength
     * @param playBack
     */
    public void subPlayBack(int idx, int start, int maxLength, StringBuilder playBack, int playBackNum) {
        int end = 0;
        end = start + MAX_DATA_LENGTH;
        if (end < maxLength) {
            String msg = playBack.substring(start, end);
            player.pushProto(SPlayer_PlayBackData.make(idx, msg, playBackNum));
        } else {
            String msg = playBack.substring(start, maxLength);
            player.pushProto(SPlayer_PlayBackData.make(idx, msg, playBackNum));
            return;
        }
        idx++;
        subPlayBack(idx, end, maxLength, playBack, playBackNum);
    }

    /**
     * 获取房间id和局数获取回放码
     *
     * @param roomId 房间Id
     * @param tabId  局数
     * @return
     */
    public SData_Result getRoomPlayBackCode(long roomId, int tabId) {
        // A replay code is shareable once a player knows it, but the endpoint
        // that discovers the code from a sequential room ID must only be
        // available to participants of that room.
        if (ContainerMgr.get().getComponent(PlayerRoomAloneBOService.class).findOne(
                Restrictions.and(Restrictions.eq("roomID", roomId),
                        Restrictions.eq("pid", this.player.getPid())), "id") == null) {
            return SData_Result.make(ErrorCode.NotAllow, "player has no access to room playback");
        }
        PlayerSetRoomRecord playerSetRoomRecord = ContainerMgr.get().getComponent(GameSetBOService.class).findOneE(Restrictions.and(Restrictions.eq("roomID", roomId), Restrictions.eq("setID", tabId)), PlayerSetRoomRecord.class, PlayerSetRoomRecord.getItemsNamePlaybackCode());
        if (Objects.isNull(playerSetRoomRecord) || playerSetRoomRecord.getPlaybackCode() <= 0) {
            return SData_Result.make(ErrorCode.PlayBack_Error, "roomId:{%d},setId:{%d}", roomId, tabId);
        }
        String hand = CommFile.getHandFromFile(PlayBackMgr.getInstance().getFile(playerSetRoomRecord.getPlaybackCode()));
        if (StringUtils.isEmpty(hand) || !StringUtils.isNumeric(hand)) {
            return SData_Result.make(ErrorCode.PlayBack_Error, "error playBackCode");
        }
        GameType gameType = GameTypeMgr.getInstance().gameType(Integer.parseInt(hand));
        if (Objects.isNull(gameType)) {
            return SData_Result.make(ErrorCode.PlayBack_Error, "error playBackCode{%s}", hand);
        }
        business.global.replay.ReplayAuthorization.grant(this.player.getPid(), playerSetRoomRecord.getPlaybackCode());
        return SData_Result.make(ErrorCode.Success, SPlayer_FindPlayBack.make(gameType.getId(), playerSetRoomRecord.getPlaybackCode()));
    }


}
