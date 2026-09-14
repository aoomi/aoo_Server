package business.scjymj.c2s.iclass;

import com.ddm.server.common.CommLogD;
import jsproto.c2s.cclass.room.BaseCreateRoom;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * 莆田麻将
 * 接收客户端数据
 * 创建房间
 *
 * @author Huaxing
 */
public class CSCJYMJ_CreateRoom extends BaseCreateRoom implements Cloneable, Serializable {
    // 2房牌,3房牌
    public int paishu;
    // 自摸加番,自摸加底
    public int hutype;
    // 2番,3番，4番
    public int fengDing;
    // 飘在内(选飘),飘在外(选飘),飘在内(座飘),飘在外(座飘),不飘
    public int piao;
    // 自动准备,庄闲玩法
    public List<Integer> other = new ArrayList<Integer>();
    // 2次,3次，4次
    public int jiesancishu;

    public int dianganghua;

    public int chajiao;
    public List<Integer> tiyan = new ArrayList<>();

    /**
     * 对象之间的浅克隆【只负责copy对象本身，不负责深度copy其内嵌的成员对象】
     *
     * @return
     */
    @Override
    public CSCJYMJ_CreateRoom clone() {
        return (CSCJYMJ_CreateRoom) super.clone();
    }

    /**
     * 实现对象间的深度克隆【从外形到内在细胞，完完全全深度copy】
     *
     * @return
     */
    public CSCJYMJ_CreateRoom deepClone() {
        // Anything 都是可以用字节流进行表示，记住是任何！
        CSCJYMJ_CreateRoom cookBook = null;
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
            cookBook = (CSCJYMJ_CreateRoom) ois.readObject();

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
