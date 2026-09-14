package core.config.refdata.ref;

import com.ddm.server.common.data.RefContainer;
import com.ddm.server.common.data.RefField;


public class RefDiamondStore extends RefBaseGame {
    @RefField(iskey = true)
    public int AppID; // // ID潜规则
    public String AppName; // 游戏类型
    public int AppPrice; // 局数
    public int DiamondNum; // 消耗房卡
    public int ExtraReward; // 消耗房卡
    public String ImageName; // 消耗房卡
    public int goodsType;//商品类型
    public int channelType;//渠道类型(0安卓,1IOS,2通用)


    @Override
    public boolean Assert() {
        return true;
    }

    @Override
    public boolean AssertAll(RefContainer<?> all) {
        return true;
    }

    @Override
    public long getId() {
        return AppID;
    }
}
