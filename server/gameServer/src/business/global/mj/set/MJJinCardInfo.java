package business.global.mj.set;

import business.global.mj.MJCard;
import cenum.mj.MJSpecialEnum;
import lombok.Data;
import org.apache.commons.collections4.MapUtils;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 麻将金牌信息
 *
 * @author Administrator
 */
@Data
public class MJJinCardInfo {
    /**
     * 金牌字典
     */
    private Map<Integer, MJCard> jinMap = null;

    /**
     * 开金数（要开几个金）
     */
    private int kaiJinNum;


    public MJJinCardInfo(int kaiJinNum) {
        super();
        this.setKaiJinNum(kaiJinNum);
        this.setJinMap(new HashMap<>(getKaiJinNum()));
    }

    /**
     * 添加金牌
     *
     * @return
     */
    public boolean addJinCard(MJCard mCard) {
        // 该金牌存在
        if (this.jinMap.containsKey(mCard.getType())) {
            return false;
        }
        // 添加金牌
        this.jinMap.put(mCard.getType(), mCard);
        return true;
    }

    /**
     * 检查金牌是否存在
     *
     * @param value 牌类型
     * @return
     */
    public boolean checkJinExist(int value) {
        if (value >= MJSpecialEnum.CARD_ID.value()) {
            // 传入的值类型是牌ID
            value = value / 100;
        }
        return this.jinMap.containsKey(value);
    }

    /**
     * 获取金key列表
     *
     * @return
     */
    public List<Integer> getJinKeys() {
        return MapUtils.isEmpty(this.getJinMap()) ? Collections.emptyList() : this.getJinMap().keySet().stream().collect(Collectors.toList());
    }

    /**
     * 获取金vlaue列表
     *
     * @return
     */
    public List<MJCard> getJinValues() {
        return MapUtils.isEmpty(this.getJinMap()) ? Collections.emptyList() : this.getJinMap().values().stream().collect(Collectors.toList());
    }

    /**
     * 检查是否有金牌
     */
    public boolean checkExistJin() {
        return MapUtils.isNotEmpty(this.getJinMap());
    }

    /**
     * 获取金数量
     *
     * @return
     */
    public int sizeJin() {
        return MapUtils.isEmpty(this.getJinMap()) ? 0 : this.getJinMap().size();
    }


    /**
     * 获取金
     *
     * @param num 第几个金
     * @return
     */
    public MJCard getJin(int num) {
        // 计算指定金数 -1
        int count = num - 1;
        // 下标 = 计算值 <=0?返回0:返回计算值
        int index = count <= 0 ? 0 : count;
        if (this.jinMap.size() >= (index + 1)) {
            return this.getJinValues().get(index);
        }
        return new MJCard(0);
    }


    /**
     * 获取开金数
     *
     * @return
     */
    public int getKaiJinNum() {
        return kaiJinNum;
    }

    /**
     * 清空
     */
    public void clear() {
        if (MapUtils.isNotEmpty(this.jinMap)) {
            this.jinMap.clear();
            this.jinMap = null;
        }
    }
}
