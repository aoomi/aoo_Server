package business.global.pk;

import cenum.mj.MJSpecialEnum;
import com.alibaba.druid.util.StringUtils;
import lombok.Data;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Objects;

/**
 * 神信息
 * 只能内部测试才能使用
 *
 * @author Administrator
 */
@Data
public class PKGodInfo {
    /**
     * 扑克牌配置
     */
    protected final BasePKConfigMgr pConfigMgr;

    /**
     * 神牌Map
     */
    protected HashMap<Long, Integer> GodCardMap = new HashMap<Long, Integer>(8);

    /**
     * 当局信息
     */
    private AbsPKSetRoom set;

    public PKGodInfo(AbsPKSetRoom set, boolean isConfigName) {
        super();
        this.set = set;
        this.pConfigMgr = new BasePKConfigMgr(isConfigName ? set.getRoom().getBaseRoomConfigure().getGameType().getName().toUpperCase() : null);
    }

    public void clear() {
        if (Objects.nonNull(this.pConfigMgr)) {
            this.pConfigMgr.clear();
        }
        if (MapUtils.isNotEmpty(this.GodCardMap)) {
            this.GodCardMap.clear();
            this.GodCardMap = null;
        }
        this.set = null;
    }

    /**
     * 获取指定摸的牌(内网测试用。)
     *
     * @return
     */
    public HashMap<Long, Integer> getGodCardMap() {
        return GodCardMap;
    }

    /**
     * 叫牌(内网测试用。)
     *
     * @param pid
     * @param msg
     */
    public void addGodCardMap(long pid, String msg) {
        if (this.pConfigMgr.getGodCard() != MJSpecialEnum.GOD_CARD.value()) {
            return;
        }
        if (StringUtils.isEmpty(msg)) {
            return;
        }
        if ("x".equals(msg)) {
            this.getSet().endSet();
        }
        if (!StringUtils.isNumber(msg)) {
            return;
        }
        int cardType = Integer.parseInt(msg);
        if (cardType >= 1 && cardType < 60) {
            this.GodCardMap.put(pid, cardType);
        }
    }

    /**
     * 是否神牌模式
     *
     * @return T：神牌模式，F：普通模式
     */
    public boolean isGodCardMode() {
        return this.pConfigMgr.getGodCard() == MJSpecialEnum.GOD_CARD.value();
    }

    /**
     * 检查是否神牌模式
     *
     * @param setPos
     * @param idxPos
     * @return
     */
    public boolean isGodCard(AbsPKSetPos setPos, int idxPos) {
        if (((PKRoom) setPos.getRoom()).isPlaying(setPos.getPosID())) {
            if (this.pConfigMgr.getGodCard() == MJSpecialEnum.GOD_CARD.value()) {
                List<Integer> forceInitCard = this.pConfigMgr.getAllPrivateCard().get(idxPos);
                if (CollectionUtils.isNotEmpty(forceInitCard)) {
                    setPos.forcePopCard(this.getSet().getPKSetCard().forcePopList(forceInitCard));
                }
                return true;
            } else {
                return false;
            }
        }
        return false;
    }

    /**
     * 神牌模式下补牌
     */
    public void godCardPrivate() {
        if (this.pConfigMgr.getGodCard() == MJSpecialEnum.GOD_CARD.value()) {
            for (int idx = 0; idx < this.getSet().getRoom().getPlayerNum(); idx++) {
                AbsPKSetPos mSetPos = this.getSet().getPKSetPos(idx);
                if (mSetPos.sizePrivateCard() == this.getSet().cardSize() || !((PKRoom) mSetPos.getRoom()).isPlaying(idx)) {
                    continue;
                }
                int podIdx = this.getSet().cardSize() - mSetPos.sizePrivateCard();
                List<Integer> privateList = this.getSet().getPKSetCard().popList(podIdx);
                this.getSet().getPKSetPos(idx).forcePopCard(privateList);
            }
        }
    }

    /**
     * 摸指定的牌（仅内部测试用。）
     *
     * @param setPos 位置信息
     * @return
     */
    public int godHandCard(AbsPKSetPos setPos) {

        if (this.pConfigMgr.getGodCard() == MJSpecialEnum.GOD_CARD.value() && this.GodCardMap.containsKey(setPos.getPid())) {
            return this.getSet().getPKSetCard().appointPopCard(this.GodCardMap.get(setPos.getPid()));
        }
        return this.getSet().getPKSetCard().pop();
    }


    public AbsPKSetRoom getSet() {
        return set;
    }


}
