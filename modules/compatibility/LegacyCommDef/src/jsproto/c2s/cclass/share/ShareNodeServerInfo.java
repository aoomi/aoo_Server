package jsproto.c2s.cclass.share;


import lombok.Builder;
import lombok.Data;

import java.io.Serializable;

/**
 * @author xsj
 * @date 2020/8/28 16:20
 * @description 服务器节点信息
 */
@Data
@Builder
public class ShareNodeServerInfo implements Serializable {
    /**
     * 节点服务id
     */
    private String id;
    /**
     * 节点信息
     */
    private ShareNodeItem shareNode;
    /**
     * 启动时间
     */
    private Long startTime;
    /**
     * 上一次心跳时间
     */
    private Long lastHeartTime;
    /**
     * 状态0表示正常启动-1关闭
     */
    private Integer status;
    /**
     * 玩家数
     */
    private int playerSize;


    public ShareNodeServerInfo() {
    }

    @Builder(toBuilder = true)
    public ShareNodeServerInfo(String id, ShareNodeItem shareNode, Long startTime, Long lastHeartTime, Integer status, int playerSize) {
        this.id = id;
        this.shareNode = shareNode;
        this.startTime = startTime;
        this.lastHeartTime = lastHeartTime;
        this.status = status;
        this.playerSize = playerSize;
    }

}
