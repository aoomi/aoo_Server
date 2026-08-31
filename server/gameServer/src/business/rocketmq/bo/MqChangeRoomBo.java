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
public class MqChangeRoomBo extends MqAbsBo {
    //玩家Id
    private long pid;
    //退出游戏Id
    private Integer exitGameTypeId;
    //进入游戏Id
    private Integer enterGameTypeId;
    //进入游戏名称
    private String enterGameTypeName;
    //消息内容
    private String body;
    //操作编码
    private String opcode;
    //节点
    private ShareNode exitShareNode;
    //节点
    private ShareNode enterShareNode;
    //请求来源节点
    private ShareNode shareNodeFrom;

    public MqChangeRoomBo() {
    }

    public MqChangeRoomBo(long pid, Integer exitGameTypeId, Integer enterGameTypeId, String enterGameTypeName, String body, String opcode, ShareNode exitShareNode, ShareNode enterShareNode) {
        this.pid = pid;
        this.exitGameTypeId = exitGameTypeId;
        this.enterGameTypeId = enterGameTypeId;
        this.enterGameTypeName = enterGameTypeName;
        this.body = body;
        this.opcode = opcode;
        this.exitShareNode = exitShareNode;
        this.enterShareNode = enterShareNode;
    }
}
