package business.global.pk.xcpdk;

import business.global.room.base.AbsRoomPos;

/**
 * 吊蟹当局位置
 */
public class XCPDKSetPos {
    /**
     * 位置ID
     */
    private int posID = 0;
    /**
     * 玩家房间信息
     */
    private AbsRoomPos roomPos = null;
    /**
     * 房间当局信息
     */
    private XCPDKRoomSet set = null;
    /**
     * 用户结算分数
     */
    private int endPoint = 0;
    /**
     * 扣分
     */
    private int deductPoint = 0;
    /**
     * 一考的扣分
     */
    private double deductPointYiKao;
    /**
     * 一考的结算分
     */
    private double deductEndPoint;

    private int useTime = 0;

    @SuppressWarnings("rawtypes")
    public XCPDKSetPos(int posID, AbsRoomPos roomPos, XCPDKRoomSet set) {
        this.posID = posID;
        this.roomPos = roomPos;
        this.set = set;
    }

    public int getPosID() {
        return posID;
    }

    public void setPosID(int posID) {
        this.posID = posID;
    }

    public AbsRoomPos getRoomPos() {
        return roomPos;
    }

    public void setRoomPos(AbsRoomPos roomPos) {
        this.roomPos = roomPos;
    }

    public XCPDKRoomSet getSet() {
        return set;
    }

    public void setSet(XCPDKRoomSet set) {
        this.set = set;
    }

    public int getEndPoint() {
        return endPoint;
    }

    public void setEndPoint(int endPoint) {
        this.endPoint = endPoint;
    }

    public int getDeductPoint() {
        return deductPoint;
    }

    public void setDeductPoint(int deductPoint) {
        this.deductPoint = deductPoint;
    }

    public double getDeductPointYiKao() {
        return deductPointYiKao;
    }

    public void setDeductPointYiKao(double deductPointYiKao) {
        this.deductPointYiKao = deductPointYiKao;
    }

    public double getDeductEndPoint() {
        return deductEndPoint;
    }

    public void setDeductEndPoint(double deductEndPoint) {
        this.deductEndPoint = deductEndPoint;
    }

    public int getUseTime() {
        return useTime;
    }

    public void setUseTime(int useTime) {
        this.useTime = useTime;
    }
}
