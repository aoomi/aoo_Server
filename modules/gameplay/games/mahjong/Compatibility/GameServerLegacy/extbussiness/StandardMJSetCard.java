package business.global.mj.extbussiness;

import business.global.mj.AbsMJSetCard;
import business.global.mj.AbsMJSetRoom;
import business.global.mj.MJCard;
import business.global.mj.RandomCard;
import business.global.room.mj.MahjongRoom;
import cenum.mj.MJCardCfg;
import com.ddm.server.common.utils.CollectionUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 麻将 每一局麻将底牌信息 抓牌人是逆时针出手 牌是顺时针被抓
 *
 * @author Huaxing
 * @apiNote 子游戏请继承这个类
 */
public class StandardMJSetCard extends AbsMJSetCard {
    public StandardMJRoomSet set;
    private List<Integer> maPaiList = new ArrayList<>();
    private HashMap<Integer, List<Integer>> maPaiMap = new HashMap<>();

    public StandardMJSetCard(StandardMJRoomSet set) {
        this.set = set;
        this.room = set.getRoom();
        this.randomCard();
        // 系数牌1、5、9，东风、红中，对应庄家位置；
        this.maPaiMap.put(0, Arrays.asList(11, 15, 19, 21, 25, 29, 31, 35, 39, 41, 45));
        // 系数牌2、6，南风，发财，对应庄家下家位置；
        this.maPaiMap.put(1, Arrays.asList(12, 16, 22, 26, 32, 36, 42, 46));
        // 系数牌3、7，西风，白板，对应庄家对家位置；
        this.maPaiMap.put(2, Arrays.asList(13, 17, 23, 27, 33, 37, 43, 47));
        // 系数牌4、8，北风，对应庄家上家位置；
        this.maPaiMap.put(3, Arrays.asList(14, 18, 24, 28, 34, 38, 44));
    }

    @Override
    public void initDPos(AbsMJSetRoom set) {
        super.initDPos(set);
        this.room.setDPos(set.getDPos());
        StandardMJRoom room = (StandardMJRoom) this.set.getRoom();
        if(room.getLastPosId()==-1){
            room.setLastPosId((this.room.getDPos()+room.getPlayerNum()-1)%room.getPlayerNum());
        }
    }

    /**
     * 洗牌
     */
    @Override
    public void randomCard() {
        List<MJCardCfg> mCfgs = new ArrayList<MJCardCfg>();
        mCfgs.add(MJCardCfg.WANG);
        mCfgs.add(MJCardCfg.TIAO);
        mCfgs.add(MJCardCfg.TONG);
        mCfgs.add(MJCardCfg.FENG);
        mCfgs.add(MJCardCfg.JIAN);
        mCfgs.add(MJCardCfg.BAI);
        mCfgs.add(MJCardCfg.HUA);
        this.setRandomCard(new RandomCard(mCfgs, this.room.getPlayerNum(), this.room.getXiPaiList().size()));
        this.initDPos(this.set);
    }

    @Override
    protected boolean firstRandomDPos() {
        return true;
    }

    @Override
    public MJCard pop(boolean isNormalMo, int cardType) {
        int sizeCard = this.randomCard.getSize();
        if (sizeCard <= set.getLeftSize()) {
            return null;
        }
        MJCard ret = this.getGodCard(cardType);
        ret = null != ret ? ret : this.randomCard.removeLeftCards(0);
        if (isNormalMo) {
            this.randomCard.setNormalMoCnt(this.randomCard.getNormalMoCnt() + 1);
        } else {
            this.randomCard.setGangMoCnt(this.randomCard.getGangMoCnt() + 1);
        }
        return ret;
    }

    /**
     * 出马牌
     */
    public List<Integer> popMaPai() {
        return popListBySize(((StandardMJRoom) room).getMaiMa());
    }

    public List<Integer> popListBySize(int sizeMaPai) {
        List<Integer> popList = new ArrayList<>();
        int popMaSize;
        if (sizeMaPai > 0) {
            int leftSize = this.randomCard.getSize();
            popMaSize = sizeMaPai >= leftSize ? leftSize : sizeMaPai;
            if (popMaSize > 0) {
                popList = randomCard.getLeftCards().subList(0, popMaSize).stream().map(MJCard::getCardID).collect(Collectors.toList());
            }
        }

        if (CollectionUtils.isEmpty(popList)) {
            return popList;
        }

        for (Integer item : popList) {
            if (this.randomCard.getSize() <= 0) {
                continue;
            }
            // 移除一张牌
            this.randomCard.getLeftCards().removeIf(z -> z.getCardID() == item.intValue());
        }
        // 获取增加马牌
        this.maPaiList = new ArrayList<>(popList);
        return this.maPaiList;
    }

    /**
     * 是否中码
     *
     * @param room
     * @param posID
     * @param cardType
     * @return
     */
    public boolean isZhongMa(MahjongRoom room, int posID, int cardType) {
        int findPosID = (posID + room.getPlayerNum() - ((StandardMJRoomSet) room.getCurSet()).getDPos()) % room.getPlayerNum();
        if (room.getPlayerNum() == 2) {
            //庄和庄下
            return maPaiMap.get(findPosID).contains(cardType);
        } else if (room.getPlayerNum() == 3) {
            //庄和庄上和庄对
            if (findPosID == 1) {
                return maPaiMap.get(2).contains(cardType);
            }else if(findPosID==2){
                return maPaiMap.get(3).contains(cardType);
            }
        }
        return maPaiMap.get(findPosID).contains(cardType);
    }
}										
