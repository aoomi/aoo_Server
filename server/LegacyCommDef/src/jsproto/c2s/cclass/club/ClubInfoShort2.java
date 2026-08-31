package jsproto.c2s.cclass.club;

import lombok.Data;

/**
 * 俱乐部信息
 * 简化版 2021/11/1傅哥要求
 */
@Data
public class ClubInfoShort2 {

    /**
     * 俱乐部ID
     */
    private long id;

    /**
     * 俱乐部名称
     */
    private String name;
    /**
     * 随机的俱乐部标识ID
     */
    private int clubsign;

    /**
     * 赛事ID
     */
    private long unionId;
    /**
     * 皮肤类型
     */
    private int skinType;
    /**
     * 圈主名字
     */
    private String clubCreateName = "";

}
