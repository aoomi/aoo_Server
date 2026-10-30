package jsproto.c2s.cclass.mj;

import java.util.ArrayList;
import java.util.List;

/**
 * 红中麻将 配置
 *
 * @author Clark
 */
// 一局中各位置的信息
public class BaseMJSet_Pos {
    private int posID = -1;
    private int handCard = -1; // 没有手牌
    private List<Integer> shouCard = new ArrayList<>(); //手牌，如果不是自己，填0， 如果个数是3n+2,则独立显示手牌
    private List<Integer> outCard = new ArrayList<>(); //打出的牌，这里不过虑被人接收的
    private List<List<Integer>> publicCardList = new ArrayList<>(); // <type, fromPos, recieveCard, cardID1,card2,..>玩家亮出来的 吃1、碰2、暗杠3 明杠4  如果是暗杠 填0
    private List<Integer> huCard = new ArrayList<>(); // 可胡牌的 type 和 番数；私人独享
    private String publicCardStrs;//回放
    private Boolean isLostConnect;// T掉线：F连接
    private boolean isTrusteeship = false;
    private int point;//分数
    private Double sportsPoint;//竞技点
    private Integer secTotal;

    public int getPosID() {
        return posID;
    }

    public void setPosID(int posID) {
        this.posID = posID;
    }

    public int getHandCard() {
        return handCard;
    }

    public void setHandCard(int handCard) {
        this.handCard = handCard;
    }

    public List<Integer> getShouCard() {
        return shouCard;
    }

    public void setShouCard(List<Integer> shouCard) {
        this.shouCard.addAll(shouCard);
    }

    public List<Integer> getOutCard() {
        return outCard;
    }

    public void setOutCard(List<Integer> outCard) {
        this.outCard.addAll(outCard);
    }

    public List<List<Integer>> getPublicCardList() {
        return publicCardList;
    }

    public void setPublicCardList(List<List<Integer>> publicCardList) {
        this.publicCardList.addAll(publicCardList);
    }

    public List<Integer> getHuCard() {
        return huCard;
    }

    public void setHuCard(List<Integer> huCard) {
        this.huCard = huCard;
    }

    public String getPublicCardStrs() {
        return publicCardStrs;
    }

    public void setPublicCardStrs(String publicCardStrs) {
        this.publicCardStrs = publicCardStrs;
    }

    public Boolean getIsLostConnect() {
        return isLostConnect;
    }

    public void setIsLostConnect(Boolean isLostConnect) {
        this.isLostConnect = isLostConnect;
    }

    public void setTrusteeship(boolean trusteeship) {
        isTrusteeship = trusteeship;
    }

    public boolean isTrusteeship() {
        return isTrusteeship;
    }

    public void setPoint(int point) {
        this.point = point;
    }

    public void setSportsPoint(Double sportsPoint) {
        this.sportsPoint = sportsPoint;
    }

    public void setSecTotal(int secTotal) {
        this.secTotal = secTotal;
    }

    public int getSecTotal() {
        return secTotal;
    }
}
