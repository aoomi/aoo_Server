package jsproto.c2s.cclass;

import lombok.Data;

import java.io.Serializable;

/**
 * @author xsj
 * @date 2020/8/24 16:42
 * @description 游戏连接地址
 */
@Data
public class GameTypeUrl implements Serializable {
    //游戏类型
    private int gametype;
    //websocket地址
    private String webSocketUrl;
    //http地址
    private String httpUrl;
    //服务器ip
    private String gameServerIP;
    //服务器端口
    private int gameServerPort;
    //是否启动 T:正常启动，F:关闭状态
    private boolean isStart = true;
}
