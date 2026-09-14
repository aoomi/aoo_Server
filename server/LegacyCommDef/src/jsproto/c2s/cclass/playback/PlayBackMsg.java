package jsproto.c2s.cclass.playback;

import jsproto.c2s.cclass.BaseSendMsg;

import java.util.List;

/**
 * 回放的动作记录
 *
 * @param <T>
 * @author Huaxing
 */
public class PlayBackMsg<T> {
    private String name;            //操作名
    private BaseSendMsg res;        //结果
    private List<T> setPosCard;        //所有人的牌设置

    public PlayBackMsg(String name, BaseSendMsg res, List<T> setPosCard) {
        super();
        this.name = name;
        this.res = res;
        this.setPosCard = setPosCard;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public BaseSendMsg getRes() {
        return res;
    }

    public void setRes(BaseSendMsg res) {
        this.res = res;
    }

    public List<T> getSetPosCard() {
        return setPosCard;
    }

    public void setSetPosCard(List<T> setPosCard) {
        this.setPosCard = setPosCard;
    }

    public void clean() {
        if (null != this.setPosCard) {
            this.setPosCard.clear();
            this.setPosCard = null;
        }
        this.name = null;
        this.res = null;
    }

}
