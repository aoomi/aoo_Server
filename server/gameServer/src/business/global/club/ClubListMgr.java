package business.global.club;

import business.global.config.CurrencyKeyMgr;
import business.global.shareclub.ShareClubListMgr;
import business.global.sharegm.ShareInitMgr;
import business.global.shareunion.ShareUnionListMgr;
import business.global.union.Union;
import business.global.union.UnionMgr;
import business.player.Player;
import business.player.PlayerMgr;
import business.player.feature.PlayerWallet;
import business.shareplayer.SharePlayer;
import business.shareplayer.SharePlayerMgr;
import com.ddm.server.common.CommLogD;
import com.ddm.server.common.Config;
import com.ddm.server.common.GameConfig;
import com.ddm.server.common.mgr.sensitive.SensitiveWordMgr;
import com.ddm.server.common.utils.CommTime;
import com.ddm.server.common.utils.Maps;
import com.ddm.server.common.utils.StringUtil;
import com.ddm.server.websocket.def.ErrorCode;
import com.google.gson.Gson;
import core.db.entity.clarkGame.ClubListBO;
import core.db.other.Restrictions;
import core.db.service.clarkGame.ClubListBOService;
import core.ioc.ContainerMgr;
import core.network.http.proto.SData_Result;
import jsproto.c2s.cclass.club.ClubPromotionShowConfig;
import jsproto.c2s.cclass.club.Club_define.Club_MINISTER;
import jsproto.c2s.cclass.club.Club_define.Club_Status;
import jsproto.c2s.cclass.union.UnionDefine;
import jsproto.c2s.iclass.club.CClub_Create;
import lombok.Data;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;


/**
 * 亲友圈管理操作
 *
 * @author zaf
 */
@Data
public class ClubListMgr {
    /**
     * 亲友圈Map
     */
    protected Map<Long, Club> clubMap = Maps.newConcurrentMap();
    /**
     * 亲友圈Bo
     */
    private ClubListBOService clubListBOService;

    public ClubListMgr() {
        clubListBOService = ContainerMgr.get().getComponent(ClubListBOService.class);
    }


    /**
     * 初始化
     */
    public void init() {
        CommLogD.info("[ClubListBO.init] load ClubListBO begin...]");
        if (ShareInitMgr.getInstance().getShareDataInit()) {
            List<ClubListBO> clubListBOs = clubListBOService.findAll(null);
            if (CollectionUtils.isEmpty(clubListBOs)) {
                return;
            }
            for (ClubListBO clubBO : clubListBOs) {
                CurrencyKeyMgr.getInstance().clearKey(clubBO.getClubsign());
                if (clubBO.getStatus() != Club_Status.CLUB_STATUS_CLOSE.value()) {
                    clubMap.put(clubBO.getId(), new Club(clubBO));
                    //共享亲友圈
                    if (Config.isShare() && ShareInitMgr.getInstance().getShareDataInit()) {
                        try {
                            ShareClubListMgr.getInstance().addClub(clubMap.get(clubBO.getId()));
                        } catch (Exception e) {
                            CommLogD.error(e.getMessage(), e);
                        }
                    }
                }
            }
            clubListBOs = null;
        } else {
            setClubMap(ShareClubListMgr.getInstance().getAllClub());
        }
        CommLogD.info("[ClubListBO.init] load ClubListBO end]");

    }

    /*
     * 更新距离部
     * */
    public boolean onUpdateClub(long clubID) {
        ClubListBO clubListBO = getClubListBOService().findOne(clubID, null);
        if (Objects.isNull(clubListBO)) {
            return false;
        }
        Club club;
        if (Config.isShare()) {
            club = ShareClubListMgr.getInstance().getClub(clubID);
        } else {
            club = getClubMap().get(clubID);
        }
        if (Objects.isNull(club)) {
            club = new Club(clubListBO);
            getClubMap().put(clubListBO.getId(), club);
            //共享亲友圈
            if (Config.isShare()) {
                ShareClubListMgr.getInstance().addClub(club);
            }
        } else {
            club.onUpdateClub(clubListBO);
        }
        if (clubListBO.getStatus() == Club_Status.CLUB_STATUS_CLOSE.value()) {
            getClubMap().remove(clubListBO.getId());
            //删除共享亲友圈
            if (Config.isShare()) {
                ShareClubListMgr.getInstance().deleteClub(clubListBO.getId());
            }
        }

        return true;
    }


    /**
     * 关闭俱乐部
     * <p>
     * 110 后台操作标记
     *
     * @param clubId 亲友圈ID
     * @return
     */
    @SuppressWarnings("rawtypes")
    public SData_Result onCloseClub(long clubId, long exePid) {
        Club club;
        if (Config.isShare()) {
            club = ShareClubListMgr.getInstance().getClub(clubId);
        } else {
            club = this.getClubMap().get(clubId);
        }
        if (Objects.isNull(club)) {
            return SData_Result.make(ErrorCode.CLUB_NOT_EXIST, "CLUB_NOT_EXIST ClubId:{%d}", clubId);
        }
        if (123L != exePid && exePid != club.getClubListBO().getOwnerID()) {
            return SData_Result.make(ErrorCode.CLUB_NOT_EXIST_CREATE, "CLUB_NOT_EXIST_CREATE ClubId:{%d},exePid:{%d}", clubId, exePid);
        }
        // 亲友圈存在赛事
        if (club.getClubListBO().getUnionId() > 0L) {
            Union union = UnionMgr.getInstance().getUnionListMgr().findUnion(club.getClubListBO().getUnionId());
            if (Objects.isNull(union)) {
                // 赛事存在
                return SData_Result.make(ErrorCode.UNION_NOT_EXIST, "UNION_NOT_EXIST UnionId:{%d}", club.getClubListBO().getUnionId());
            }
            if (club.getClubListBO().getOwnerID() == union.getUnionBO().getOwnerId() && club.getClubListBO().getId() == union.getUnionBO().getClubId()) {
                // 赛事创建者不能解散亲友圈
                return SData_Result.make(ErrorCode.CLUB_UNION_CREATE, "CLUB_UNION_CREATE ClubId:{%d}", clubId);
            }
            // 检查竞技点是否为0:
            if (ClubMgr.getInstance().getClubMemberMgr().checkExistSportsPointNotEqualZero(clubId)) {
                return SData_Result.make(ErrorCode.UNION_EXIST_SPORTS_POINT_NOT_EQUAL_ZERO, "UNION_EXIST_SPORTS_POINT_NOT_EQUAL_ZERO");
            }
        }
        // 关闭所有房间，改变状态
        club.close();
        // 关闭亲友圈时踢出所有玩家
        ClubMgr.getInstance().getClubMemberMgr().onTuiChuInCloseClub(clubId);
        // 移除亲友圈
        this.getClubMap().remove(clubId);
        //删除共享亲友圈
        if (Config.isShare()) {
            ShareClubListMgr.getInstance().deleteClub(clubId);
        }
        // 查询指定亲友圈的所有状态
        UnionMgr.getInstance().getUnionMemberMgr().clubIdFindList(clubId, UnionDefine.UNION_PLAYER_STATUS.PLAYER_ALL).forEach(k -> {
            if (k.getStatus(UnionDefine.UNION_PLAYER_STATUS.PLAYER_JIARU.value())) {
                k.setStatus(UnionDefine.UNION_PLAYER_STATUS.PLAYER_TUICHU.value(), exePid);
            } else {
                k.setStatus(UnionDefine.UNION_PLAYER_STATUS.PLAYER_JUJIE.value(), exePid);
            }
        });
        return SData_Result.make(ErrorCode.Success);
    }

    /**
     * 游戏关闭返回预开的房卡
     */
    public void onGiveBackOnGameClose() {
        for (Club club : this.getClubMap().values()) {
            // 放回预开的房间
            club.gainRoomReservation();
        }
    }

    /**
     * 查找
     *
     * @param clubID 亲友圈ID
     * @return
     */
    public Club findClub(long clubID) {
        if (Config.isShare()) {
            return ShareClubListMgr.getInstance().getClub(clubID);
        } else {
            return this.getClubMap().get(clubID);
        }
    }

    /**
     * 查找
     *
     * @param clubID 亲友圈ID
     * @return
     */
    public Club findClubShare(long clubID) {
        return ShareClubListMgr.getInstance().getClub(clubID);
    }

    /**
     * 查找亲友圈信息
     *
     * @param clubSign 亲友圈标识key
     * @return
     */
    public Club findClub(int clubSign) {
        if (Config.isShare()) {
            ClubListBO clubListBO = clubListBOService.findOne(Restrictions.and(Restrictions.eq("clubsign", clubSign), Restrictions.eq("status", Club_Status.CLUB_STATUS_OPEN.value())), null);
            if (clubListBO != null) {
                return ShareClubListMgr.getInstance().getClub(clubListBO.getId());
            } else {
                return null;
            }
        } else {
            return this.getClubMap().values().stream().filter(k -> k.getClubListBO().getClubsign() == clubSign && k.getClubListBO().getStatus() == Club_Status.CLUB_STATUS_OPEN.value()).findAny().orElse(null);
        }
    }


    /**
     * 创建club
     */
    @SuppressWarnings("rawtypes")
    public SData_Result onClubCreate(CClub_Create data, Player player) {
        if (GameConfig.CreateClub() > 0 && (!player.getFeature(PlayerWallet.class).check(GameConfig.CreateClub()))) {
            return SData_Result.make(ErrorCode.NotEnough_RoomCard, String.format("钻石不足%d无法创建亲友圈", GameConfig.CreateClub()));
        }
        // 空字符
        if (StringUtils.isEmpty(data.getClubName())) {
            return SData_Result.make(ErrorCode.CLUB_NAME_ERROR, "名字不能为空");
        }
        // 获取字符长度 中文算两个字符 英文一个
        int length = StringUtil.String_length(data.getClubName());
        if (length > 16) {
            return SData_Result.make(ErrorCode.CLUB_NAME_ERROR, "名字长度超过规定字符数16");
        }
        String clubName = SensitiveWordMgr.getInstance().replaceSensitiveWordMax(data.getClubName());
        // 检查名称是否存在
        if (this.checkNameExist(clubName)) {
            return SData_Result.make(ErrorCode.CLUB_NAME_EXIST, "名称重复");
        }
        ClubListBO clubListBO = new ClubListBO();
        clubListBO.setClubName(clubName);
        clubListBO.setStatus(Club_Status.CLUB_STATUS_OPEN.value());
        clubListBO.setClubsign(CurrencyKeyMgr.getInstance().getNewKey());
        clubListBO.setCreattime(CommTime.nowSecond());
        clubListBO.setAgentsID(player.getFamiliID());
        clubListBO.setLevel(3);
        clubListBO.setMaxplayernum(800);
        clubListBO.setOwnerID(player.getPid());
        clubListBO.setDiamondsAttentionAll(100);//钻石提醒 全员默认100
        clubListBO.setDiamondsAttentionMinister(500);//钻石提醒 管理默认500
        //推广员显示列表默认
        List<Integer> promotionShowList = Arrays.asList(0, 3, 4, 5, 6, 7, 8);
        List<Integer> promotionShowSecondList = Arrays.asList(0, 1, 2);
        String promotionShowConfig = new Gson().toJson(new ClubPromotionShowConfig(promotionShowList, promotionShowSecondList));
        clubListBO.setPromotionShowConfig(promotionShowConfig);
        long clubID = clubListBO.getBaseService().saveOrUpDate(clubListBO);
        if (clubID <= 0L) {
            return SData_Result.make(ErrorCode.ErrorSysMsg, "创建亲友圈失败");
        }
        Club club = new Club(clubListBO);
        this.getClubMap().put(clubListBO.getId(), club);
        //共享亲友圈
        if (Config.isShare()) {
            ShareClubListMgr.getInstance().addClub(club);
        }
        // 操作添加亲友圈成员（已加入）
        ClubMgr.getInstance().getClubMemberMgr().onInsertClubMember(player, club, Club_MINISTER.Club_MINISTER_CREATER.value(), player.getPid());
        return SData_Result.make(ErrorCode.Success, ClubMgr.getInstance().getClubInfo(club, player));
    }


    /**
     * 检查名称是否存在
     *
     * @param clubName 亲友圈名称
     * @return T:存在重复，F:不存在
     */
    public boolean checkNameExist(String clubName) {
        if (Config.isShare()) {
            ClubListBO ClubListBO = clubListBOService.findOne(Restrictions.and(Restrictions.eq("name", clubName), Restrictions.eq("status", Club_Status.CLUB_STATUS_OPEN.value())), null);
            if (ClubListBO != null) {
                return true;
            } else {
                return false;
            }
        } else {
            // 检查是否有亲友圈数据。
            if (MapUtils.isEmpty(this.getClubMap())) {
                return false;
            }
            return clubMap.values().stream().filter(k -> Objects.nonNull(k) && clubName.equals(k.getClubListBO().getName())).findAny().isPresent();
        }
    }

    /**
     * 获取赛事的亲友圈id列表
     *
     * @param unionId
     * @return
     */
    public List<Long> getClubIdListByUnion(Long unionId) {
        List<ClubListBO> clubBoList = clubListBOService.findAll(Restrictions.eq("unionId", unionId), "");
        List<Long> clubIdList = clubBoList.stream().map(k -> k.getId()).collect(Collectors.toList());
        return clubIdList;
    }

    /**
     * 修改玩家邀请状态
     *
     * @return
     */
    @SuppressWarnings("rawtypes")
    public SData_Result changePlayerInvite(long exePid, int type) {
        if (Config.isShare()) {
            SharePlayer sharePlayer = SharePlayerMgr.getInstance().getSharePlayer(exePid);
            if (Objects.isNull(sharePlayer)) {
                return SData_Result.make(ErrorCode.Player_PidError);
            }
            sharePlayer.setInviteFlag(type == 0);
            SharePlayerMgr.getInstance().updateSharePlayer(sharePlayer);
        } else {
            Player player = PlayerMgr.getInstance().getPlayer(exePid);
            if (Objects.isNull(player)) {
                return SData_Result.make(ErrorCode.Player_PidError);
            }
            player.setInviteFlag(type == 0);
        }

        return SData_Result.make(ErrorCode.Success);
    }

}
