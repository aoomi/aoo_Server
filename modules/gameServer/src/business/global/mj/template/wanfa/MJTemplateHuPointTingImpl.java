package business.global.mj.template.wanfa;

import business.global.mj.AbsMJSetPos;
import business.global.mj.MJCard;
import business.global.mj.MJCardInit;
import business.global.mj.manage.MJFactory;
import business.global.mj.template.MJTemplateRoom;
import business.global.mj.template.MJTemplateSetCard;
import business.global.mj.template.MJTemplateSetPos;
import business.global.mj.ting.AbsTing;
import cenum.mj.MJSpecialEnum;
import jsproto.c2s.cclass.mj.template.MJTemplateTingInfo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 听牌的时候提示胡牌分
 * 如果有听牌，回把这张牌的听牌分放进去
 */
public class MJTemplateHuPointTingImpl extends AbsTing {

    @Override
    public boolean tingHu(AbsMJSetPos mSetPos, MJCardInit mCardInit) {
        return MJFactory.getHuCard(((MJTemplateRoom) mSetPos.getRoom()).getHuCardImpl()).checkHuCard(mSetPos, mCardInit);
    }

    /**
     * 检查听到的牌
     *
     * @param mSetPos
     * @param allCardList
     * @return
     */

    public List<Integer> absCheckHuPointTingCard(AbsMJSetPos mSetPos, List<MJCard> allCardList, int cardId) {
        List<Integer> ret = new ArrayList<>();
        MJCardInit mInit = mSetPos.mjCardInit(allCardList, true);
        if (null == mInit) {
            return ret;
        }
        boolean isHu = true;
        // 添加一张任意牌，进行测试是否能胡
        MJCardInit mjCardInit = this
                .newMJCardInit(mInit.getAllCardInts(), mInit.getJins(), MJSpecialEnum.NOT_JIN.value());
        isHu = tingHu(mSetPos, mjCardInit);
        if (!isHu) {
            // 任意牌都不能出，其他牌相应的也不能胡。
            return ret;
        }
        MJTemplateSetPos setPos = (MJTemplateSetPos) mSetPos;
        int jinPoint = setPos.preCalcPosPoint(mjCardInit, MJSpecialEnum.NOT_JIN.value());
        Map<Integer, Integer> pointMap = new HashMap<>();
        if (mSetPos.getSet().getmJinCardInfo().checkExistJin()) {
            for (Integer jin : mSetPos.getSet().getmJinCardInfo().getJinKeys()) {
                pointMap.put(jin, jinPoint);
                ret.add(jin);
            }
        }
        // 遍历其他牌
        MJCardInit init;
        List<Integer> checkHuCards = setPos.getMJSetCard().checkHuCards;
        for (int type : checkHuCards) {
            if (mSetPos.getSet().getmJinCardInfo().checkJinExist(type)) {
                continue;
            } else {
                init = this.newMJCardInit(mInit.getAllCardInts(), mInit.getJins(), type);
            }
            isHu = tingHu(mSetPos, init);
            if (isHu && !ret.contains(type)) {
                ret.add(type);
                int point = setPos.preCalcPosPoint(init, type);
                pointMap.put(type, point);
            }
        }
        mInit = null;
        allCardList = null;
        setPos.addTingInfoList(new MJTemplateTingInfo(cardId / 100, pointMap));
        return ret;
    }

    /**
     * 检查是否有听到牌
     *
     * @param mSetPos
     * @return
     */
    public boolean absCheckHuPointTingList(AbsMJSetPos mSetPos) {
        mSetPos.getPosOpNotice().clearTingCardMap();
        MJTemplateSetPos setPos = (MJTemplateSetPos) mSetPos;
        setPos.getTingInfoList().clear();
        // 听列表
        tingList(mSetPos, new ArrayList<>(), 0);
        return setPos.getTingInfoList().size() > 0;

    }

    /**
     * 听列表
     *
     * @param lists 列表
     * @param idx   下标
     * @return
     */
    @Override
    public List<Integer> tingList(AbsMJSetPos mSetPos, List<Integer> lists, int idx) {
        // 获取所有牌
        List<MJCard> allCards = mSetPos.allCards();
        // 如果牌的下标 == 所有牌 -1
        if (allCards.size() == idx) {
            return lists;
        }
        // 获取牌ID
        int cardId = allCards.get(idx).cardID;
        // 移除一张牌
        allCards.remove(idx);
        // 听牌
        List<Integer> tingList;
        if (((MJTemplateRoom) mSetPos.getRoom()).isWanFaShowTingHuPoint()) {
            tingList = absCheckHuPointTingCard(mSetPos, allCards, cardId);
        } else {
            tingList = super.absCheckTingCard(mSetPos, allCards);
        }
        idx++;
        // 判断听牌数
        if (tingList.size() > 0) {
            mSetPos.getPosOpNotice().addTingCardList(cardId / 100, tingList);
            lists.add(cardId);
            return tingList(mSetPos, lists, idx);
        }
        return tingList(mSetPos, lists, idx);
    }

    /**
     * 检查听到的牌
     *
     * @param mSetPos
     * @param allCardList
     * @return
     */
    public List<Integer> absCheckHuPointTingCard(AbsMJSetPos mSetPos, List<MJCard> allCardList) {
        List<Integer> ret = new ArrayList<>();
        // 麻将牌的初始信息
        MJCardInit mInit = mSetPos.mjCardInit(allCardList, true);
        if (null == mInit) {
            return ret;
        }
        MJTemplateSetPos setPos = (MJTemplateSetPos) mSetPos;
        setPos.getHuInfo().clear();
        boolean isHu = true;
        MJCardInit mjCardInit = this.newMJCardInit(mInit.getAllCardInts(), mInit.getJins(), MJSpecialEnum.NOT_JIN.value());
        // 添加一张任意牌，进行测试是否能胡
        isHu = tingHu(mSetPos, mjCardInit);
        if (!isHu) {
            // 任意牌都不能出，其他牌相应的也不能胡。
            return ret;
        }
        int jinPoint = setPos.preCalcPosPoint(mjCardInit, MJSpecialEnum.NOT_JIN.value());
        if (mSetPos.getSet().getmJinCardInfo().checkExistJin()) {
            for (Integer jin : mSetPos.getSet().getmJinCardInfo().getJinKeys()) {
                setPos.getHuInfo().put(jin, jinPoint);
                ret.add(jin);
            }
        }
        MJCardInit init;
        // 遍历其他牌
        List<Integer> checkHuCards = setPos.getMJSetCard().checkHuCards;
        for (int type : checkHuCards) {
            if (mSetPos.getSet().getmJinCardInfo().checkJinExist(type)) {
                continue;
            } else {
                init = this.newMJCardInit(mInit.getAllCardInts(), mInit.getJins(), type);
            }
            isHu = tingHu(mSetPos, init);
            if (isHu) {
                if (!ret.contains(type)) {
                    ret.add(type);
                    int point = setPos.preCalcPosPoint(init, type);
                    setPos.getHuInfo().put(type, point);
                }
            }
        }
        mInit = null;
        allCardList = null;
        init = null;
        return ret;
    }

    @Override
    public List<Integer> checkTingCard(AbsMJSetPos mSetPos, List<MJCard> allCardList) {
        if (((MJTemplateRoom) mSetPos.getRoom()).isWanFaShowTingHuPoint()) {
            return absCheckHuPointTingCard(mSetPos, allCardList);
        }
        return absCheckTingCard(mSetPos, allCardList);
    }

    /**
     * 检查听到的牌
     *
     * @param mSetPos
     * @param allCardList
     * @return
     */
    @Override
    public List<Integer> absCheckTingCard(AbsMJSetPos mSetPos, List<MJCard> allCardList) {
        List<Integer> ret = new ArrayList<>();
        // 麻将牌的初始信息
        MJCardInit mInit = mSetPos.mjCardInit(allCardList, true);
        if (null == mInit) {
            return ret;
        }
        boolean isHu = true;
        // 添加一张任意牌，进行测试是否能胡
        isHu = tingHu(mSetPos,
                this.newMJCardInit(mInit.getAllCardInts(), mInit.getJins(), MJSpecialEnum.NOT_JIN.value()));
        if (!isHu) {
            // 任意牌都不能出，其他牌相应的也不能胡。
            return ret;
        }
        // 检查是否有金牌
        if (mSetPos.getSet().getmJinCardInfo().checkExistJin()) {
            // 添加金牌列表
            ret.addAll(mSetPos.getSet().getmJinCardInfo().getJinKeys());
        }
        // 遍历其他牌
        List<Integer> checkHuCards = ((MJTemplateSetCard) mSetPos.getMJSetCard()).checkHuCards;
        for (int type : checkHuCards) {
            // 遍历其他牌
            isHu = tingHu(mSetPos, this.newMJCardInit(mInit.getAllCardInts(), mInit.getJins(), type));
            if (isHu) {
                if (mSetPos.getSet().getmJinCardInfo().checkJinExist(type)) {
                    continue;
                } else {
                    if (!ret.contains(type)) {
                        ret.add(type);
                    }
                }

            }

        }
        mInit = null;
        allCardList = null;
        return ret;
    }

    @Override
    public boolean checkTingList(AbsMJSetPos mSetPos) {
        if (((MJTemplateRoom) mSetPos.getRoom()).isWanFaShowTingHuPoint()) {
            return absCheckHuPointTingList(mSetPos);
        }
        return absCheckTingList(mSetPos);
    }
}
