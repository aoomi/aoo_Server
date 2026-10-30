package business.global.pk;

import cenum.PKOpType;
import jsproto.c2s.cclass.mj.OpTypeInfo;
import jsproto.c2s.cclass.pk.base.BasePKSet_Pos;
import lombok.Data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Data
public abstract class AbsPKSetPosMgr {
    /**
     * 当局信息
     */
    protected AbsPKSetRoom set;

    /**
     * 动作信息列表
     */
    protected List<OpTypeInfo> opTypeInfoList = Collections.synchronizedList(new ArrayList<>());

    // 动作存储的值
    protected Map<PKOpType, Map<Integer, Integer>> opValueMap = new ConcurrentHashMap<>();

    public AbsPKSetPosMgr(AbsPKSetRoom set) {
        this.set = set;
    }

    /**
     * 获取所有玩家的牌
     */
    public abstract List<BasePKSet_Pos> getAllPlayBackNotify();

    /**
     * 获取所有玩家的牌 所有玩家的回放记录。
     *
     * @return
     */
    public List<BasePKSet_Pos> getPKAllPlayBackNotify() {
        return this.set.getPosDict().values().stream().filter(k -> null != k).map(k -> k.getPlayBackNotify()).collect(Collectors.toList());
    }

    /**
     * 清空信息
     */
    public void clear() {
        this.set = null;
        this.opTypeInfoList = null;
        this.opValueMap = null;
    }

}
