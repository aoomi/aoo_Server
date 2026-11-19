package jsproto.c2s.cclass.mj;

import cenum.mj.HuType;
import cenum.mj.MJCEnum;
import cenum.mj.OpType;

/**
 * 动作类型信息
 *
 * @author Administrator
 */
public class OpTypeInfo {
    // id:排名
    private int id;
    // posId:玩家位置
    private int posId;
    // opType:动作
    private OpType opType;
    // type:操作类型,0：未操作，1：过，2：相应操作
    private int type;


    public OpTypeInfo(int posId, OpType opType) {
        super();
        this.posId = posId;
        this.opType = opType;
    }

    public OpTypeInfo(int id, int posId, OpType opType) {
        super();
        this.id = id;
        this.posId = posId;
        this.opType = opType;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getPosId() {
        return posId;
    }

    public void setPosId(int posId) {
        this.posId = posId;
    }

    public OpType getOpType() {
        return opType;
    }

    public void setOpType(OpType opType) {
        this.opType = opType;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    /**
     * 是否胡类型
     *
     * @return
     */
    public boolean isHuType() {
        return !HuType.NotHu.equals(MJCEnum.OpHuType(this.opType));
    }

    @Override
    public String toString() {
        return "OpTypeInfo [id=" + id + ", posId=" + posId + ", opType=" + opType + ", type=" + type + "]";
    }
}
