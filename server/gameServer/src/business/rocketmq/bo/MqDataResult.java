package business.rocketmq.bo;

import com.ddm.server.common.rocketmq.MqAbsBo;
import lombok.Data;

/**
 * @author xsj
 * @date 2020/8/14 16:15
 * @description Mq同步消息返回结果
 */
@Data
public class MqDataResult extends MqAbsBo {
    private static final long serialVersionUID = 1L;
    private int code;
    private Object data;
    private String msg;
    private long custom;

    public MqDataResult() {
    }

    public MqDataResult(int code, Object data, String msg, long custom) {
        this.code = code;
        this.data = data;
        this.msg = msg;
        this.custom = custom;
    }
}
