package business.global.sharegm;

import business.shareplayer.ShareNode;
import lombok.Data;

/**
 * @author xsj
 * @date 2020/8/28 16:20
 * @description 服务器节点信息
 */
@Data
public class ShareNodeServer {
    //节点信息
    private ShareNode shareNode;
    //启动时间
    private Long startTime;
    //上一次心跳时间
    private Long lastHeartTime;
    //状态0表示正常启动-1关闭
    private Integer status;

}
