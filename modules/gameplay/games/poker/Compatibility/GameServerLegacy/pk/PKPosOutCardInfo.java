package business.global.pk;

import com.ddm.server.common.CommLogD;
import lombok.Data;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.io.*;

@Data
public class PKPosOutCardInfo implements Cloneable, Serializable {

    /**
     * 打出的牌信息
     */
    private PKOpCard pkOpCard;

    /**
     * 打牌的位置
     */
    private int posId;

    public PKPosOutCardInfo(PKOpCard pkOpCard, int posId) {
        this.pkOpCard = pkOpCard;
        this.posId = posId;
    }

    /**
     * 实现对象间的深度克隆【从外形到内在细胞，完完全全深度copy】
     *
     * @return
     */
    public PKPosOutCardInfo deepClone() {
        // Anything 都是可以用字节流进行表示，记住是任何！
        PKPosOutCardInfo cookBook = null;
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
            cookBook = (PKPosOutCardInfo) ois.readObject();

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
