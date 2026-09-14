package business.rocketmq.bo;

import BaseCommon.CommLog;
import com.ddm.server.common.rocketmq.MqAbsBo;
import com.google.gson.Gson;
import jsproto.c2s.cclass.BaseSendMsg;
import lombok.Data;

/**
 * @author xsj
 * @date 2020/8/21 17:30
 * @description 请求玩家mq通用数据
 */
@Data
public class MqPlayerPushProtoBo<T> extends MqAbsBo {
    //推送玩家ID
    private Long pid;
    //通知消息内容
    private T baseSendMsg;
    //通知类
    private String baseSendMsgClassType;

    public BaseSendMsg getBaseSendMsgT() {
        try {
            Class clazz = Class.forName(baseSendMsgClassType);
            Gson gson = new Gson();
            return (BaseSendMsg) gson.fromJson(gson.toJson(baseSendMsg), clazz);
        } catch (ClassNotFoundException e) {
            System.getLogger("legacy").log(System.Logger.Level.ERROR, "Legacy operation failed", e);
            CommLog.error(e.getMessage(), e);
        }
        return null;
    }

    public MqPlayerPushProtoBo(Long pid, T baseSendMsg, String baseSendMsgClassType) {
        this.pid = pid;
        this.baseSendMsg = baseSendMsg;
        this.baseSendMsgClassType = baseSendMsgClassType;
    }
}
