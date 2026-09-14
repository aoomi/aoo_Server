package jsproto.c2s.cclass;

import cenum.ReturnTypeEnum;

/**
 * 返回房卡
 *
 * @author Huaxing
 */
public class ReturnRoomCard {
    private ReturnTypeEnum returnType = ReturnTypeEnum.UNKWON;//1：扣除直充,2:扣除平台,3:直充、平台都扣除,4,5,41,6,32
    private int roomCard = 0;
    private int zleRoomCard = 0;
    private int clubRoomCard = 0;

    /**
     * @param roomCard    直充
     * @param zleRoomCard 平台
     * @param returnType  扣除类型
     */
    public ReturnRoomCard(int roomCard, int zleRoomCard, int clubRoomCard, ReturnTypeEnum returnType) {
        super();
        this.returnType = returnType;
        this.roomCard = roomCard;
        this.zleRoomCard = zleRoomCard;
        this.clubRoomCard = clubRoomCard;
    }

    public ReturnRoomCard() {
        super();
        this.returnType = ReturnTypeEnum.UNKWON;
        this.roomCard = 0;
        this.zleRoomCard = 0;
        this.clubRoomCard = 0;
    }

    public ReturnRoomCard(ReturnRoomCard returnRoomCard) {
        if (null == returnRoomCard) {
            return;
        }
        this.returnType = returnRoomCard.getReturnType();
        this.roomCard = returnRoomCard.getRoomCard();
        this.zleRoomCard = returnRoomCard.getZleRoomCard();
        this.clubRoomCard = returnRoomCard.getClubRoomCard();
    }

    /**
     * 设置返回的房卡
     *
     * @param roomCard
     * @param zleRoomCard
     * @param returnType
     */
    public void setReturnRoomCard(int roomCard, int zleRoomCard, int clubRoomCard, ReturnTypeEnum returnType) {
        this.returnType = returnType;
        this.roomCard = roomCard;
        this.zleRoomCard = zleRoomCard;
        this.clubRoomCard = clubRoomCard;
    }

    /**
     * 清除返回的房卡
     */
    public void cleanReturnRoomCard() {
        this.returnType = ReturnTypeEnum.UNKWON;
        this.roomCard = 0;
        this.zleRoomCard = 0;
        this.clubRoomCard = 0;
    }


    public int getRoomCard() {
        return this.roomCard;
    }

    public void setRoomCard(int roomCard) {
        this.roomCard = roomCard;
    }

    public int getZleRoomCard() {
        return this.zleRoomCard;
    }

    public void setZleRoomCard(int zleRoomCard) {
        this.zleRoomCard = zleRoomCard;
    }

    /**
     * @return returnType
     */
    public ReturnTypeEnum getReturnType() {
        return this.returnType;
    }

    /**
     * @param returnType 要设置的 returnType
     */
    public void setReturnType(ReturnTypeEnum returnType) {
        this.returnType = returnType;
    }

    /**
     * @return clubRoomCard
     */
    public int getClubRoomCard() {
        return this.clubRoomCard;
    }

    /**
     * @param clubRoomCard 要设置的 clubRoomCard
     */
    public void setClubRoomCard(int clubRoomCard) {
        this.clubRoomCard = clubRoomCard;
    }

    /* （非 Javadoc）
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "ReturnRoomCard [returnType=" + returnType + ", roomCard=" + roomCard + ", zleRoomCard=" + zleRoomCard
                + ", clubRoomCard=" + clubRoomCard + "]";
    }


}
