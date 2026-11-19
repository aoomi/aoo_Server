package jsproto.c2s.cclass.share;
import lombok.Data;

import java.io.Serializable;

/**
 * @author xsj
 * @date 2020/8/7 14:32
 * @description 共享玩家节点信息
 */
@Data
public class ShareNodeItem implements Serializable {
    /**
     * 节点名称
     */
    private String name;
    /**
     * 节点地址
     */
    private String vipAddress;
    /**
     * 节点ip
     */
    private String ip;
    /**
     * 节点端口
     */
    private Integer port;


    public ShareNodeItem(){}

    public ShareNodeItem(String ip, Integer port) {
        this.ip = ip;
        this.port = port;
    }

    public ShareNodeItem(String name, String vipAddress, String ip, Integer port) {
        this.name = name;
        this.vipAddress = vipAddress;
        this.ip = ip;
        this.port = port;
    }

    /**
     * 获取节点Id
     * @return
     */
    public String getNodeId() {
        return String.format("%s:%d",this.ip,this.port);
    }
}
