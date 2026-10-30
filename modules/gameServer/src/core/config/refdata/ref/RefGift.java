package core.config.refdata.ref;

import com.ddm.server.common.data.RefContainer;
import com.ddm.server.common.data.RefField;

public class RefGift extends RefBaseGame {
    @RefField(iskey = true)
    public int ID; // 唯一id
    public int Type; //  扣除类型 PrizeType
    public int Num; // 扣除数量


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
        return ID;
    }
}
