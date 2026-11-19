package business.global.pk;

import com.ddm.server.common.CommLogD;
import com.ddm.server.common.utils.Lists;
import lombok.Data;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.io.*;
import java.util.List;

@Data
public class PKCurOutCardInfo implements Cloneable, Serializable {
    /**
     * 最后打牌的玩家位置
     */
    private int outCardPos = -1;
    /**
     * 出牌类型
     */
    private int outCardType = -1;
    /**
     * 当前打出的牌
     */
    private List<Integer> curOutCards = Lists.newArrayList();
    /**
     * 赖子牌代替
     */
    private List<Integer> substituteCard;
    /**
     * 比较值
     */
    private int compValue = 0;
    /**
     * 比较的数量
     */
    private int compSize = 0;
    /**
     * 最后打牌的玩家位置 是否已经出完牌
     */
    private boolean finishFlag = false;
    /**
     * 比较的类型长度（例如：33334444 类型长度=4）
     */
    private int compareLength = 0;

    /**
     * 检查存在必出牌
     */
    public boolean checkExistMustComeOutCard(int posId) {
        return (this.getOutCardPos() <= -1 || this.getOutCardType() <= -1) || this.getOutCardPos() == posId;
    }

    /**
     * 检查打牌后是否没有记录数据
     *
     * @param posId 操作者位置
     * @return
     */
    public boolean checkExistOurCardErrorInfo(int posId) {
        return getOutCardPos() <= -1 || CollectionUtils.isEmpty(getCurOutCards()) || getOutCardPos() != posId;
    }

    /**
     * 清空当前打出的牌
     */
    public void clearCurOutCard() {
        this.outCardPos = -1;
        this.outCardType = -1;
        this.curOutCards.clear();
        this.compValue = 0;
        this.compSize = 0;
    }


    /**
     * 设置当前打出的操作。
     *
     * @param outCardPos
     * @param outCardType
     * @param curOutCards
     */
    public boolean setCurOutCards(int outCardPos, int outCardType, List<Integer> curOutCards, int compValue) {
        this.outCardPos = outCardPos;
        this.outCardType = outCardType;
        this.curOutCards = curOutCards;
        this.compValue = compValue;
        return true;
    }

    /**
     * 设置当前打出的操作。
     *
     * @param outCardPos
     * @param outCardType
     * @param curOutCards
     */
    public boolean setCurOutCards(int outCardPos, int outCardType, List<Integer> curOutCards, int compValue, int compSize) {
        this.outCardPos = outCardPos;
        this.outCardType = outCardType;
        this.curOutCards = curOutCards;
        this.compValue = compValue;
        this.compSize = compSize;
        return true;
    }

    /**
     * 设置当前打出的操作。
     *
     * @param outCardPos
     * @param outCardType
     * @param curOutCards
     */
    public boolean setCurOutCards(int outCardPos, int outCardType, List<Integer> curOutCards, int compValue, int compSize, int compareLength) {
        this.outCardPos = outCardPos;
        this.outCardType = outCardType;
        this.curOutCards = curOutCards;
        this.compValue = compValue;
        this.compSize = compSize;
        this.compareLength = compareLength;
        return true;
    }


    /**
     * 设置当前打出的操作。
     *
     * @param outCardPos
     * @param outCardType
     * @param curOutCards
     */
    public boolean setCurOutCards(int outCardPos, int outCardType, List<Integer> curOutCards, List<Integer> substituteCard, int compValue) {
        this.outCardPos = outCardPos;
        this.outCardType = outCardType;
        this.curOutCards = curOutCards;
        this.substituteCard = substituteCard;
        this.compValue = compValue;
        return true;
    }

    /**
     * 设置当前打出的操作。
     *
     * @param outCardPos
     * @param outCardType
     * @param curOutCards
     */
    public boolean setCurOutCards(int outCardPos, int outCardType, List<Integer> curOutCards, List<Integer> substituteCard, int compValue, int compSize) {
        this.outCardPos = outCardPos;
        this.outCardType = outCardType;
        this.curOutCards = curOutCards;
        this.substituteCard = substituteCard;
        this.compValue = compValue;
        this.compSize = compSize;
        return true;
    }

    @Override
    public String toString() {
        return "PKCurOutCardInfo{" +
                "outCardPos=" + outCardPos +
                ", outCardType=" + outCardType +
                ", curOutCards=" + curOutCards +
                ", compValue=" + compValue +
                ", compSize=" + compSize +
                '}';
    }

    /**
     * 对象之间的浅克隆【只负责copy对象本身，不负责深度copy其内嵌的成员对象】
     *
     * @return
     */
    @Override
    public PKCurOutCardInfo clone() {
        try {
            return (PKCurOutCardInfo) super.clone();
        } catch (CloneNotSupportedException ex) {
            CommLogD.error(ex.getClass() + ":" + ex.getMessage());
        }
        return null;
    }

    /**
     * 实现对象间的深度克隆【从外形到内在细胞，完完全全深度copy】
     *
     * @return
     */
    public PKCurOutCardInfo deepClone() {
        // Anything 都是可以用字节流进行表示，记住是任何！
        PKCurOutCardInfo cookBook = null;
        try {

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            // 将当前的对象写入baos【输出流 -- 字节数组】里
            oos.writeObject(this);

            // 从输出字节数组缓存区中拿到字节流
            byte[] bytes = baos.toByteArray();

            // 创建一个输入字节数组缓冲区
            ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
            // 创建一个对象输入流
            ObjectInputStream ois = new ObjectInputStream(bais);
            // 下面将反序列化字节流 == 重新开辟一块空间存放反序列化后的对象
            cookBook = (PKCurOutCardInfo) ois.readObject();

        } catch (Exception e) {
            CommLogD.error(e.getClass() + ":" + e.getMessage());
        }
        return cookBook;
    }

    @Override
    public int hashCode() {
        return HashCodeBuilder.reflectionHashCode(this);
    }


    @Override
    public boolean equals(Object obj) {
        return EqualsBuilder.reflectionEquals(this, obj);
    }
}
