package business.global.mj.scjymj;

import business.global.mj.AbsCalcPosEnd;
import business.global.mj.AbsMJSetPos;
import business.global.mj.scjymj.SCJYMJRoomEnum.SCJYMJEndType;
import business.global.mj.scjymj.SCJYMJRoomEnum.SCJYMJOpPoint;
import cenum.mj.MJHuOpType;
import cenum.mj.OpType;
import cenum.room.RoomDissolutionState;
import com.ddm.server.common.CommLogD;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static business.global.mj.scjymj.SCJYMJRoomEnum.SCJYMJPiaoWanFa.Piao_Net_Xuan;
import static business.global.mj.scjymj.SCJYMJRoomEnum.SCJYMJPiaoWanFa.Piao_Net_Zuo;

/**
 * 安岳麻将
 *
 * @author Administrator
 */
public class SCJYMJCalcPosEnd extends AbsCalcPosEnd {
    private SCJYMJSetPos mSetPos = null;
    // 动作map
    private Map<SCJYMJOpPoint, List<Integer>> huTypeMap = new ConcurrentHashMap<>();
    private SCJYMJRoom<?> room;
    private SCJYMJRoomSet set;
    // 杠翻数 + 根番数
    private int gangCount;

    public SCJYMJCalcPosEnd(SCJYMJSetPos mSetPos) {
        super(mSetPos);
        this.mSetPos = mSetPos;
        this.set = (SCJYMJRoomSet) mSetPos.getSet();
        this.room = (SCJYMJRoom<?>) mSetPos.getRoom();

    }

    /**
     * 对局结束
     * 	摸完所有牌时，未听牌，花猪玩家要接受额外惩罚；
     */
    public void setPosEnd() {
//        if (!set.isHuang()) {
//            // 没有摸完牌
//            this.calcGangPoint();
//            return;
//        }
        if (room.getRoomSelfDissolutionState() == RoomDissolutionState.Dissolution) {
            return;
        }
        if (this.mSetPos.isHu()) {
            this.calcGangPoint();
            return;
        }
        // 	查大叫：摸完所有牌时，未听牌玩家要给听牌玩家最大可能倍数（不含自摸倍数），假如我点炮给他最大要付多钱;

        if (this.mSetPos.sizeHuCardTypes() > 0) {
            this.calcGangPoint();
        }
        if (this.mSetPos.chaDaJiao()) {
            SCJYMJSetPos setPos = null;
            for (int i = 1; i < this.set.getPlayerNum(); i++) {
                int nextPos = (this.mSetPos.getPosID() + i) % this.set.getPlayerNum();
                setPos = (SCJYMJSetPos) this.set.getMJSetPos(nextPos);
                if (null == setPos || setPos.isHu()) {
                    continue;
                }
                if (this.mSetPos.isTing() && setPos.isTing()) {//互相爆听不互查
                    continue;
                }
                if (!setPos.isTing() && setPos.sizeHuCardTypes() > 0) {//不是爆听的听牌玩家不查
                    continue;
                }
                if (setPos.isTing()) {
                    setPos.opPointType(SCJYMJOpPoint.FanCha, 0);
                }
                set.getLastOpInfo().setLastOpPos(setPos.getPosID());
                this.calcHuPoint(true);
                setPos.opPointType(SCJYMJOpPoint.ChaDaJiao, 0);
                setPos.removeOpPointType(SCJYMJOpPoint.DianPao);
                this.removeOpPointType(SCJYMJOpPoint.JiePao);
            }
        }
        int size = this.mSetPos.sizeHuCardTypes();
        if (size <= 0) {
            // 	退税：摸完所有牌时，未听牌玩家，不算全部杠牌的得分；
            this.tuiShui();
            // 	查花猪（只有三房牌）：摸完所有牌时，手牌有三门花色的玩家为花猪，赔给其他所有玩家每人16分；
            this.chaHuaZhu();
        }

    }

    /**
     * 退税
     * 	退税：摸完所有牌时，未听牌玩家，不算全部杠牌的得分；
     */
    public void tuiShui() {
        // 摸完所有的牌;
        // 获取可胡的牌数;
        int size = mSetPos.sizeHuCardTypes();
        if (size <= 0) {
            // 未听牌玩家，不算全部杠牌的得分;
            int diFen = -1;
            diFen *= room.cfg.getBeishu();
            for (List<Integer> gang : this.mSetPos.getPublicCardList()) {
                int type = gang.get(0);
                int pos = gang.get(1);
                int cardType = gang.get(2) / 100;
                // 被杠位置列表
                List<Integer> gangPosList = this.set.gangPosList(cardType);
                if (null == gangPosList || gangPosList.size() <= 0) {
                    continue;
                }
                if (OpType.AnGang.value() == type) {
                    // 暗杠 4
                    this.calcGangPoint(diFen * SCJYMJRoomEnum.OpValue(SCJYMJOpPoint.AnGang), cardType, SCJYMJOpPoint.AnGang, gangPosList);
                } else if (OpType.Gang.value() == type) {
                    // 补杠
                    if (mSetPos.getGangList().contains(cardType)) {
                        // TODO 	有补杠不杠，后面可以继续杠，但不收雨钱（不算杠分）；
                    } else {
                        // 补杠 2
                        this.calcGangPoint(diFen * SCJYMJRoomEnum.OpValue(SCJYMJOpPoint.Gang), cardType, SCJYMJOpPoint.Gang, gangPosList);
                    }
                } else if (OpType.JieGang.value() == type) {
                    // 接杠 4
                    this.calcJieGangPoint(pos, diFen * SCJYMJRoomEnum.OpValue(SCJYMJOpPoint.JieGang), cardType, SCJYMJOpPoint.JieGang, gangPosList);
                }
            }
        }
    }

    /**
     * 查花猪
     * 	查花猪（只有三房牌）：摸完所有牌时，手牌有三门花色的玩家为花猪，赔给其他所有玩家每人16分；
     */
    public void chaHuaZhu() {
        Map<Integer, Long> map = this.mSetPos.allCards().stream().map(k -> k.getType() / 10).collect(Collectors.groupingBy(p -> p, Collectors.counting()));
        if (null == map || map.size() <= 0) {
            return;
        }
        // 出现3个类型
        if (map.size() >= 3) {
            // 查花猪算分
            this.calcChaHuaZhuPoint(SCJYMJRoomEnum.OpValue(SCJYMJOpPoint.ChaHuaZhu), SCJYMJOpPoint.ChaHuaZhu);
        }
    }

    /**
     * 计算平均分数
     * 算查花猪
     *
     * @param calcPoint 计算分数
     */
    private void calcChaHuaZhuPoint(int calcPoint, SCJYMJOpPoint opPoint) {
        SCJYMJSetPos mOSetPos = null;
        for (int i = 0; i < this.mSetPos.getPlayerNum(); i++) {
            mOSetPos = (SCJYMJSetPos) this.mSetPos.getMJSetPos(i);
            if (mOSetPos == null) {
                continue;
            }
            if (mOSetPos.getPid() == this.mSetPos.getPid()) {
                continue;
            } else {
                this.mSetPos.setDeductPoint(this.mSetPos.getDeductPoint() - calcPoint);
                mOSetPos.setDeductPoint(mOSetPos.getDeductPoint() + calcPoint);
            }
        }
        this.calcOpPointType(opPoint, 0);
    }

    /**
     * 算根翻
     */
    private void calcGenPoint(boolean isGen) {
        int gen = isGen ? 1 : 0;
        Map<Integer, Long> map = this.mSetPos.allCards().stream().map(k -> k.getType()).collect(Collectors.groupingBy(p -> p, Collectors.counting()));
        if (null == map || map.size() <= 0) {
            return;
        }

        for (Map.Entry<Integer, Long> entry : map.entrySet()) {
            if (entry.getValue() >= 4L) {
                gen++;
            }
        }
        List<Integer> pengList = this.mSetPos.getPublicCardList().stream().filter(k -> k.get(0) == OpType.Peng.value()).map(k -> k.get(2) / 100).collect(Collectors.toList());
        if (null != pengList && pengList.size() > 0) {
            for (Integer card : pengList) {
                if (map.containsKey(card)) {
                    gen++;
                }
            }
        }
        if (gen > 0) {
            this.addhuType(SCJYMJOpPoint.Gen, gen, SCJYMJEndType.NOT);
        }
        this.gangCount += gen;
    }

    /**
     * 杠上炮转雨*
     *
     * @return
     */
    private boolean zhuanYuGang(int calcPoint, int cardType, SCJYMJOpPoint opPoint, List<Integer> gangPosList) {
        if (!this.room.RoomCfg(SCJYMJRoomEnum.SCJYMJCfg.GangShangPaoZhuanYu)) {
            // 没有选择杠上炮转雨
            return false;
        }
        // 获取杠上炮,几响
        List<Integer> gspPos = this.set.getGangShangPaoList(cardType);
        if (null == gspPos || gspPos.size() <= 0 || gspPos.size() >= 3) {
            return false;
        }
        int gangPoint = 0;
        SCJYMJSetPos mOSetPos = null;
        for (Integer posId : gangPosList) {
            mOSetPos = (SCJYMJSetPos) this.mSetPos.getMJSetPos(posId);
            if (mOSetPos == null) {
                continue;
            }
            mOSetPos.setDeductPoint(mOSetPos.getDeductPoint() - calcPoint);
            gangPoint += calcPoint;
        }
        this.calcOpPointType(opPoint, cardType);
        int avg = gangPoint / gspPos.size();
        for (Integer posId : gspPos) {
            if (posId == this.mSetPos.getPosID()) {
                continue;
            }
            mOSetPos = (SCJYMJSetPos) this.mSetPos.getMJSetPos(posId);
            if (mOSetPos == null) {
                continue;
            }
            mOSetPos.setDeductPoint(mOSetPos.getDeductPoint() + avg);
        }
        this.calcOpPointType(SCJYMJOpPoint.GangShangPaoZhuanYu, this.set.getHuCount());
        return true;
    }

    /**
     * 杠上炮转雨
     *
     * @param cardType
     * @return
     */
    private boolean zhuanYuJieGang(int lastOpPos, int calcPoint, int cardType) {
        if (!this.room.RoomCfg(SCJYMJRoomEnum.SCJYMJCfg.GangShangPaoZhuanYu)) {
            // 没有选择杠上炮转雨
            return false;
        }
        // 获取杠上炮,几响
        List<Integer> gspPos = this.set.getGangShangPaoList(cardType);
        if (null == gspPos || gspPos.size() <= 0 || gspPos.size() >= 3) {
            return false;
        }
        SCJYMJSetPos aSetPos = (SCJYMJSetPos) this.mSetPos.getMJSetPos(lastOpPos);
        if (null == aSetPos) {
            return false;
        }
        // 接杠玩家记录
        this.calcOpPointType(SCJYMJOpPoint.JieGang, cardType);
        // 点杠玩家记录
        aSetPos.opPointType(SCJYMJOpPoint.DianGang, cardType);
        aSetPos.setDeductPoint(aSetPos.getDeductPoint() - calcPoint);

        int avg = calcPoint / gspPos.size();
        for (Integer posId : gspPos) {
            if (posId == this.mSetPos.getPosID()) {
                continue;
            }
            aSetPos = (SCJYMJSetPos) this.mSetPos.getMJSetPos(posId);
            if (aSetPos == null) {
                continue;
            }
            aSetPos.setDeductPoint(aSetPos.getDeductPoint() + avg);
        }
        this.calcOpPointType(SCJYMJOpPoint.GangShangPaoZhuanYu, this.set.getHuCount());
        return true;
    }

    /**
     * 计算杠数量
     */
    private void calcGangCount() {
        gangCount += (int) this.mSetPos.getPublicCardList().stream().filter(k -> k.get(0) == OpType.AnGang.value() || k.get(0) == OpType.Gang.value() || k.get(0) == OpType.JieGang.value()).count();
    }

    /**
     * 计算杠分
     */
    private void calcGangPoint() {
        int diFen = 1;
        diFen *= room.cfg.getBeishu();
        for (List<Integer> gang : this.mSetPos.getPublicCardList()) {
            int type = gang.get(0);
            int pos = gang.get(1);
            int cardType = gang.get(2) / 100;
            // 被杠位置列表
            List<Integer> gangPosList = this.set.gangPosList(cardType);
            if (null == gangPosList || gangPosList.size() <= 0) {
                continue;
            }
            if (OpType.AnGang.value() == type) {
                // 杠上炮转雨
                if (!this.zhuanYuGang(diFen * SCJYMJRoomEnum.OpValue(SCJYMJOpPoint.AnGang), cardType, SCJYMJOpPoint.AnGang, gangPosList)) {
                    // 暗杠 4
                    this.calcGangPoint(diFen * SCJYMJRoomEnum.OpValue(SCJYMJOpPoint.AnGang), cardType, SCJYMJOpPoint.AnGang, gangPosList);
                }
            } else if (OpType.Gang.value() == type) {
                // 补杠
                if (mSetPos.getGangList().contains(cardType)) {
                    // TODO 	有补杠不杠，后面可以继续杠，但不收雨钱（不算杠分）；
                } else {
                    // 杠上炮转雨
                    if (!this.zhuanYuGang(diFen * SCJYMJRoomEnum.OpValue(SCJYMJOpPoint.Gang), cardType, SCJYMJOpPoint.Gang, gangPosList)) {
                        // 补杠 2
                        this.calcGangPoint(diFen * SCJYMJRoomEnum.OpValue(SCJYMJOpPoint.Gang), cardType, SCJYMJOpPoint.Gang, gangPosList);
                    }
                }
            } else if (OpType.JieGang.value() == type) {
                if (!this.zhuanYuJieGang(pos, diFen * SCJYMJRoomEnum.OpValue(SCJYMJOpPoint.JieGang), cardType)) {
                    // 接杠 4
                    this.calcJieGangPoint(pos, diFen * SCJYMJRoomEnum.OpValue(SCJYMJOpPoint.JieGang), cardType, SCJYMJOpPoint.JieGang, gangPosList);
                }
            }
        }
    }


    /**
     * 计算平均分数
     * 暗杠、补杠
     *
     * @param calcPoint 计算分数
     */
    private void calcGangPoint(int calcPoint, int cardTye, SCJYMJOpPoint opPoint, List<Integer> gangPosList) {
        SCJYMJSetPos mOSetPos = null;
        for (Integer posId : gangPosList) {
            mOSetPos = (SCJYMJSetPos) this.mSetPos.getMJSetPos(posId);
            if (mOSetPos == null) {
                continue;
            }
            this.mSetPos.setDeductPoint(this.mSetPos.getDeductPoint() + calcPoint);
            mOSetPos.setDeductPoint(mOSetPos.getDeductPoint() - calcPoint);
        }
        this.calcOpPointType(opPoint, cardTye);
    }

    /**
     * 计算接杠分数
     *
     * @param calcPoint 计算分数
     */
    private void calcJieGangPoint(int lastOpPos, int calcPoint, int cardId, SCJYMJOpPoint opPoint, List<Integer> gangPosList) {
        SCJYMJSetPos aSetPos = (SCJYMJSetPos) this.mSetPos.getMJSetPos(lastOpPos);
        if (null == aSetPos) {
            return;
        }
        // 接杠玩家记录
        this.calcOpPointType(SCJYMJOpPoint.JieGang, cardId);
        // 点杠玩家记录
        aSetPos.opPointType(SCJYMJOpPoint.DianGang, cardId);
        this.mSetPos.setDeductPoint(this.mSetPos.getDeductPoint() + calcPoint);
        aSetPos.setDeductPoint(aSetPos.getDeductPoint() - calcPoint);
        for (Integer posId : gangPosList) {
            if (posId != lastOpPos) {
                aSetPos = (SCJYMJSetPos) this.mSetPos.getMJSetPos(posId);
                // 点杠玩家记录
                aSetPos.opPointType(SCJYMJOpPoint.CaGua, cardId);
                this.mSetPos.setDeductPoint(this.mSetPos.getDeductPoint() + SCJYMJRoomEnum.OpValue(SCJYMJOpPoint.CaGua));
                aSetPos.setDeductPoint(aSetPos.getDeductPoint() - SCJYMJRoomEnum.OpValue(SCJYMJOpPoint.CaGua));
            }
        }
    }

    public void calcPosPoint() {
        this.mSetPos.setEndPoint(this.mSetPos.getEndPoint() + this.mSetPos.getDeductPoint());
    }

    /**
     * 计算动作分数类型
     */
    public void calcOpPointType(SCJYMJOpPoint opPoint, int count) {
        // 添加胡类型
        this.addhuType(opPoint, count, SCJYMJEndType.NOT);
    }


    public void removeOpPointType(SCJYMJOpPoint opPoint) {
        this.huTypeMap.remove(opPoint);
    }

    /**
     * 检查是特殊牌型  杠上开花：总分数*2；  海底捞月：总分数*2；  大吊车（独钓）：总分数*2；  抢杠胡：总分数*3；
     *
     * @return
     */
    private boolean checkSpecial(SCJYMJOpPoint opPoint) {
        switch (opPoint) {
            case GSKH:
            case HDLY:
            case QGHu:
            case JinGouDiao:
                // 独张
            case DuZhang:
                // 一条龙
            case Long:
                // 板板高
            case BanBanGao:
                // 夹心五
            case JiaXinWu:
                return true;
            case GSP:
                SCJYMJSetPos aSetPos = (SCJYMJSetPos) mSetPos.getMJSetPos(mSetPos.getSet().getLastOpInfo().getLastOpPos());
                if (null != aSetPos) {
                    this.set.addGangShangPaoMap(aSetPos.getGangCardId());
                } else {
                    CommLogD.error("checkSpecial GSP getLastOpPos:{}", mSetPos.getSet().getLastOpInfo().getLastOpPos());
                }
                return true;
            default:
                break;
        }
        return false;
    }

    private void calcHu() {
        switch (this.mSetPos.getHuType()) {
            case NotHu:
            case DianPao:
                break;
            case ZiMo:
            case JiePao:
            case QGH:
                this.calcHuPoint(true);
                break;
            default:
                break;
        }
    }

    /**
     * 计算胡分
     */
    private void calcHuPoint(boolean isHu) {
        List<SCJYMJOpPoint> oEnums = this.mSetPos.getPosOpRecord().getOpHuList().stream().map(k -> (SCJYMJOpPoint) k).collect(Collectors.toList());
        if (null == oEnums || oEnums.size() <= 0) {
            return;
        }
        clearTypeMap();
        this.gangCount = 0;
        // 计算根翻
        this.calcGenPoint(oEnums.contains(SCJYMJOpPoint.Gen));
        // 计算杠翻
        this.calcGangCount();
        // 计算翻数
        int fanCount = this.gangCount;
        // 计算胡翻
        int huCount = 0;
        for (SCJYMJOpPoint oEnum : oEnums) {
            if (SCJYMJOpPoint.Gen.equals(oEnum)) {
                continue;
            }
            if (checkSpecial(oEnum)) {
                fanCount += SCJYMJRoomEnum.OpValue(oEnum);
                this.addhuType(oEnum, SCJYMJRoomEnum.OpValue(oEnum), SCJYMJEndType.NOT);
            } else {
                huCount += oEnum.value();
                this.addhuType(oEnum, oEnum.value(), SCJYMJEndType.NOT);
            }
        }


        // 胡牌分*加番
        int point = pow(huCount);
        point *= room.cfg.getBeishu();
        if (MJHuOpType.JiePao.equals(this.mSetPos.getmHuOpType())) {
            this.setDianPao();
            this.calcPaoPoint(point, fanCount);
        } else if (MJHuOpType.QGHu.equals(this.mSetPos.getmHuOpType())) {
            this.calcPaoPoint(point, fanCount);
        } else {
            if (SCJYMJRoomEnum.SCJYMJHuPai.ZiMoFan.equals(room.getHuType())) {
                List<Integer> pengList = this.mSetPos.getPublicCardList().stream().filter(k -> k.get(0) == OpType.Peng.value()).map(k -> k.get(2) / 100).collect(Collectors.toList());
                if (pengList.contains(mSetPos.getHandCard().getType())) {
                    fanCount += 1;
                    this.addhuType(SCJYMJOpPoint.ZiMoZiQG, 1, SCJYMJEndType.NOT);
                }
                // 自摸加翻
                fanCount += 1;
            }
            // 计算自摸平分
            this.calcAvgPoint(point, fanCount);
        }
    }

    private void clearTypeMap() {
        List<Integer> dianPaoList = this.huTypeMap.get(SCJYMJOpPoint.DianPao);
        List<Integer> baoTing = this.huTypeMap.get(SCJYMJOpPoint.BaoTing);
        List<Integer> fanCha = this.huTypeMap.get(SCJYMJOpPoint.FanCha);
        this.huTypeMap.clear();
        if (dianPaoList != null) {
            this.huTypeMap.put(SCJYMJOpPoint.DianPao, dianPaoList);
        }
        if (baoTing != null) {
            this.huTypeMap.put(SCJYMJOpPoint.BaoTing, baoTing);
        }
        if (fanCha != null) {
            this.huTypeMap.put(SCJYMJOpPoint.FanCha, fanCha);
        }
    }


    /**
     * 查大叫
     *
     * @param oEnums
     * @return
     */
    public static int calcDaJiaoPoint(List<SCJYMJOpPoint> oEnums) {
        // 计算翻数
        int count = 0;
        for (SCJYMJOpPoint oEnum : oEnums) {
            if (SCJYMJOpPoint.Gen.equals(oEnum)) {
                count += 1;
                continue;
            }
            if (checkDaJiaoSpecial(oEnum)) {
                count += SCJYMJRoomEnum.OpValue(oEnum);
            } else {
                if (SCJYMJOpPoint.GSP.equals(oEnum)) {
                    continue;
                }
                count += oEnum.value();
            }
        }

        // 胡牌分*加番
        int point = (int) Math.pow(2, count);
        // 胡牌分*加番      
        return point;
    }


    /**
     * 检查是特殊牌型  杠上开花：总分数*2；  海底捞月：总分数*2；  大吊车（独钓）：总分数*2；  抢杠胡：总分数*3；
     *
     * @return
     */
    private static boolean checkDaJiaoSpecial(SCJYMJOpPoint opPoint) {
        switch (opPoint) {
            case GSKH:
            case HDLY:
            case QGHu:
            case JinGouDiao:
                // 独张
            case DuZhang:
                // 一条龙
            case Long:
                // 板板高
            case BanBanGao:
                // 夹心五
            case JiaXinWu:
                return true;
            default:
                break;
        }
        return false;
    }


//    /**
//     * 算胡分
//     *
//     * @param huCount 胡翻
//     * @return
//     */
//    private int huPoint(int huCount) {
//        // 底分固定1分
//        int diFen = 1;
//        int huPoint = diFen * pow(huCount);
//        if (MJCEnum.MJHuOpType.JiePao.equals(this.mSetPos.getmHuOpType())) {
//            return huPoint;
//        } else {
//            if (SCJYMJRoomEnum.SCJYMJHuPai.ZiMoFan.equals(room.getHuType())) {
//                // 自摸加翻
//                return huPoint * 2;
//            }
//        }
//        return huPoint;
//    }


    /**
     * 计算点炮分数
     *
     * @param calcPoint
     */
    private void calcPaoPoint(int calcPoint, int fanCount) {
        SCJYMJSetPos aSetPos = (SCJYMJSetPos) this.mSetPos.getMJSetPos(this.set.getLastOpInfo().getLastOpPos());
        if (null == aSetPos) {
            return;
        }
        // （加翻+报听翻）
        int count = this.mSetPos.getTingCount() + aSetPos.getTingCount() + fanCount;
        // 胡牌分*加番
        int fanPoint = calcPoint * pow(count);
        // 获取飘分
        int piao = aSetPos.getPiao().value() + this.mSetPos.getPiao().value();

        // 番数转倍数
        piao = pow(piao);
        // 如果飘分分数 <= 0,则赋值1,否则原值,不然0乘任何数都为0
        piao = piao <= 0 ? 1 : piao;
        // 计算分数
        int point = this.calcPoint(this.mSetPos.getPosID() == this.set.getDPos() || aSetPos.getPosID() == this.set.getDPos(), this.calcFengDing(fanPoint, piao));
        this.mSetPos.setDeductPoint(this.mSetPos.getDeductPoint() + point);
        aSetPos.setDeductPoint(aSetPos.getDeductPoint() - point);
        // 点炮
        aSetPos.opPointType(SCJYMJOpPoint.DianPao, this.set.getHuCount());
        // 接炮
        calcOpPointType(SCJYMJOpPoint.JiePao, this.set.getHuCount());

    }


    /**
     * 计算平均分数
     *
     * @param calcPoint 计算分数
     */
    private void calcAvgPoint(int calcPoint, int fanCount) {
        SCJYMJSetPos mOSetPos = null;
        if (mSetPos.getPosOpRecord().getOpHuList().contains(SCJYMJOpPoint.GSKH) && SCJYMJRoomEnum.SCJYMJDianGangHua.valueOf(room.cfg.dianganghua) == SCJYMJRoomEnum.SCJYMJDianGangHua.DianPao) {
            List<Integer> gang = mSetPos.getPublicCardList().get(mSetPos.getPublicCardList().size() - 1);
            int pos = gang.get(1);
            mOSetPos = (SCJYMJSetPos) this.mSetPos.getMJSetPos(pos);
            // （加翻+报听翻）
            int count = this.mSetPos.getTingCount() + mOSetPos.getTingCount() + fanCount;
            // 胡牌分*加番
            int fanPoint = calcPoint * pow(count);
            // 获取飘分
            int piao = mOSetPos.getPiao().value() + this.mSetPos.getPiao().value();
            // 番数转倍数
            piao = pow(piao);
            // 如果飘分分数 <= 0,则赋值1,否则原值,不然0乘任何数都为0
            piao = piao <= 0 ? 1 : piao;
            // 计算分数
            int point = this.calcPoint(this.mSetPos.getPosID() == this.set.getDPos() || mOSetPos.getPosID() == this.set.getDPos(), this.calcFengDing(fanPoint, piao));
            this.mSetPos.setDeductPoint(this.mSetPos.getDeductPoint() + point);
            mOSetPos.setDeductPoint(mOSetPos.getDeductPoint() - point);
        } else {
            for (int i = 0; i < this.mSetPos.getPlayerNum(); i++) {
                mOSetPos = (SCJYMJSetPos) this.mSetPos.getMJSetPos(i);
                if (mOSetPos == null) {
                    continue;
                }
                if (mOSetPos.isHu()) {
                    // 跳过胡牌玩家
                    continue;
                }
                if (mOSetPos.getPid() == this.mSetPos.getPid()) {
                    continue;
                } else {
                    // （加翻+报听翻）
                    int count = this.mSetPos.getTingCount() + mOSetPos.getTingCount() + fanCount;
                    // 胡牌分*加番
                    int fanPoint = calcPoint * pow(count);
                    // 获取飘分
                    int piao = mOSetPos.getPiao().value() + this.mSetPos.getPiao().value();
                    // 番数转倍数
                    piao = pow(piao);
                    // 如果飘分分数 <= 0,则赋值1,否则原值,不然0乘任何数都为0
                    piao = piao <= 0 ? 1 : piao;
                    // 计算分数
                    int point = this.calcPoint(this.mSetPos.getPosID() == this.set.getDPos() || mOSetPos.getPosID() == this.set.getDPos(), this.calcFengDing(fanPoint, piao));
                    this.mSetPos.setDeductPoint(this.mSetPos.getDeductPoint() + point);
                    mOSetPos.setDeductPoint(mOSetPos.getDeductPoint() - point);
                }
            }
        }
        this.calcOpPointType(SCJYMJOpPoint.ZiMo, this.set.getHuCount());
    }


    /**
     * 计算封顶分数
     */
    private int calcFengDing(int calcPoint, int piao) {
        // 封顶
        SCJYMJRoomEnum.SCJYMJFengDing fengDing = this.room.getFengDing();
        // 飘玩法
        SCJYMJRoomEnum.SCJYMJPiaoWanFa piaoWanFa = this.room.getPiaoWanFa();
        int point = this.pow(fengDing.value);
        point *= room.cfg.getBeishu();
        if (Piao_Net_Xuan.equals(piaoWanFa) || Piao_Net_Zuo.equals(piaoWanFa)) {
            // 	飘在内：封顶包含飘（杠分，庄家分另外算）；
            int count = calcPoint * piao;
            if (count >= point) {
                // 如果胡牌分*加番*飘番 >= 封顶
                // 直接返回封顶分数
                return point;
            } else {
                // 返回原分数
                return count;
            }
        } else {
            // 	飘在外：封顶不含飘分，另外加算飘分（杠分，庄家分另外算）；
            if (calcPoint >= point) {
                return point * piao;
            } else {
                return calcPoint * piao;
            }
        }
    }

    /**
     * 2^x
     *
     * @param value 值
     * @return
     */
    private int pow(int value) {
        return (int) Math.pow(2, value);
    }

    /**
     * 添加胡类型
     *
     * @param opPoint
     * @param point
     * @param bEndType
     */
    private void addhuType(SCJYMJOpPoint opPoint, int point, SCJYMJEndType bEndType) {
        if (this.huTypeMap.containsKey(opPoint)) {
            if (!this.huTypeMap.get(opPoint).contains(point)) {
                this.huTypeMap.get(opPoint).add(point);
            }
        } else {
            List<Integer> arrays = new ArrayList<>();
            arrays.add(point);
            this.huTypeMap.put(opPoint, arrays);
        }
    }

    public CalcPosEnd getCalcPosEnd() {
        return new CalcPosEnd(this.huTypeMap);
    }

    /**
     * 计算分数
     *
     * @param isZhuang  T:庄家，F:闲家
     * @param calcPoint 胡牌分*加番*飘番+杠分
     * @return
     */
    public int calcPoint(boolean isZhuang, int calcPoint) {
        int point = calcPoint;
        if (MJHuOpType.ZiMo.equals(this.mSetPos.getmHuOpType()) && SCJYMJRoomEnum.SCJYMJHuPai.ZiMoDi.equals(room.getHuType())) {
            point += room.cfg.getBeishu();
        }
        if (this.room.getOther(SCJYMJRoomEnum.SCJYMJQiTa.ZhuangXian)) {
            return isZhuang ? point + room.cfg.getBeishu() : point;
        } else {
            return point;
        }
    }

    @Override
    public int calcPoint(boolean isZhuang, Object... params) {
        return 0;
    }

    @Override
    public <T> void calcOpPointType(T opType, int count) {

    }

    @Override
    public void calcPosEnd(AbsMJSetPos mSetPos) {
        // 计算胡分数
        this.calcHu();
    }

    @Override
    public void calcPosPoint(AbsMJSetPos mSetPos) {

    }

    private class CalcPosEnd {
        @SuppressWarnings("unused")
        private Map<SCJYMJOpPoint, List<Integer>> huTypeMap = new HashMap<>();

        public CalcPosEnd(Map<SCJYMJOpPoint, List<Integer>> huTypeMap) {
            this.huTypeMap = huTypeMap;
        }
    }

    public int getOpPintCount(SCJYMJOpPoint opPoint) {
        List<Integer> list = this.huTypeMap.get(opPoint);
        if (null == list || list.size() <= 0) {
            return 0;
        }
        return list.size();
    }

}
