package business.global.shareroom;

import lombok.Data;

import java.io.Serializable;

/**
 * @author xsj
 * @date 2020/8/12 9:16
 * @description 共享房间所属游戏节点信息
 */
@Data
public class ShareRoomGameBo implements Serializable {
    private long id;
    private int gametype;
    private String name = "";
    private String logoico = "";
    private String barColors = "";
    private String gameName = "";
    private int have_xifen;
    private int tab;
    private String hutypelist = "";
    private int sort;
    private int classType;
    private String webSocketUrl;
    private String httpUrl;
}
