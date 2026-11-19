package business.global.mj.template.wanfa;

import business.global.mj.AbsMJSetPos;
import business.global.mj.template.MJTemplateRoomSet;
import lombok.Data;

import java.util.Set;

/**
 * @author leo_wi
 * 跟庄
 * 打同张：服务端可选包，勾选后需记录玩家在一局内打出的同一张牌张数；
 * 打同张定义：在同一局内，玩家累计打出4张一样的牌（百搭牌除外），则需要向其他三家支付一定的分数；
 * 如果是翻开的那张则累计打出3张）
 */
@Data
public class MJTemplateGenZhuang {
    /**
     * 第一个打出的牌
     */
    public int curCardType;
    /**
     * 下轮的出牌玩家
     */
    public int nextOutPos;
    /**
     * 是否不需要再检查了
     */
    public boolean checkEnd;
    /**
     * 跟庄次数
     */
    public int count;
    protected MJTemplateRoomSet set;

    public MJTemplateGenZhuang(MJTemplateRoomSet set) {
        this.set = set;
    }

    /**
     * 检查跟庄
     *
     * @return
     */
    public boolean checkGenZhuang(int type, int outPos) {
        //如果检查结束了
        if (checkEnd) {
            return false;
        }
        AbsMJSetPos setPos = set.getMJSetPos(outPos);
        //吃碰杠过 跟庄结束
        Set<Integer> buHuaTypeSet = set.getRoom().getBuHuaTypeSet();
        if (setPos.sizePublicCardList() > 0 || setPos.getOutCardIDs().stream().filter(k -> !buHuaTypeSet.contains(k / 100)).count() > 1) {
            checkEnd = true;
            return false;
        }
        //如果是庄家
        if (outPos == set.getDPos()) {
            //如果出过了又重复出 跟庄结束
            if (curCardType == type) {
                checkEnd = true;
            }
            curCardType = type;
            nextOutPos = (outPos + 1) % set.getPlayerNum();
            return false;
        }
        //如果出得牌不一致
        if (type != curCardType || nextOutPos != outPos) {
            checkEnd = true;
            return false;
        }
        nextOutPos = (outPos + 1) % set.getPlayerNum();
        //打了四个
        if (nextOutPos == set.getDPos()) {
            count++;
            return true;
        }
        return false;
    }
}
