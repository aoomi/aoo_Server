package jsproto.c2s.cclass.club;

import lombok.Data;

import java.util.ArrayList;

/**
 * 俱乐部管理裂变
 *
 * @author
 */
@Data
public class ClubMemberManage {
    /**
     * 俱乐部ID
     */
    private long clubId;
    /**
     * 随机的俱乐部标识ID
     */
    private int clubsign;
    /**
     * 俱乐部名称
     */
    private String clubName;
    /**
     * 亲友圈成员信息列表
     */
    private ArrayList<ClubPlayerInfo> players = new ArrayList<ClubPlayerInfo>();
}
