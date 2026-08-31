package jsproto.c2s.cclass.room;

import lombok.Data;

/**
 * 练习场项
 *
 * @author Administrator
 */
@Data
public class GoldItem {
    private long id;//ID
    private int playerNum;//玩家人数

    public GoldItem() {
        super();
    }

    public GoldItem(long id, int playerNum) {
        super();
        this.id = id;
        this.playerNum = playerNum;
    }


}
