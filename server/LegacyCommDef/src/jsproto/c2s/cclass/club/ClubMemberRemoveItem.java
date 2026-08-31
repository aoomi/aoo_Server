package jsproto.c2s.cclass.club;

import lombok.Data;

import java.io.Serializable;

@Data
public class ClubMemberRemoveItem implements Serializable {
    private long memberId;
    private long pid = 0; // 玩家pid
    private long clubId;
    private int reason = 0; // 产生原因类型
    private int status = 0; // 当前剩余
    private int isminister = 0; // 职务 0普通会员 1管理 2创建者
    private long exePid;//操作者PID
    private int level;
    private long upLevelId;//
    private int creattime;// 申请时间
    private int updatetime;// 处理时间
    private int deletetime;// 踢出时间
    public static String getItemsName() {
        return "memberId,pid,clubId,exePid,deletetime";
    }
}
