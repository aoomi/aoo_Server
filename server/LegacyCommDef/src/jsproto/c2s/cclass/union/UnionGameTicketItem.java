package jsproto.c2s.cclass.union;

import lombok.Data;

/**
 * 俱乐部
 */
@Data
public class UnionGameTicketItem {
    private int isminister;// 职务 0普通会员 1管理 2圈主 3联盟管理
    private double sportsPoint;
    private int gameTicket=0;
    public UnionGameTicketItem(double sportsPoint,int gameTicket,int isminister) {
        this.sportsPoint = sportsPoint;
        this.gameTicket = gameTicket;
        this.isminister = isminister;

    }
}
