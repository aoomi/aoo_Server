package business.global.pk.njpdk;

import business.global.mj.set.BigDataControlVo;
import business.global.mj.set.PlayerWinControlVo;
import business.global.pk.njpdk.cardtype.NJPDKALGContainer;
import business.global.room.base.AbsRoomPos;
import business.global.room.base.AbsRoomSet;
import business.global.room.base.RoomPlayBack;
import business.njpdk.c2s.cclass.*;
import business.njpdk.c2s.cclass.NJPDK_define.FirstCardPosType;
import business.njpdk.c2s.cclass.NJPDK_define.FirstCardType;
import business.njpdk.c2s.cclass.NJPDK_define.NJPDK_CARD_TYPE;
import business.njpdk.c2s.cclass.NJPDK_define.NJPDK_GameStatus;
import business.njpdk.c2s.iclass.*;
import business.player.Player;
import business.player.PlayerMgr;
import business.player.Robot.RobotMgr;
import business.player.feature.PlayerCurrency;
import cenum.PrizeType;
import cenum.RoomTypeEnum;
import cenum.room.TrusteeshipState;
import com.alibaba.fastjson.JSON;
import com.aoo.bcg.common.random.GameRandomSource;
import com.aoo.bcg.common.random.SeededGameRandomSource;
import com.ddm.server.common.CommLogD;
import com.ddm.server.common.redis.RedisSource;
import com.ddm.server.common.utils.CommTime;
import com.ddm.server.websocket.handler.requset.WebSocketRequest;
import core.db.entity.clarkGame.GameSetBO;
import core.db.service.clarkGame.GameSetBOService;
import core.ioc.ContainerMgr;
import jsproto.c2s.cclass.BaseSendMsg;
import jsproto.c2s.cclass.pk.BasePockerLogic;
import jsproto.c2s.cclass.pk.Victory;
import jsproto.c2s.cclass.playback.PlayBackData;
import jsproto.c2s.cclass.room.BaseCreateRoom;
import jsproto.c2s.cclass.room.RoomPosInfo;
import org.apache.commons.collections4.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;


/**
 * 资阳跑得快一局游戏逻辑
 */
public class NJPDKRoomSet extends AbsRoomSet {
    private final GameRandomSource gameRandom = SeededGameRandomSource.create();
    public NJPDKRoom room;
    public long startMS = 0;
    protected NJPDK_GameStatus status = NJPDK_GameStatus.PDK_GAME_STATUS_SENDCARD;
    public NJPDKSetCard setCard = null;
    public NJPDKRoom_SetEnd setEnd = new NJPDKRoom_SetEnd();
    public GameSetBO bo = null;
    protected int m_OpPos = 0;                //当前操作位置
    protected Victory m_FirstOpVic = new Victory(-1, -1);        //首出操作位置
    protected int m_FirstOpCard = 0;        //先出的牌
    protected boolean m_bFirstOp = true;        //是否是一轮的首出
    protected ArrayList<Integer> m_RoomDoubleList = new ArrayList<Integer>();            //房间倍数
    public ArrayList<Integer> pointList;        //得分
    public ArrayList<Boolean> isCalcList;    //是否结算过
    public ArrayList<Integer> surplusCardRecordList;    //剩余牌数
    protected RoomPlayBack roomPlayBack;        //回放

    HashMap<Integer, List<Integer>> hMap = new HashMap<Integer, List<Integer>>();
    public NJPDKRoomSetRound curRound = null;
    public List<NJPDKRoomSetRound> historyRound = new ArrayList<>();
    public ArrayList<Victory> roomZhaDanList;        //房间倍数
    public int baoPeiPos = -1;// 包赔玩家
    public List<SNJPDK_OutCardList> cardList = new ArrayList<>();//出手牌顺序
    public List<Integer> totalPointResult = new ArrayList<>();//总分
    public ArrayList<Double> sportsPointList = null;
    public int cardSize = 16;//默认手牌十六张

    public NJPDKRoomSet(NJPDKRoom room) {
        super(room.getCurSetID());
        int cardSize = 16;
        if (room.getRoomCfg().getKexuanwanfa().contains(NJPDK_define.KeXuanWanFa.Card15.getType())) {
            cardSize = 15;
            this.cardSize = 15;
        }
        this.room = room;
        this.pointList = new ArrayList<>(Collections.nCopies(this.room.getPlayerNum(), 0));
        this.isCalcList = new ArrayList<>(Collections.nCopies(this.room.getPlayerNum(), false));
        this.surplusCardRecordList = new ArrayList<>(Collections.nCopies(this.room.getPlayerNum(), cardSize));
        this.initSportsPointList();
        this.addGameConfig();
        this.startSet();
        this.roomZhaDanList = new ArrayList<>();
    }

    private void initSportsPointList() {
        if (RoomTypeEnum.UNION.equals(this.room.getRoomTypeEnum())) {
            this.sportsPointList = new ArrayList<>(Collections.nCopies(this.room.getPlayerNum(), 0D));
        }
    }

    /**
     * 回放记录添加游戏配置
     */
    @Override
    public void addGameConfig() {
        this.getRoomPlayBack().addPlaybackList(SNJPDK_Config.make(room.getCfg(), this.room.getRoomTyepImpl().getRoomTypeEnum()), null);
    }

    /**
     * 标识Id
     *
     * @return
     */
    @Override
    public int getTabId() {
        return this.room.getTabId();
    }


    /**
     * 清除当局
     */
    @Override
    public void clear() {
        this.room = null;
        if (null != this.setCard) {
            this.setCard.clean();
            this.setCard = null;
        }
        this.cleanEndSetRoom();
        this.pointList = null;
        this.isCalcList = null;
        this.surplusCardRecordList = null;
        this.m_FirstOpVic = null;
        this.m_RoomDoubleList = null;
        this.setEnd = null;
        this.hMap = null;
        this.bo = null;
        this.totalPointResult = null;
        this.cardList = null;
    }

    @Override
    public void clearBo() {
        this.bo = null;
    }

    /**
     * 记录发起解散的玩家
     */
    public void addDissolveRoom(BaseSendMsg baseSendMsg) {
        if (this.status == NJPDK_GameStatus.PDK_GAME_STATUS_RESULT) {
            return;
        }
        NJPDKRoomPosMgr roomPosMgr = (NJPDKRoomPosMgr) this.room.getRoomPosMgr();
        this.getRoomPlayBack().addPlaybackList(baseSendMsg, roomPosMgr.getAllPlayBackNotify());
    }

    @Override
    public boolean checkExistPrizeType(PrizeType prizeType) {
        return prizeType.equals(this.room.getBaseRoomConfigure().getPrizeType());
    }

    /**
     * 每200ms更新1次   秒
     *
     * @param sec
     * @return T 是 F 否
     * PDK_GAME_STATUS_SENDCARD(0), //发牌
     * PDK_GAME_STATUS_COMPAER_ONE(1), //比牌
     * PDK_GAME_STATUS_COMPAER_SECOND(2), //比牌
     * PDK_GAME_STATUS_RESULT(3), //结算
     */
    public boolean update(int sec) {
        boolean isClose = false;
        switch (this.getStatus()) {
            case PDK_GAME_STATUS_SENDCARD:
                if (CommTime.nowMS() - this.startMS >= this.getWaitTimeByStatus()) {
                    //进入普通打牌阶段
                    this.onSendCardEnd();
                }
                break;
            case PDK_GAME_STATUS_COMPAER_SECOND:
                if (this.curRound == null) {
                    //新的回合
                    if (!this.startNewRound()) {
                        this.endSet();
                    }
                } else if (this.curRound != null) {
                    //第一回合后是否回合结束
                    boolean isRoundClosed = this.curRound.update();
                    if (isRoundClosed) {
                        if (this.curRound.isSetEnd()) {
                            this.endSet();
                        } else if (!this.startNewRound()) {//回合结束新增一回合
                            this.endSet();
                        }
                    }
                }
                break;
            case PDK_GAME_STATUS_RESULT:
                isClose = true;
                cleanEndSetRoom();
                break;
            default:
                break;
        }

        return isClose;
    }

    /**
     * 清空结束房间当前局
     */
    public void cleanEndSetRoom() {
        // 清空回合记录
        if (null != this.historyRound) {
            this.historyRound.forEach(key -> {
                if (null != key) {
                    key.clean();
                }
            });
            this.historyRound.clear();
            this.historyRound = null;
        }
        // 清空当前回合
        if (null != this.curRound) {
            this.curRound.clean();
            this.curRound = null;
        }
        // 房间回放
        if (null != this.roomPlayBack) {
            this.roomPlayBack.clear();
            this.roomPlayBack = null;
        }
    }

    /**
     * 发牌
     */
    public void onSendCardEnd() {
        this.setStatus(NJPDK_GameStatus.PDK_GAME_STATUS_COMPAER_SECOND);
        this.room.getRoomPosMgr().notify2All(SNJPDK_ChangeStatus.make(this.room.getRoomID(), this.status.value(), this.m_OpPos));
    }

    /*
     * 设置状态
     * */
    public void setStatus(NJPDK_GameStatus state) {
        if (this.status == state) return;
        this.status = state;
        this.startMS = CommTime.nowMS();
    }

    /*
     * 获取状态
     * */
    public NJPDK_GameStatus getStatus() {
        return this.status;
    }


    /**
     * 开启新的回合
     *
     * @return
     */
    public boolean startNewRound() {
        if (this.curRound != null) {
            this.historyRound.add(this.curRound);
        }

        this.curRound = new NJPDKRoomSetRound(this); // 开启第一轮
        return true;
    }

    public String getCacheWinName() {
        return "mj_player_win" + room.getRoomTypeEnum().ordinal() + "_" + room.getGameRoomBO().getGameType() + "_" + ((BaseCreateRoom) room.getCfg()).getBeishu();
    }

    /**
     * 发牌开始
     */
    public void startSet() {
        //设置参与游戏的玩家
        for (AbsRoomPos pos : this.room.getRoomPosMgr().posList) {
            NJPDKRoomPos roomPos = (NJPDKRoomPos) pos;
            if ((pos.isReady() && this.room.getCurSetID() == 1) || (this.room.getCurSetID() > 1 && pos.getPid() != 0)) {
                roomPos.setPlayTheGame(true);
            }
        }
        business.global.replay.LegacyReplayParticipantRegistry.getInstance().grantAll(
                this.room.getRoomID(), this.room.getCurSetID(), this.room.getRoomPosMgr().posList.stream()
                        .filter(pos -> pos.getPid() > 0 && pos.isPlayTheGame())
                        .collect(Collectors.toMap(pos -> pos.getPid(), pos -> pos.getPosID(), (left, right) -> left)));
        int cardSize = 16;
        if (room.getRoomCfg().getKexuanwanfa().contains(NJPDK_define.KeXuanWanFa.Card15.getType())) {
            cardSize = 15;
        }
        // 洗底牌
        this.setCard = new NJPDKSetCard(this.room);
        //是否开启神牌模式
        if (room.isGodCard())
            godCard();
        for (int i = 0; i < this.room.getXiPaiList().size(); i++) {
            this.setCard.onXiPai();
        }
        this.room.getXiPaiList().clear();

        List<NJPDKRoomPos> controlPos = new ArrayList<>();

        for (int j = 0; j < this.room.getPlayerNum(); j++) {
            NJPDKRoomPos roomPos = (NJPDKRoomPos) this.room.getRoomPosMgr().getPosByPosID((m_OpPos + j) % this.room.getPlayerNum());

            RedisSource redisSource = ContainerMgr.get().getRedis();

            double total = 0;
            String cacheName = getCacheWinName() + roomPos.getPid();
            if (redisSource.exists(cacheName)) {
                total = Double.parseDouble(redisSource.get(cacheName));
            }

            // 单控
            if (redisSource.exists("player_win_control")) {
                String controlString = redisSource.get("player_win_control");
                List<PlayerWinControlVo> controlVos = JSON.parseArray(controlString, PlayerWinControlVo.class);
                for (PlayerWinControlVo controlVo : controlVos) {
                    if (!controlVo.isFinish() && controlVo.getId() == roomPos.getPid() && controlVo.getRoomType() == room.getRoomTypeEnum().ordinal() && controlVo.getGameType() == room.getGameRoomBO().getGameType() && controlVo.getBeishu() == ((BaseCreateRoom) room.getCfg()).getBeishu()) {
                        if (room.getRoomTypeEnum() == RoomTypeEnum.CLUB && controlVo.getUnion() != room.getGameRoomBO().getClubID()) {
                            continue;
                        } else if (room.getRoomTypeEnum() == RoomTypeEnum.UNION && controlVo.getUnion() != room.getGameRoomBO().getUnionId()) {
                            continue;
                        }
                        if (controlVo.getFinishValue() > total) {
                            if (gameRandom.nextInt(100) < controlVo.getRate()) {
                                controlPos.add(roomPos);
                                break;
                            }
                        } else {
                            controlVo.setFinish(true);
                            redisSource.put("player_win_control", JSON.toJSONString(controlVos));
                        }
                    }
                }
            }
        }

        // 大数据控
        if (0 == controlPos.size()) {
            for (int j = 0; j < this.room.getPlayerNum(); j++) {
                NJPDKRoomPos roomPos = (NJPDKRoomPos) this.room.getRoomPosMgr().getPosByPosID((m_OpPos + j) % this.room.getPlayerNum());

                RedisSource redisSource = ContainerMgr.get().getRedis();

                double total = 0;
                String cacheName = getCacheWinName() + roomPos.getPid();
                if (redisSource.exists(cacheName)) {
                    total = Double.parseDouble(redisSource.get(cacheName));
                }

                if (total < 0 && redisSource.exists("big_data_control")) {
                    String controlString = redisSource.get("big_data_control");
                    List<BigDataControlVo> controlVos = JSON.parseArray(controlString, BigDataControlVo.class);
                    controlVos = controlVos.stream().filter(bigDataControlVo -> bigDataControlVo.getRoomType() == room.getRoomTypeEnum().ordinal() && bigDataControlVo.getGameType() == room.getGameRoomBO().getGameType()).collect(Collectors.toList());
                    if (0 < controlVos.size()) {
                        for (BigDataControlVo vo : controlVos) {
                            if (-total > ((BaseCreateRoom) room.getCfg()).getBeishu() * vo.getBeishu()) {
                                if (gameRandom.nextInt(100) < vo.getRate()) {
                                    controlPos.add(roomPos);
                                }
                            }
                        }
                    }
                }
            }
        }

        NJPDKRoomPos controlItem = null;
        if (0 != controlPos.size()) {
            controlItem = controlPos.get(gameRandom.nextInt(controlPos.size()));
            List<Integer> tempCards = new ArrayList<>();
            ArrayList<Integer> allCards = this.setCard.leftCards;
            // 先来点大牌
            List<Integer> daCards = Arrays.asList(0x3F, 0x1E, 0x2E, 0x3E, 0x0D, 0x1D, 0x2D, 0x3D);
            for (Integer card : daCards) {
                if (gameRandom.nextBoolean()) {
                    if (allCards.remove(card)) {
                        tempCards.add(card);
                    }
                }
            }
            Map<Integer, List<Integer>> s = NJPDKALGContainer.getInstance().getValueListMapByList(allCards);
//            // 来炸弹
//            if (gameRandom.nextInt(100) < 10) {
//                List<Integer> zhadan = new ArrayList<>();
//                for (Map.Entry<Integer, List<Integer>> entry : s.entrySet()) {
//                    if (entry.getValue().size() == 4) {
//                        zhadan.add(entry.getKey());
//                    }
//                }
//                if (13 > tempCards.size()) {
//                    int round = gameRandom.nextInt(zhadan.size());
//                    if (allCards.removeAll(s.get(zhadan.get(round)))) {
//                        tempCards.addAll(s.get(zhadan.get(round)));
//                    }
//                }
//                s = NJPDKALGContainer.getInstance().getValueListMapByList(allCards);
//            }
            // 来三个
            if (13 > tempCards.size() && gameRandom.nextInt(100) < 30) {
                List<Integer> sange = new ArrayList<>();
                for (Map.Entry<Integer, List<Integer>> entry : s.entrySet()) {
                    if (entry.getValue().size() > 2) {
                        sange.add(entry.getKey());
                    }
                }
                int round = gameRandom.nextInt(sange.size());
                List<Integer> sange1 = s.get(sange.get(round));
                List<Integer> lianduiCards = new ArrayList<>();
                lianduiCards.add(sange1.remove(gameRandom.nextInt(sange1.size())));
                lianduiCards.add(sange1.remove(gameRandom.nextInt(sange1.size())));
                lianduiCards.add(sange1.remove(gameRandom.nextInt(sange1.size())));
                if (allCards.removeAll(lianduiCards)) {
                    tempCards.addAll(lianduiCards);
                }
                sange.remove(round);
                if (13 > tempCards.size() && gameRandom.nextInt(100) < 10) {
                    round = gameRandom.nextInt(sange.size());
                    sange1 = s.get(sange.get(round));
                    lianduiCards = new ArrayList<>();
                    lianduiCards.add(sange1.remove(gameRandom.nextInt(sange1.size())));
                    lianduiCards.add(sange1.remove(gameRandom.nextInt(sange1.size())));
                    lianduiCards.add(sange1.remove(gameRandom.nextInt(sange1.size())));
                    if (allCards.removeAll(lianduiCards)) {
                        tempCards.addAll(lianduiCards);
                    }
                    sange.remove(round);
                }
                s = NJPDKALGContainer.getInstance().getValueListMapByList(allCards);
            }

            if (11 > tempCards.size()) {
                // 来个顺子
                List<Integer> danpai = new ArrayList<>(s.keySet());
                gameRandom.shuffle(danpai);
                List<Integer> liandui = new ArrayList<>();
                for (Integer entry : danpai) {
                    if (danpai.contains(entry + 1) && danpai.contains(entry + 2) && danpai.contains(entry + 3) && danpai.contains(entry + 4)) {
                        liandui.add(entry);
                    }
                }
                if (liandui.size() > 0) {
                    int round = gameRandom.nextInt(liandui.size());
                    List<Integer> lianduiCards = new ArrayList<>();
                    List<Integer> liandui1 = s.get(liandui.get(round));
                    List<Integer> liandui2 = s.get(liandui.get(round) + 1);
                    List<Integer> liandui3 = s.get(liandui.get(round) + 2);
                    List<Integer> liandui4 = s.get(liandui.get(round) + 3);
                    List<Integer> liandui5 = s.get(liandui.get(round) + 4);
                    lianduiCards.add(liandui1.remove(gameRandom.nextInt(liandui1.size())));
                    lianduiCards.add(liandui2.remove(gameRandom.nextInt(liandui2.size())));
                    lianduiCards.add(liandui3.remove(gameRandom.nextInt(liandui3.size())));
                    lianduiCards.add(liandui4.remove(gameRandom.nextInt(liandui4.size())));
                    lianduiCards.add(liandui5.remove(gameRandom.nextInt(liandui5.size())));
                    if (allCards.removeAll(lianduiCards)) {
                        tempCards.addAll(lianduiCards);
                    }
                    s = NJPDKALGContainer.getInstance().getValueListMapByList(allCards);
                }
            }

            if (10 > tempCards.size()) {
                // 来4连对
                List<Integer> duizi = new ArrayList<>();
                for (Map.Entry<Integer, List<Integer>> entry : s.entrySet()) {
                    if (entry.getValue().size() > 1) {
                        duizi.add(entry.getKey());
                    }
                }
                gameRandom.shuffle(duizi);
                List<Integer> liandui = new ArrayList<>();
                for (Integer entry : duizi) {
                    if (duizi.contains(entry + 1) && duizi.contains(entry + 2)) {
                        liandui.add(entry);
                    }
                }
                int round = gameRandom.nextInt(liandui.size());
                List<Integer> lianduiCards = new ArrayList<>();
                List<Integer> liandui1 = s.get(liandui.get(round));
                List<Integer> liandui2 = s.get(liandui.get(round) + 1);
                List<Integer> liandui3 = s.get(liandui.get(round) + 2);
                lianduiCards.add(liandui1.remove(gameRandom.nextInt(liandui1.size())));
                lianduiCards.add(liandui1.remove(gameRandom.nextInt(liandui1.size())));
                lianduiCards.add(liandui2.remove(gameRandom.nextInt(liandui2.size())));
                lianduiCards.add(liandui2.remove(gameRandom.nextInt(liandui2.size())));
                lianduiCards.add(liandui3.remove(gameRandom.nextInt(liandui3.size())));
                lianduiCards.add(liandui3.remove(gameRandom.nextInt(liandui3.size())));
                if (allCards.removeAll(lianduiCards)) {
                    tempCards.addAll(lianduiCards);
                }
            }


            if (0 < tempCards.size()) {
                if (cardSize > tempCards.size()) {
                    tempCards.addAll(this.setCard.popList(cardSize - tempCards.size()));
                }
                controlItem.init(tempCards);

            }
        }


        for (int j = 0; j < this.room.getPlayerNum(); j++) {
            NJPDKRoomPos roomPos = (NJPDKRoomPos) this.room.getRoomPosMgr().getPosByPosID((m_OpPos + j) % this.room.getPlayerNum());
            if (roomPos == controlItem) {
                continue;
            }
            //如果是DEBUG模式发送神牌
            if (room.isGodCard()) {
                //获取神牌牌堆
                roomPos.init(hMap.get(j));
            } else {
                roomPos.init(this.setCard.popList(cardSize));
            }
        }

        //设置出牌玩家，出的牌
        this.setDefaultOutCardPos();
        this.startMS = CommTime.nowMS();
        for (int j = 0; j < room.getPlayerNum(); j++) {
            NJPDKRoomPos roomPos = (NJPDKRoomPos) room.getRoomPosMgr().getPosByPosID(j);
            roomPos.canGuan = false;
        }
        //特殊玩法，3A12直接大关
        if (room.getRoomCfg().teshu.contains(0)) {
            for (int j = 0; j < room.getPlayerNum(); j++) {
                NJPDKRoomPos roomPos = (NJPDKRoomPos) room.getRoomPosMgr().getPosByPosID(j);
                if (roomPos.getPrivateCards().containsAll(Arrays.asList(0x1E, 0x2E, 0x3E, 0x3F))) {
                    roomPos.canGuan = true;
                    break;
                }
            }
        }
        if (room.getRoomCfg().teshu.contains(1)) {
            for (int j = 0; j < room.getPlayerNum(); j++) {
                NJPDKRoomPos roomPos = (NJPDKRoomPos) room.getRoomPosMgr().getPosByPosID(j);
                if (roomPos.getPrivateCards().containsAll(Arrays.asList(0x03, 0x13, 0x23, 0x33))) {
                    roomPos.canGuan = true;
                    break;
                }
            }
        }
        if (room.getRoomCfg().teshu.contains(2)) {
            for (int j = 0; j < room.getPlayerNum(); j++) {
                NJPDKRoomPos roomPos = (NJPDKRoomPos) room.getRoomPosMgr().getPosByPosID(j);
                if (room.checkQuanDui(roomPos.getPrivateCards())) {
                    roomPos.canGuan = true;
                    break;
                }
            }
        }
        if (room.getRoomCfg().teshu.contains(3)) {
            for (int j = 0; j < room.getPlayerNum(); j++) {
                NJPDKRoomPos roomPos = (NJPDKRoomPos) room.getRoomPosMgr().getPosByPosID(j);
                if (room.check10Xiao(roomPos.getPrivateCards())) {
                    roomPos.canGuan = true;
                    break;
                }
            }
        }
        int guanPos = room.getNextGuan(m_OpPos);
        if (-1 != guanPos) {
            setOpPos(guanPos);
        }

        NJPDKRoomPosMgr roomPosMgr = (NJPDKRoomPosMgr) this.room.getRoomPosMgr();
        //开始发牌
        for (int i = 0; i < this.room.getPlayerNum(); i++) {
            long pid = this.room.getRoomPosMgr().getPosByPosID(i).getPid();
            if (0 == i) {
                //只记录一次setStart到数据库，并发送setStart
                this.getRoomPlayBack().playBack2Pos(i, SNJPDK_SetStart.make(this.room.getRoomID(), this.getNotify_set(pid)), roomPosMgr.getAllPlayBackNotify());
            } else {
                this.room.getRoomPosMgr().notify2Pos(i, SNJPDK_SetStart.make(this.room.getRoomID(), this.getNotify_set(pid)));
            }
        }
        this.room.getTrusteeship().setTrusteeshipState(TrusteeshipState.Normal);
    }


    /**
     * 局结束
     */
    public void endSet() {
        //printCallStatck();
        //已经结算过当局，返回
        if (this.status == NJPDK_GameStatus.PDK_GAME_STATUS_RESULT)
            return;

        if (!business.global.billing.LegacySettlementGuard.beginRound(
                this.room.getRoomID(), this.room.getCurSetID(), "njpdk-v1"))
            return;

        this.setStatus(NJPDK_GameStatus.PDK_GAME_STATUS_RESULT);
        this.calPoint();

        List<Integer> shutDownList = new ArrayList<>(Collections.nCopies(this.room.getPlayerNum(), -1));
        for (AbsRoomPos pos : room.getRoomPosMgr().posList) {
            ((NJPDKRoomPos) pos).cards().sort(BasePockerLogic.sorterBigToSmallNotTrump);
            int privateListSize = ((NJPDKRoomPos) pos).cards().size();
            int outCard = cardSize - privateListSize;
            shutDownList.set(pos.getPosID(), outCard == 1 || outCard == 0 ? outCard : -1);
        }
        roomPlayBack();

        List<Integer> bombList = new ArrayList<>(Collections.nCopies(this.room.getPlayerNum(), 0));
        for (Victory victory : roomZhaDanList) {
            bombList.set(victory.getPos(), victory.getNum());
        }
        if (this.room.getDissolveRoom() == null) {
            // 广播
            NJPDKRoomPosMgr roomPosMgr = (NJPDKRoomPosMgr) room.getRoomPosMgr();
            for (int viewerPos = 0; viewerPos < room.getPlayerNum(); viewerPos++) {
                getRoomPlayBack().playBack2Pos(viewerPos, SNJPDK_SetEnd.make(this.room.getRoomID(), this.status.value(), this.startMS,
                        this.getFirstOpPos(), this.pointList, surplusCardRecordList, bombList, totalPointResult,
                        cardList, cardViewFor(viewerPos), this.bo.getPlayBackCode(), this.sportsPointList, shutDownList),
                        roomPosMgr.getAllPlayBackNotify());
            }
        }
    }

    public static void printCallStatck() {
        System.getLogger(NJPDKRoomSet.class.getName()).log(System.Logger.Level.DEBUG,
                "NJPDK call stack", new Throwable("NJPDK call stack"));
    }

    /**
     * 结算积分
     */
    public void calPoint() {
        GameSetBO gameSetBO = ContainerMgr.get().getComponent(GameSetBOService.class).findOne(room.getRoomID(), this.room.getCurSetID());
        this.bo = gameSetBO == null ? new GameSetBO() : gameSetBO;
        if (gameSetBO == null) {
            bo.setRoomID(room.getRoomID());
            bo.setSetID(this.room.getCurSetID());
            bo.setTabId(this.room.getTabId());
        }

        //计算包赔和牌数
        this.calcSurplusCardList();
        //结算分数
        NJPDKGameResult result = new NJPDKGameResult(this.room);
        int winPos = result.calPoint();
        for (int i = 0; i < this.room.getPlayerNum(); i++) {
            NJPDKRoomPos iRoomPos = (NJPDKRoomPos) this.room.getRoomPosMgr().getPosByPosID(i);
            iRoomPos.setMaxPoint(this.pointList.get(i));
            if (PrizeType.Gold == this.room.getBaseRoomConfigure().getPrizeType()) {
                this.pointList.set(i, this.pointList.get(i) * this.room.getBaseMark());
            }
        }
        this.room.setLastWinPos(winPos);
        for (int i = 0; i < this.room.getPlayerNum(); i++) {
            NJPDKRoomPos roomPos = (NJPDKRoomPos) this.room.getRoomPosMgr().getPosByPosID(i);
            if (winPos != -1) {
                if (i == winPos) {
                    roomPos.addWin(1);
                } else {
                    roomPos.addLose(1);
                }
            } else {
                roomPos.addFlat(1);
            }
        }
        //分数扣除金币，并通知客户端
        for (int i = 0; i < this.room.getPlayerNum(); i++) {
            NJPDKRoomPos roomPos = (NJPDKRoomPos) this.room.getRoomPosMgr().getPosByPosID(i);
            NJPDKRoom_PosEnd posEnd = roomPos.calcPosEnd();
            goldEnd(i, this.pointList.get(i));
            if (CollectionUtils.isNotEmpty(this.sportsPointList)) {
                this.sportsPointList.set(posEnd.pos, Objects.isNull(posEnd.sportsPoint) ? 0D : posEnd.sportsPoint);
            }
            this.setEnd.posResultList.add(posEnd);
            totalPointResult.add(roomPos.getPoint());
        }

        room.getRoomPosMgr().setAllLatelyOutCardTime();

        List<Integer> bombList = new ArrayList<>();
        for (int i = 0; i < room.getPlayerNum(); i++) {
            bombList.add(i < roomZhaDanList.size() ? roomZhaDanList.get(i).getNum() : 0);
        }

        this.setEnd.roomDoubleList = bombList;
        this.setEnd.endTime = CommTime.nowSecond();

        NJPDKRoom_SetEnd lSetEnd = this.getNotify_setEnd();

        String gsonSetEnd = JSON.toJSONString(lSetEnd);
        bo.setPlayBackCode(getPlayBackDateTimeInfo().getPlayBackCode());
        bo.setDataJsonRes(gsonSetEnd);
        bo.setEndTime(setEnd.endTime);
        bo.getBaseService().saveOrUpDate(bo);
        RedisSource redisSource = ContainerMgr.get().getRedis();
        for (NJPDKRoom_PosEnd posEnd : this.setEnd.posResultList) {
            String cacheName = getCacheWinName() + posEnd.pid;
            if (room.getRoomTypeEnum() == RoomTypeEnum.UNION) {
                if (redisSource.exists(cacheName)) {
                    double total = Double.parseDouble(redisSource.get(cacheName));
                    redisSource.put(cacheName, String.valueOf(total + posEnd.sportsPoint));
                } else {
                    redisSource.put(cacheName, String.valueOf(posEnd.sportsPoint));
                }
            } else {
                if (redisSource.exists(cacheName)) {
                    double total = Double.parseDouble(redisSource.get(cacheName));
                    redisSource.put(cacheName, String.valueOf(total + posEnd.point));
                } else {
                    redisSource.put(cacheName, String.valueOf(posEnd.point));
                }
            }
        }
    }

    /**
     * 练习场结算
     */
    private void goldEnd(int posID, int shui) {
        if (this.checkExistPrizeType(PrizeType.Gold)) {
            NJPDKRoomPos pos = (NJPDKRoomPos) this.room.getRoomPosMgr().getPosByPosID(posID);
            if (!RobotMgr.getInstance().isRobot((int) pos.getPid())) {
                Player player = PlayerMgr.getInstance().getPlayer(pos.getPid());
                player.getFeature(PlayerCurrency.class).goldRoomEnd(shui, room.getBaseMark(),
                        this.room.getBaseRoomConfigure().getGameType().getId());
            } else {
                RobotMgr.getInstance().freeRobot((int) pos.getPid());
            }
        }
    }

    /**
     * 获取通知设置
     *
     * @param pid 用户ID
     * @return
     */
    public NJPDKRoomSetInfo getNotify_set(long pid) {
        NJPDKRoomSetInfo ret = new NJPDKRoomSetInfo();
        ret.roomID = this.room.getRoomID();
        ret.setSetID(this.room.getCurSetID());
        ret.startTime = this.startMS;
        ret.runWaitSec = (CommTime.nowMS() - startMS) / 1000;
        ret.state = this.status.value();
        ret.opPos = m_OpPos;
        ret.firstOpCard = m_FirstOpCard;
        ret.isFirstOp = m_bFirstOp;
        ret.roomDoubleList = m_RoomDoubleList;
        ret.cardNumMap = this.setCard.getCardNumMap();
        if (null != this.curRound) {
            ret.lastOpPos = this.curRound.getLastOpPos();
            ret.opType = this.curRound.getOpCardType();
            ret.cardList = this.curRound.getCardList();
            ret.opPos = this.curRound.getOpPos();
            ret.isSetEnd = this.curRound.isSetEnd();
        }
        List<Integer> shutDownList = new ArrayList<>(Collections.nCopies(this.room.getPlayerNum(), -1));
        if (this.status == NJPDK_GameStatus.PDK_GAME_STATUS_RESULT) {
            List<List<Integer>> privateList = new ArrayList<>();
            for (AbsRoomPos pos : room.getRoomPosMgr().posList) {
                ((NJPDKRoomPos) pos).cards().sort(BasePockerLogic.sorterBigToSmallNotTrump);
                privateList.add(pos.getPid() == pid ? List.copyOf(((NJPDKRoomPos) pos).cards()) : List.of());
                int privateListSize = ((NJPDKRoomPos) pos).cards().size();
                int outCard = cardSize - privateListSize;
                shutDownList.set(pos.getPosID(), outCard == 1 || outCard == 0 ? outCard : -1);
            }
            List<Integer> bombList = new ArrayList<>();
            for (int i = 0; i < room.getPlayerNum(); i++) {
                bombList.add(i < roomZhaDanList.size() ? roomZhaDanList.get(i).getNum() : 0);
            }

            SNJPDK_SetEnd end = SNJPDK_SetEnd.make(this.room.getRoomID(), this.status.value(), this.startMS,
                    this.getFirstOpPos(), this.pointList, surplusCardRecordList, bombList, totalPointResult, cardList, privateList, this.bo.getPlayBackCode(), sportsPointList, shutDownList);
            ret.setEnd = end;
        }

        if (NJPDK_GameStatus.PDK_GAME_STATUS_RESULT.equals(this.status)) {
            ret.playBackCode = getPlayBackDateTimeInfo().getPlayBackCode();
        }

        // 每个玩家的牌面
        ret.posInfo = new ArrayList<>();
        for (int i = 0; i < this.room.getPlayerNum(); i++) {
            NJPDKRoomPos roomPos = (NJPDKRoomPos) this.room.getRoomPosMgr().getPosByPosID(i);
            RoomPosInfo room_Pos = this.room.getRoomPosMgr().getNotify_PosList().get(i);
            int setPoint = this.pointList.get(i);
            NJPDKRoomSet_Pos roomSet_Pos = new NJPDKRoomSet_Pos();
            roomSet_Pos.posID = room_Pos.getPos();
            roomSet_Pos.pid = room_Pos.getPid();
            roomSet_Pos.cards = roomPos.getNotifyCard(pid);
            roomSet_Pos.point = setPoint;
            roomSet_Pos.guanpai = roomPos.canGuan;
            roomSet_Pos.surplusCardList = surplusCardRecordList;
            roomSet_Pos.sportsPoint = roomPos.setSportsPoint(setPoint);
            ret.posInfo.add(roomSet_Pos);
        }

        return ret;
    }

    /**
     * 获取通知设置结束
     *
     * @return
     */
    public NJPDKRoom_SetEnd getNotify_setEnd() {
        return setEnd;
    }

    //托管
    public void roomTrusteeship(int pos) {
        if (null != this.curRound) this.curRound.roomTrusteeship(pos);
    }

    /**
     * 获取阶段等待时间
     *
     * @return
     */
    public int getWaitTimeByStatus() {
        int waitTime = 0;
        switch (this.status) {
            case PDK_GAME_STATUS_SENDCARD:
                waitTime = 0;
                break;
            case PDK_GAME_STATUS_COMPAER_SECOND:
                waitTime = 15000;
                break;
            case PDK_GAME_STATUS_RESULT:
                waitTime = 10000;
                break;
            default:
                break;
        }
        return waitTime;
    }

    /**
     * 设置首出牌和首出玩家
     */
    public void setDefaultOutCardPos() {
        //判断出牌玩家，再判断出的牌
        NJPDKRoomPosMgr roomPosMgr = (NJPDKRoomPosMgr) this.room.getRoomPosMgr();
        //黑桃三的值
        int card = 0;

        //获取设置首出玩家
        if (FirstCardPosType.Has_Spade_Three_Of_FirstSet.has(room.getRoomCfg().chupai)) {
            // 玩法是首局拥有黑桃三的首出
            //第一局需要判断黑桃三
            if (this.room.getCurSetID() == 1) {
                card = 0x33;
                //判断拥有黑桃三的位置，这个位置是首出的位置
                int pos = roomPosMgr.getPosByCard(card);
                while (pos == -1) {
                    card -= 16;
                    if (card < 0) {
                        card += 65;
                    }
                    pos = roomPosMgr.getPosByCard(card);
                }
                setOpPos(pos >= 0 ? pos : this.m_OpPos);
            } else {
                //大于第一局首出的位置是赢家
                setOpPos(Math.max(this.room.getLastWinPos(), 0));
            }
        } else if (FirstCardPosType.Has_Spade_Three_Of_EverySet.has(room.getRoomCfg().chupai)) {
            //玩法是每局拥有黑桃三的首出
            //判断拥有黑桃三的位置，这个位置是首出的位置
            card = 0x33;
            int pos = roomPosMgr.getPosByCard(card);
            while (pos == -1) {
                card -= 16;
                if (card < 0) {
                    card += 65;
                }
                pos = roomPosMgr.getPosByCard(card);
            }
            setOpPos(pos >= 0 ? pos : this.m_OpPos);
        } else if (FirstCardPosType.Random_EverySet.has(room.getRoomCfg().chupai)) {
            //玩法是每局随机一个玩家
            setOpPos(gameRandom.nextInt(room.getRoomCfg().getPlayerNum()));
        } else {
            CommLogD.error("room.cfg.chupai is Error");
        }

        //设置获取首出牌
        if (FirstCardType.Has_Spade_Three.has(room.getRoomCfg().heitaosanbichu) && 0 != card) {
            //首出牌带黑桃三
            this.m_FirstOpCard = card;
        } else if (FirstCardType.Random_EverySet.has(room.getRoomCfg().heitaosanbichu)) {
            //随机牌出
            this.m_FirstOpCard = 0;
        } else {
            CommLogD.error("room.cfg.heitaosanbichu is Error");
        }
    }

    /**
     * @return curRound
     */
    public NJPDKRoomSetRound getCurRound() {
        return curRound;
    }


    /**
     * 获取房间回放记录
     *
     * @return
     */
    public RoomPlayBack getRoomPlayBack() {
        if (null == this.roomPlayBack)
            this.roomPlayBack = new NJPDKRoomPlayBackImpl(this.room);
        return this.roomPlayBack;
    }

    /**
     * 如果是房卡类型，才需要回放记录
     */
    public void roomPlayBack() {
        if (this.checkExistPrizeType(PrizeType.RoomCard)) {
            PlayBackData playBackData = new PlayBackData(this.room.getRoomID(),
                    this.room.getCurSetID(), 0, this.room.getCount(),
                    this.room.getRoomKey(),
                    this.room.getBaseRoomConfigure().getGameType().getId(), getPlayBackDateTimeInfo());
            this.getRoomPlayBack().addPlayBack(playBackData);
        }
    }

    /**
     * 插入牌 并在牌堆里面删除
     */
    public ArrayList<Integer> getGodCard(ArrayList<Integer> list) {
        if (!room.isGodCard()) return new ArrayList<Integer>();
        int cardNum = 16;
        if (room.getRoomCfg().getKexuanwanfa().contains(NJPDK_define.KeXuanWanFa.Card15.getType())) {
            cardNum = 15;
        }
        ArrayList<Integer> cardList = new ArrayList<Integer>(cardNum);
        cardList.addAll(list);
        int count = cardNum - cardList.size();
        ArrayList<Integer> tempList = this.setCard.popList(count);
        BasePockerLogic.deleteCard(this.setCard.getLeftCards(), tempList);
        cardList.addAll(tempList);
        return cardList;
    }

    /**
     * 设置神牌
     */
//	0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x0A, 0x0B, 0x0C, 0x0D,  0x0E,	0x0F, //方块3~2
//	0x13, 0x14, 0x15, 0x16, 0x17, 0x18, 0x19, 0x1A, 0x1B, 0x1C, 0x1D,  0x1E,	0x1F, //梅花3~2
//	0x23, 0x24, 0x25, 0x26, 0x27, 0x28, 0x29, 0x2A, 0x2B, 0x2C, 0x2D,  0x2E,	0x2F, //红桃3~2
//	0x33, 0x34, 0x35, 0x36, 0x37, 0x38, 0x39, 0x3A, 0x3B, 0x3C, 0x3D,  0x3E,	0x3F, //黑桃3~2
    public void godCard() {
        if (!room.isGodCard()) return;
        boolean flag1 = BasePockerLogic.deleteCard(this.setCard.getLeftCards(), room.getConfigMgr().getPrivate_Card1());
        boolean flag2 = BasePockerLogic.deleteCard(this.setCard.getLeftCards(), room.getConfigMgr().getPrivate_Card2());
        boolean flag3 = BasePockerLogic.deleteCard(this.setCard.getLeftCards(), room.getConfigMgr().getPrivate_Card3());
        boolean flag4 = BasePockerLogic.deleteCard(this.setCard.getLeftCards(), room.getConfigMgr().getPrivate_Card4());
        if (flag1 && flag2 && flag3 && flag4) {
            ArrayList<Integer> card1 = getGodCard(room.getConfigMgr().getPrivate_Card1());
            ArrayList<Integer> card2 = getGodCard(room.getConfigMgr().getPrivate_Card2());
            ArrayList<Integer> card3 = getGodCard(room.getConfigMgr().getPrivate_Card3());
            ArrayList<Integer> card4 = getGodCard(room.getConfigMgr().getPrivate_Card4());
            hMap.put(0, card1);
            hMap.put(1, card2);
            hMap.put(2, card3);
            hMap.put(3, card4);
        } else {
            this.setCard.randomCard();
            int cardNum = 16;
            if (room.getRoomCfg().getKexuanwanfa().contains(NJPDK_define.KeXuanWanFa.Card15.getType())) {
                cardNum = 15;
            }
            hMap.put(0, this.setCard.popList(cardNum));
            hMap.put(1, this.setCard.popList(cardNum));
            hMap.put(2, this.setCard.popList(cardNum));
            hMap.put(3, this.setCard.popList(cardNum));
        }
    }

    /**
     * @return m_OpPos
     */
    public int getOpPos() {
        return m_OpPos;
    }

    /**
     * @param m_OpPos 要设置的 m_OpPos
     */
    public void setOpPos(int m_OpPos) {
        NJPDKRoomPos tempRoomPos = (NJPDKRoomPos) this.room.getRoomPosMgr().getPosByPosID(m_OpPos);
        tempRoomPos.setLatelyOutCardTime(CommTime.nowMS());
        this.m_OpPos = m_OpPos;
    }

    /**
     * @param m_bFirstOp 要设置的 m_bFirstOp
     */
    public void setFirstOp(boolean m_bFirstOp) {
        this.m_bFirstOp = m_bFirstOp;
    }

    /**
     * @return m_bFirstOp
     */
    public boolean isFirstOp() {
        return m_bFirstOp;
    }

    public int getFirstOpPos() {
        return m_FirstOpVic.getPos();
    }

    /*
     * 计算剩余牌数
     * **/
    @SuppressWarnings("unchecked")
    public void calcSurplusCardList() {
        for (int i = 0; i < this.room.getPlayerNum(); i++) {
            NJPDKRoomPos roomPos = (NJPDKRoomPos) this.room.getRoomPosMgr().getPosByPosID(i);
            this.surplusCardRecordList.set(i, roomPos.cards().size());
            if (roomPos.isBaoPei) {
                baoPeiPos = roomPos.getPosID();
            }
        }
    }

    public void onOpenCard(WebSocketRequest request, CNJPDK_OpenCard openCard, long authenticatedPid) {
        if (NJPDK_define.NJPDK_GameStatus.PDK_GAME_STATUS_RESULT == this.status) {
            NJPDKRoomPos roomPos = (NJPDKRoomPos) this.room.getRoomPosMgr().getPosByPosID(openCard.pos);
            if (roomPos == null || roomPos.getPid() != authenticatedPid) {
                if (request != null) request.error(com.ddm.server.websocket.def.ErrorCode.NotAllow,
                        "cannot reveal another player's cards");
                return;
            }
            this.getRoomPlayBack().playBack2All(SNJPDK_OpenCard.make(openCard.roomID, openCard.pos, 0, roomPos.cards()));
        }
        if (null != request) request.response();
    }

    /**
     * 新增到操作链
     *
     * @param cardList
     * @param opCardType
     * @param opCardType
     */
    public void addOpCardList(List<Integer> cardList, int opCardType, int pos) {
        if (opCardType != NJPDK_CARD_TYPE.PDK_CARD_TYPE_BUCHU.value()) {
            this.cardList.add(SNJPDK_OutCardList.make(pos, opCardType, cardList));
        }
    }

    private List<List<Integer>> cardViewFor(int viewerPos) {
        List<List<Integer>> view = new ArrayList<>(room.getPlayerNum());
        for (int pos = 0; pos < room.getPlayerNum(); pos++) {
            NJPDKRoomPos roomPos = (NJPDKRoomPos) room.getRoomPosMgr().getPosByPosID(pos);
            view.add(pos == viewerPos ? List.copyOf(roomPos.cards()) : List.of());
        }
        return view;
    }

}
