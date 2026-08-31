package business.rocketmq.bo;

import business.shareplayer.ShareNode;
import com.ddm.server.common.rocketmq.MqAbsBo;
import lombok.Data;

/**
 * @author xsj
 * @date 2020/8/13 17:30
 * @description 请求mq通用数据
 */
@Data
public class MqAbsRequestBo extends MqAbsBo {
    //请求标识
    private long requestId;
    //玩家Id
    private long pid;
    //游戏名
    private String gameTypeName;
    //游戏Id
    private Integer gameTypeId;
    //消息内容
    private String body;
    //操作编码
    private String opcode;
    //节点
    private ShareNode shareNode;
    //请求来源节点
    private ShareNode shareNodeFrom;

    public MqAbsRequestBo() {
    }

    public MqAbsRequestBo(long pid, String gameTypeName, Integer gameTypeId, String body, String opcode, ShareNode shareNode) {
        this.pid = pid;
        this.gameTypeName = gameTypeName;
        this.gameTypeId = gameTypeId;
        this.body = body;
        this.opcode = opcode;
        this.shareNode = shareNode;
    }
}
