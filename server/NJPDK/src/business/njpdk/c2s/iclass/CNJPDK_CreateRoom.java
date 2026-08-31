package business.njpdk.c2s.iclass;

import com.ddm.server.common.CommLogD;
import jsproto.c2s.cclass.room.BaseCreateRoom;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import java.io.*;
import java.util.ArrayList;

/**
 * 安岳跑的快创建房间接收实体
 * <pre>
 *   局数 8（默认）-10-16
 *   人数 2 - 3 - 4
 *   出牌玩家 首局拥有黑桃3的先出（默认） 每局拥有黑桃3的先出 每局随机选玩家，文档的字段叫出牌
 *   首出的牌 带黑桃3 任意牌
 *   可选玩法 记牌器 显示余牌 两张报对（放走包赔）
 *   两张报对（放走包赔）（勾选后玩家剩余两张牌时需进行报警；（我们现在是单张报牌放走包赔）（同时如果玩家首出牌时有单牌或其他非对子牌，此时出对子导致下家直接过牌则需要包分；如果上家出对子，下家剩余两张牌，则手上有对子需要从最大的开始出；如果出非最大的导致下家过牌则需要包分））
 *   单张包赔（如果下家剩余一张牌，此时玩家手上有非单牌，则首出牌不能出单牌，否则下家下家出完则需要包分；如果上家出单牌，下家剩余1张牌，则需要从最大的单牌开始出，如果出非最大的导致下家过牌则需要包分；）
 *   炸弹分数 炸弹不翻倍（默认） 炸弹翻倍（4炸封顶）炸弹加10分 针对所有玩家
 *   限时操作 15秒 60秒 不托管 (0:15秒 1:60秒 2:不托管)
 *   大小关 50/30 30/20 大关指：一张牌都没出的关门； 小关指：牌局结束时只出了一张牌；
 *   牌型 允许3不带（最后一手）（默认） 允许3带1 允许3带一对 允许3带2单牌 可出姊妹对（飞机44 55）
 *   特殊 3张A 1张2直接赢
 *   高级选项 同ip 不同ip
 *   解散 30秒,1分钟,3分钟,5分钟,不可解散
 *  </pre>
 *
 * @author zaf
 */
public class CNJPDK_CreateRoom extends BaseCreateRoom implements Cloneable, Serializable {
    public int chupai = 0; // 0:首局拥有黑桃3的先出 1:每局拥有黑桃3的先出 2:每局随机选玩家
    public int heitaosanbichu = 0; // 0:带黑桃3 1:任意牌
    public int zhadan;  // 0:炸弹不翻倍 1:炸弹翻倍（4炸封顶）2:炸弹加10分
    public int daxiaoguan; // 0:50/30 1:30/20
    // 2次,3次，4次
    public int jiesancishu;
    public ArrayList<Integer> paixing = new ArrayList<Integer>(); // 0:允许3不带（最后一手）1:允许3带1 2:允许3带一对 3:允许3带2单牌 4:可出姊妹对（飞机44 55）5允许4带1
    public ArrayList<Integer> teshu = new ArrayList<Integer>(); // 0: 3张A 1张2直接赢 ....以后扩展

    /**
     * 对象之间的浅克隆【只负责copy对象本身，不负责深度copy其内嵌的成员对象】
     *
     * @return
     */
    @Override
    public CNJPDK_CreateRoom clone() {
        return (CNJPDK_CreateRoom) super.clone();
    }

    /**
     * 实现对象间的深度克隆【从外形到内在细胞，完完全全深度copy】
     *
     * @return
     */
    public CNJPDK_CreateRoom deepClone() {
        // Anything 都是可以用字节流进行表示，记住是任何！
        CNJPDK_CreateRoom cookBook = null;
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
            cookBook = (CNJPDK_CreateRoom) ois.readObject();

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
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
    }

    @Override
    public boolean equals(Object obj) {
        return EqualsBuilder.reflectionEquals(this, obj);
    }
}
