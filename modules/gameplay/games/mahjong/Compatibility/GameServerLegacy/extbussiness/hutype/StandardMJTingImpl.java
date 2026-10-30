package business.global.mj.extbussiness.hutype;

import business.global.mj.AbsMJSetPos;
import business.global.mj.MJCard;
import business.global.mj.MJCardInit;
import business.global.mj.extbussiness.StandardMJRoom;
import business.global.mj.extbussiness.StandardMJRoomEnum.OpPoint;
import business.global.mj.extbussiness.StandardMJRoomSet;
import business.global.mj.extbussiness.StandardMJSetPos;
import business.global.mj.extbussiness.dto.StandardMJPointItem;
import business.global.mj.extbussiness.dto.StandardMJTingInfo;
import business.global.mj.manage.MJFactory;
import business.global.mj.ting.AbsTing;
import business.global.mj.util.HuUtil;
import cenum.mj.MJSpecialEnum;
import com.ddm.server.common.utils.Lists;
import org.apache.commons.collections4.ListUtils;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 麻将听牌
 *
 * @author Huaxing
 */
public class StandardMJTingImpl extends AbsTing {

    @Override
    public boolean tingHu(AbsMJSetPos mSetPos, MJCardInit mCardInit) {
        if (MJFactory.getHuCard(StandardMJNormalHuCardImpl.class).checkHuCard(mSetPos, mCardInit)) {
            return true;
        }
        if (MJFactory.getHuCard(StandardMJDDHuCardImpl.class).checkHuCard(mSetPos, mCardInit)) {
            return true;
        }
        if (MJFactory.getHuCard(StandardMJSSBKImpl.class).checkHuCard(mSetPos, mCardInit)) {
            return true;
        }
        return false;
    }

    /**
     * 检测胡牌类型
     *
     * @param mSetPos   m集pos
     * @param mCardInit 米卡初始化
     */
    public boolean checkHuObj(StandardMJSetPos mSetPos, MJCardInit mCardInit) {
        return getPointItem(mSetPos, mCardInit, true) != null;
    }


    /**
     * 检查听到的牌
     *
     * @return
     */
    public boolean checkTingCardList(List<MJCard> allCardList, AbsMJSetPos setPos) {
        // 听牌
        List<Integer> tingList = absCheckTingCard(setPos, allCardList, false);
        // 判断听牌数
        return tingList.size() > 0;
    }

    @Override
    public List<Integer> checkTingCard(AbsMJSetPos mSetPos, List<MJCard> allCardList) {
        // 判断听牌数
        return absCheckTingCard(mSetPos, allCardList, true);
    }

    /**
     * 检查是否有听到牌
     *
     * @param mSetPos
     * @return
     */
    public boolean absCheckTingList(AbsMJSetPos mSetPos) {
        mSetPos.getPosOpNotice().clearTingCardMap();
        if (((StandardMJRoom) mSetPos.getRoom()).needHuInfo()) {
            ((StandardMJSetPos) mSetPos).getTingInfoListMap().clear();
        }
        List<Integer> tings = Collections.synchronizedList(Lists.newArrayList());
        // 听列表
        tings = tingList(mSetPos, tings);
        if (tings.size() > 0) {
            return true;
        }
        return false;
    }


    /**
     * 听列表
     *
     * @param lists 列表
     * @param idx   下标
     * @return
     */
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
        List<Integer> tingList = absCheckTingCard(mSetPos, allCards, cardId);
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
     * 听列表
     *
     * @param lists 列表
     *              下标
     * @return
     */
    public List<Integer> tingList(AbsMJSetPos mSetPos, List<Integer> lists) {
        // 获取所有牌
        List<MJCard> filterCardList = new ArrayList<>();
        mSetPos.allCards().forEach(n -> {
            if (filterCardList.stream().allMatch(m -> m.getType() != n.getType())) {
                filterCardList.add(n);
            }
        });
        HashMap<Integer, List<Integer>> tingCardMap = new HashMap<>(17);
        filterCardList.parallelStream().forEach(n -> {
            List<MJCard> allCards = mSetPos.allCards();
            allCards.removeIf(m -> m.getCardID().intValue() == n.getCardID());
            List<Integer> tingList = absCheckTingCard(mSetPos, allCards, n.getCardID());
            // 判断听牌数
            if (tingList.size() > 0) {
                tingCardMap.put(n.getType(), tingList);
                lists.add(n.getCardID());
            }
        });
        mSetPos.getPosOpNotice().getTingCardMap().putAll(tingCardMap);
        return lists;
    }

    /**
     * ting胡分数
     *
     * @param mSetPos   m集pos
     * @param mCardInit 米卡初始化
     * @return int
     */
    public int tingHuPoint(AbsMJSetPos mSetPos, MJCardInit mCardInit, int cardType,boolean isTingAnyCard) {
        StandardMJPointItem item;
        if(((StandardMJRoom)mSetPos.getRoom()).isTingAnyCard()){
            item = this.getPointItem((StandardMJSetPos) mSetPos, mCardInit, true,isTingAnyCard);
        }else{
            item = this.getPointItem((StandardMJSetPos) mSetPos, mCardInit, true);
        }
        if (Objects.nonNull(item)) {
            return item.getPoint();
        }
        return 0;
    }

    /**
     * 检查听到的牌
     *
     * @param mSetPos
     * @param allCardList
     * @return
     */
    public List<Integer> absCheckTingCard(AbsMJSetPos mSetPos, List<MJCard> allCardList, boolean needSetHuInfo) {
        StandardMJSetPos setPos = (StandardMJSetPos) mSetPos;
        if (((StandardMJRoom) mSetPos.getRoom()).needHuInfo() && needSetHuInfo) {
            ((StandardMJSetPos) mSetPos).getHuInfo().clear();
        }
        List<Integer> ret = Collections.synchronizedList(Lists.newArrayList());
        // 麻将牌的初始信息
        final MJCardInit mInit = mSetPos.mjCardInit(allCardList, true);
        if (null == mInit) {
            return ret;
        }
        boolean isHu;
        // 添加一张任意牌，进行测试是否能胡
        isHu = tingHu(mSetPos,
                this.newMJCardInit(mInit.getAllCardInts(), mInit.getJins(), MJSpecialEnum.NOT_JIN.value()));
        if (!isHu) {
            // 任意牌都不能出，其他牌相应的也不能胡。
            return ret;
        }
        boolean isTingAnyCard = isTingAnyCard(mInit, mSetPos);
        Map<Integer, Integer> huInfo = new ConcurrentHashMap<>();
        // 遍历其他牌
        HuUtil.CheckTypes.parallelStream().forEach(type -> {
            MJCardInit cardInit = this.newMJCardInit(mInit.getAllCardInts(), mInit.getJins(), type, mSetPos.getSet().getmJinCardInfo().checkJinExist(type));
            cardInit.setLastOutCard(type);
            if (((StandardMJRoom) mSetPos.getRoom()).needHuInfo()) {
                int point = tingHuPoint(mSetPos,cardInit, 0,isTingAnyCard);
                if (point > 0) {
                    if (!ret.contains(type)) {
                        huInfo.put(type, point);
                        ret.add(type);
                    }
                }
            } else {
                if (tingHu(mSetPos, cardInit)) {
                    if (!ret.contains(type)) {
                        ret.add(type);
                    }
                }
            }
        });
        if (((StandardMJRoom) mSetPos.getRoom()).needHuInfo() && needSetHuInfo) {
            setPos.setHuInfo(huInfo);
        }
        allCardList = null;
        return ret;
    }

    /**
     * 检查听到的牌
     *
     * @param mSetPos
     * @param allCardList
     * @return
     */
    public List<Integer> absCheckTingCard(AbsMJSetPos mSetPos, List<MJCard> allCardList, Integer cardType) {
        StandardMJSetPos setPos = (StandardMJSetPos) mSetPos;
        List<Integer> ret = Collections.synchronizedList(Lists.newArrayList());
        // 遍历其他牌
        MJCardInit mInit = mSetPos.mjCardInit(allCardList, true);
        if (null == mInit) {
            return ret;
        }
        boolean isHu = true;
        // 添加一张任意牌，进行测试是否能胡
        isHu = tingHu(mSetPos,
                this.newMJCardInit(mInit.getAllCardInts(), mInit.getJins(), MJSpecialEnum.NOT_JIN.value()));
        if (!isHu) {
            return ret;
        }
        // 遍历其他牌
        Map<Integer, Integer> pointMap = new ConcurrentHashMap<>();
        List<Integer> cardList = mInit.getAllCardInts();
        List<Integer> jin = mInit.getJins();
        boolean isTingAnyCard = isTingAnyCard(mInit, mSetPos);
        // 遍历其他牌
        HuUtil.CheckTypes.parallelStream().forEach(type -> {
            MJCardInit mjCardInit = this.newMJCardInit(cardList, jin, type, mSetPos.getSet().getmJinCardInfo().checkJinExist(type));
            mjCardInit.setLastOutCard(type);
            if (((StandardMJRoom) mSetPos.getRoom()).needHuInfo()) {
                int point = tingHuPoint(mSetPos,mjCardInit, cardType / 100,isTingAnyCard);
                if (point > 0) {
                    pointMap.put(type, point);
                    if (!ret.contains(type)) {
                        ret.add(type);
                    }
                }
            } else {
                if (tingHu(mSetPos, mjCardInit)) {
                    if (!ret.contains(type)) {
                        ret.add(type);
                    }
                }
            }
        });
        mInit = null;
        allCardList = null;
        if (((StandardMJRoom) mSetPos.getRoom()).needHuInfo() && pointMap.size()>0) {
            setPos.addTingInfoList(new StandardMJTingInfo(cardType / 100, pointMap));
        }
        return ret;
    }

    /**
     * 是否听任意牌
     * @return
     * @param mInit
     * @param mSetPos
     */
    public boolean isTingAnyCard(MJCardInit mInit, AbsMJSetPos mSetPos) {
        return false;
    }

    /**
     * @param allCardInts 所有卡整数
     * @param jins        斤
     * @param cardType    卡类型
     * @param isJin       是否金
     * @return {@link MJCardInit}
     */
    public MJCardInit newMJCardInit(List<Integer> allCardInts, List<Integer> jins, int cardType, boolean isJin) {
        MJCardInit mInit = new MJCardInit();
        mInit.addAllCardInts(allCardInts);
        mInit.addAllJins(jins);
        if (isJin) {
            mInit.addJins(cardType);
        } else {
            mInit.addCardInts(cardType);
        }
        return mInit;
    }

    /**
     * 根据可胡的胡牌牌型求最大分
     *
     * @param mSetPos
     * @param mCardInit
     * @return
     */
    public StandardMJPointItem getPointItem(StandardMJSetPos mSetPos, MJCardInit mCardInit, boolean isZiMo) {
        //判断是否存在金
        List<StandardMJPointItem> oItems = Lists.newArrayList();
        List<Integer> jinModel = mCardInit.sizeJin() > 0 ? Arrays.asList(0, mCardInit.sizeJin()) : Collections.singletonList(0);
        jinModel.forEach(jinSize -> {
            List<Integer> allCardInit = ListUtils.union((jinSize > 0 ? new ArrayList() : mCardInit.getJins()), mCardInit.getAllCardInts());
            List<Integer> jins = Collections.nCopies(jinSize, mSetPos.getSet().getmJinCardInfo().getJin(1).getType());
            MJCardInit mInit = new MJCardInit();
            mInit.addAllCardInts(allCardInit);
            mInit.addAllJins(jins);
            //检测七对
            StandardMJPointItem QDItem = (StandardMJPointItem) MJFactory.getHuCard(StandardMJDDHuCardImpl.class).checkHuCardReturn(mSetPos, mInit);
            if (Objects.nonNull(QDItem)) {
                checkOtherHuAdd(mSetPos, QDItem, isZiMo, jinSize > 0);
                oItems.add(QDItem);
            }
            //七星十三烂检查
            StandardMJPointItem QSSLItem = (StandardMJPointItem) MJFactory.getHuCard(StandardMJSSBKQingImpl.class).checkHuCardReturn(mSetPos, mInit);
            if (Objects.nonNull(QSSLItem)) {
                checkOtherHuAdd(mSetPos, QSSLItem, isZiMo, jinSize > 0);
                oItems.add(QSSLItem);
            }
            //十三烂检查
            StandardMJPointItem SSLItem = (StandardMJPointItem) MJFactory.getHuCard(StandardMJSSBKImpl.class).checkHuCardReturn(mSetPos, mInit);
            if (Objects.nonNull(SSLItem)) {
                checkOtherHuAdd(mSetPos, SSLItem, isZiMo, jinSize > 0);
                oItems.add(SSLItem);
            }
            //碰碰胡胡的检查
            StandardMJPointItem PPHItem = (StandardMJPointItem) MJFactory.getHuCard(StandardMJPPHuCardImpl.class).checkHuCardReturn(mSetPos, mInit);
            if (Objects.nonNull(PPHItem)) {
                checkOtherHuAdd(mSetPos, PPHItem, isZiMo, jinSize > 0);
                oItems.add(PPHItem);
            }
            //平胡的检查
            StandardMJPointItem item = (StandardMJPointItem) MJFactory.getHuCard(StandardMJNormalHuCardImpl.class).checkHuCardReturn(mSetPos, mInit);
            if (Objects.nonNull(item)) {
                checkOtherHuAdd(mSetPos, item, isZiMo, jinSize > 0);
                oItems.add(item);
            }
        });

        return oItems.stream().max(Comparator.comparingInt(StandardMJPointItem::getPoint)).orElse(null);
    }

    /**
     * 需要检测精吊啥的走这个方法
     * 根据可胡的胡牌牌型求最大分
     *
     * @param mSetPos
     * @param mCardInit
     * @return
     */
    public StandardMJPointItem getPointItem(StandardMJSetPos mSetPos, MJCardInit mCardInit, boolean isZiMo,boolean isTingAnyCard) {
        return null;
    }

    /**
     * 检查杠上开花
     *
     * @param mSetPos
     */
    public int checkGSKH(StandardMJSetPos mSetPos) {
        return mSetPos.isOpSize();
    }

    /**
     * 检测是否还有其他的胡类型
     *
     * @param mSetPos
     * @param item
     */
    private void checkOtherHuAdd(StandardMJSetPos mSetPos, StandardMJPointItem item, boolean isZiMo, boolean isConstantJin) {
        //检查杠上开花
        if (checkGSKH(mSetPos) > 0) {
            item.addOpPointEnum(OpPoint.GSKH.name(), OpPoint.GSKH.value());
        }
        //检测自摸
        if (isZiMo) {
            item.addOpPointEnum(OpPoint.ZiMo.name(), OpPoint.ZiMo.value());
        } else {
            //检查抢杠胡
            if (mSetPos.isQiangGangHuFlag()) {
                item.addOpPointEnum(OpPoint.QiangGangHu.name(), OpPoint.QiangGangHu.value());
            } else {
                //检测接炮
                item.addOpPointEnum(OpPoint.JiePao.name(), OpPoint.JiePao.value());
            }
        }
        //检查精吊
        if (isZiMo && mSetPos.sizeHuCardTypes() >= MJSpecialEnum.TING.value()) {
            item.addOpPointEnum(OpPoint.JingDiao.name(), OpPoint.JingDiao.value());
        }
        //德国：胡牌的时候手中没有精或精都是当本身用
        if (!isConstantJin) {
            item.addOpPointEnum(OpPoint.DG.name(), OpPoint.DG.value());
        }
        //检查天胡
        if (((StandardMJRoomSet) mSetPos.getSet()).isTiAnHuFlag()) {
            item.addOpPointEnum(OpPoint.TianHu.name(), OpPoint.TianHu.value());
        }
        //检查地胡
        if (mSetPos.getPosID() != mSetPos.getSet().getDPos() && mSetPos.isDiHuFlag()) {
            item.addOpPointEnum(OpPoint.DiHu.name(), OpPoint.DiHu.value());
        }
    }

}


