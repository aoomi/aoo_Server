package jsproto.c2s.iclass.union;


import lombok.Data;

/**
 * 踢出游戏
 */
@Data
public class CUnion_KickRoom extends CUnion_Base {
    /**
     * 房间key
     */
    private String roomKey;

    /**
     * 位置
     */
    private int posIndex;
}