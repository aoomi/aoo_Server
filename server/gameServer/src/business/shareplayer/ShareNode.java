package business.shareplayer;

import lombok.Data;

import java.io.Serializable;
import com.ddm.server.common.semantic.ValueSemantics;

/**
 * @author xsj
 * @date 2020/8/7 14:32
 * @description 共享玩家节点信息
 */
@Data
@ValueSemantics
public class ShareNode implements Serializable {
    public ShareNode() {
    }

    public ShareNode(String name, String vipAddress, String ip, Integer port) {
        this.name = name;
        this.vipAddress = vipAddress;
        this.ip = ip;
        this.port = port;
    }

    //节点名称
    private String name;
    //节点地址
    private String vipAddress;
    //节点ip
    private String ip;
    //节点端口
    private Integer port;


}
