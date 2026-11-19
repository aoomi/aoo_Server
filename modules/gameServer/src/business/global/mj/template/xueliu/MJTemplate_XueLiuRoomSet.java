package business.global.mj.template.xueliu;

import business.global.mj.template.MJTemplateRoom;
import business.global.mj.template.MJTemplateRoomEnum;
import business.global.mj.template.MJTemplateRoomSet;
import cenum.mj.HuType;
import jsproto.c2s.cclass.BaseSendMsg;
import jsproto.c2s.cclass.mj.template.MJTemplatePlayerPosInfo;
import jsproto.c2s.cclass.pos.PlayerPosInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public abstract class MJTemplate_XueLiuRoomSet extends MJTemplateRoomSet {
    public MJTemplate_XueLiuRoomSet(int setID, MJTemplateRoom room, int dPos) {
        super(setID, room, dPos);
    }

    /**
     * 实时算分信息
     *
     * @param kouFenPos
     * @param point
     * @param losePos
     * @return
     */
    @Override
    public List<? extends PlayerPosInfo> getPlayerInfoList(int kouFenPos, int point, int losePos) {
        List<PlayerPosInfo> playerPosInfoList = this.room.getRoomPosMgr().getPlayerPosInfoList();
        List<MJTemplatePlayerPosInfo> playerPosInfos = new ArrayList<>();
        MJTemplate_XueLiuSetPos setPos;
        for (PlayerPosInfo playerPosInfo : playerPosInfoList) {
            setPos = (MJTemplate_XueLiuSetPos) getMJSetPos(playerPosInfo.getPosID());
            MJTemplatePlayerPosInfo playerPosInfo1 = new MJTemplatePlayerPosInfo(playerPosInfo, setPos.getCurLiuShui().getPoint());
            playerPosInfos.add(playerPosInfo1);
        }
        return playerPosInfos;
    }

    /**
     * 血流玩法的没次每轮胡牌都要计算
     */
    public void calcCurRoundHuPoint() {
        List<Integer> huPostList = curRoundHuPosList();
        huPostList.stream().forEach(k -> {
            MJTemplate_XueLiuSetPos huPos = (MJTemplate_XueLiuSetPos) getMJSetPos(k);
            if (Objects.isNull(huPos)) {
                return;
            }
            //去掉手牌，改为去胡牌那边取
            huPos.cleanHandCard();
            huPos.calcPosPoint();
            huPos.setHuType(HuType.NotHu);
            if (Objects.nonNull(huPos.getLouHu())) {
                huPos.getLouHu().reSetLouHu();
            }
            huPos.setGSKH(false);
            //记录总结算
        });
        setGSP(false);
        //这个要自己改成自己的游戏类 不是每个游戏必要的  自己添加
        getRoomPlayBack().playBack2All(noticePromptly(-1, 1, -1));
        //添加当前流水
        getPosDict().values().forEach(k -> ((MJTemplate_XueLiuSetPos) k).addLiuShui());
        //计算下次胡牌序号
        calcNextHuCardType();
    }

    /**
     * 实时变更分数消息
     *
     * @param posID
     * @param point
     * @param dianPos
     * @return
     */
    public abstract BaseSendMsg noticePromptly(int posID, int point, int dianPos);

    /**
     * 计算下轮摸牌位置
     *
     * @return
     */
    public int calcHuNextPopSetPosID() {
        List<Integer> huPostList = preRoundHuPosList();
        int moPaiPos;
        //一炮三响 点炮摸牌  三人场一炮双响就点炮玩家继续摸牌
        if (huPostList.size() == 3 && getPlayerNum() - huPostList.size() == 1) {
            moPaiPos = getLastOpInfo().getLastOpPos();
            //四人场 一炮双响 轮到非点炮/胡的 那个位置
        } else if (huPostList.size() == 2 && getPlayerNum() == 4) {
            moPaiPos = posDict.keySet().stream().filter(k -> k != getLastOpInfo().getLastOpPos() && !huPostList.contains(k)).findFirst().get();
        } else {
            //玩家炮胡/自摸，默认从随机玩家开始摸牌打牌；
            moPaiPos = (huPostList.get(0) + 1) % getPlayerNum();
        }
        return moPaiPos;
    }

    /**
     * 计算接下来第几个胡
     */
    @Override
    public void calcNextHuCardType() {
        if (this.nextHuEndType.equals(MJTemplateRoomEnum.HuCardEndType.NOT)) {
            return;
        }
        int huCount = Math.toIntExact(posDict.values().stream().filter(k -> ((MJTemplate_XueLiuSetPos) k).getHuInfos().size() > 0).count());
        if (huCount == getPlayerNum()) {
            this.nextHuEndType = MJTemplateRoomEnum.HuCardEndType.NOT;
        } else {
            this.nextHuEndType = MJTemplateRoomEnum.HuCardEndType.value2Of(huCount + 1);
        }
    }

    public boolean checkXiuLiuEnd() {
        return false;
    }

    public List<Integer> curRoundHuPosList() {
        return getMHuInfo().getRoundHuPostList(curRound.getRoundID());
    }

    public List<Integer> preRoundHuPosList() {
        return getMHuInfo().getRoundHuPostList(preRound.getRoundID());
    }
}
