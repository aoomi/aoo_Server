package business.rocketmq.bo;

import BaseCommon.CommLog;
import com.ddm.server.common.rocketmq.MqAbsBo;
import com.google.gson.Gson;
import jsproto.c2s.cclass.BaseSendMsg;
import lombok.Data;

import java.util.List;

/**
 * @author xsj
 * @date 2020/8/13 17:30
 * @description 请求mq通用数据
 */
@Data
public class MqClubMemberNotifyBo<T> extends MqAbsBo {
    //亲友圈ID
    private Long clubID;
    //推送玩家ID
    private Long pid;
    //推送玩家列表
    private List<Long> pidList;
    //需要判断玩家是否在房间内
    private Boolean notExistRoom;
    //需要判断玩家在亲友圈里面
    private Long signEnumClubID;
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
}
