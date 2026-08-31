package business.global.mj.set;

import business.global.mj.AbsMJSetRoom;
import cenum.mj.OpType;
import lombok.Data;

import java.util.HashMap;
import java.util.Map;

/**
 * 最近操作的信息
 * 记录信息
 * 最后一个被打出的牌，最后一个被杠的牌，记录当前最后（操作）玩家位置，记录当前最后（打牌）玩家位置，记录最后操作时间，记录是否海底捞月
 *
 * @author Administrator
 */
@Data
public class LastOpInfo {
    /**
     * 最后一个被打出的牌
     */
    protected int lastOutCard = 0;
    /**
     * 最后一个被杠的牌
     */
    protected int lastOpCard = 0;
    /**
     * 记录当前最后（操作）玩家位置
     */
    protected int lastOpPos = -1;
    /**
     * 记录当前最后（打牌）玩家位置
     */
    protected int lastOutCardPos = -1;
    /**
     * 记录最后操作时间
     */
    protected int lastShotTime = 0;
    /**
     * 记录是否海底捞月
     */
    protected boolean isHaiDiLao = false;
    /**
     * 记录最后（操作动作）玩家位置
     */
    protected int lastOpOutPos = -1;
    /**
     * 摸牌位置
     */
    protected int lastPopPos = -1;
    /**
     * 最近操作类型项map
     */
    private Map<OpType, LastOpTypeItem> lastOpItemMap = new HashMap<>(6);
    /**
     * 补杠动作记录
     */
    private Map<Integer, OpType> buGangMap = new HashMap<>();

    /**
     * 记录最近一次杠牌操作信息
     */
    private LastOpGangItem lastOpGangItem = new LastOpGangItem();

    /**
     * 当局信息
     */
    private AbsMJSetRoom set;

    public LastOpInfo(AbsMJSetRoom set) {
        super();
        this.set = set;
    }

    /**
     * 当前最近一次补杠的杠牌(抢杠胡的情况下。)
     *
     * @return
     */
    public int getLastOpCard() {
        return lastOpCard;
    }


    /**
     * 接手并清除上次打出的牌
     */
    public void clearLastOutCard() {
        if (this.lastOutCard <= 0) {
            // 清除动作的牌
            return;
        }
        // 原出牌人公牌要清除此牌
        int ownnerPos = this.getSet().getMJSetCard().getCardByID(this.lastOutCard).ownnerPos;
        this.getSet().getMJSetPos(ownnerPos).removeOutCardIDs(this.lastOutCard);
        this.lastOutCard = 0;
    }

    /**
     * 获取最近一次操作的牌
     *
     * @return
     */
    public int getLastOpPos() {
        // 当前最后（操作）玩家位置，(因为抢杠胡会初始lastOpPos，造成接杠杠上开花出现问题，所以加了判断。)
        if (this.lastOpPos < 0) {
            // 当前最后（打牌）玩家位置
            return this.lastOutCardPos;
        }
        return lastOpPos;
    }

    /**
     * 清空最后一次操作的牌和位置ID
     */
    public void clearLast() {
        // 最后一个被杠的牌
        this.lastOpCard = 0;
        this.lastOpPos = -1;
    }

    /**
     * 添加最近操作项
     *
     * @param opType 操作类型
     * @param item   最近动作操作项
     */
    public void addLastOpItem(OpType opType, LastOpTypeItem item) {
        this.getLastOpItemMap().put(opType, item);
    }

    /**
     * 获取指定动作类型最近操作项
     *
     * @param opType 操作类型
     * @return
     */
    public LastOpTypeItem getLastOpTypeItem(OpType opType) {
        return this.getLastOpItemMap().get(opType);
    }


    /**
     * 增加补杠记录。
     *
     * @param cardType 牌类型
     * @param opType   动作类型
     */
    public void addBuGang(int cardType, OpType opType) {
        this.buGangMap.put(cardType, opType);
    }

    /**
     * 检查补杠是否可以操作。
     *
     * @param cardType 牌类型
     * @param opType   动作类型
     * @return
     */
    public boolean checkBuGang(int cardType, OpType opType) {
        OpType oType = this.buGangMap.get(cardType);
        if (null == oType) {
            // 不存在记录
            return false;
        }
        // 操作牌类型的动作记录存在
        if (opType.equals(oType)) {
            return true;
        }
        // 不存在
        return false;
    }

}
