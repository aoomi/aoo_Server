package core.config.refdata.ref;

import com.ddm.server.common.data.RefContainer;
import com.ddm.server.common.data.RefField;

public class RefTrusteeshipTime extends RefBaseGame {

    @RefField(iskey = true)
    public int id;            // ID潜规则
    public String GameType;    //游戏类型
    public int ClientTime;    //客户端
    public int ServerTime;    //服务端

    @Override
    public long getId() {
        return id;
    }

    @Override
    public boolean Assert() {
        // TODO Auto-generated method stub
        return true;
    }

    @Override
    public boolean AssertAll(RefContainer<?> all) {
        // TODO Auto-generated method stub
        return true;
    }

}
