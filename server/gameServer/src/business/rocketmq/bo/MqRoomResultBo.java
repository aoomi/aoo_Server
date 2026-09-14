package business.rocketmq.bo;

import com.ddm.server.common.rocketmq.MqAbsBo;
import core.network.http.proto.SData_Result;

/**
 * @author xsj
 * @date 2020/8/13 16:18
 * @description Mq返回大厅房间对象
 */

public class MqRoomResultBo extends MqAbsBo {
    private MqAbsRequestBo mqAbsRequestBo;
    private SData_Result sData_result;

    public MqRoomResultBo() {
    }

    public MqRoomResultBo(MqAbsRequestBo mqAbsRequestBo, SData_Result sData_result) {
        this.mqAbsRequestBo = mqAbsRequestBo;
        this.sData_result = sData_result;
    }

    public MqAbsRequestBo getMqAbsRequestBo() {
        return mqAbsRequestBo;
    }

    public void setMqAbsRequestBo(MqAbsRequestBo mqAbsRequestBo) {
        this.mqAbsRequestBo = mqAbsRequestBo;
    }

    public SData_Result getsData_result() {
        return sData_result;
    }

    public void setsData_result(SData_Result sData_result) {
        this.sData_result = sData_result;
    }
}
