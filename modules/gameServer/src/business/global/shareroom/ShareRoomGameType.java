package business.global.shareroom;

import lombok.Data;

import java.io.Serializable;

/**
 * 游戏类型
 *
 * @author Clark
 */
@Data
public class ShareRoomGameType implements Serializable {
    // ID
    private int id;
    // 名称
    private String name;
    // 类型
    private int type;

    public ShareRoomGameType(int id, String name, int type) {
        this.id = id;
        this.name = name;
        this.type = type;
    }
}
