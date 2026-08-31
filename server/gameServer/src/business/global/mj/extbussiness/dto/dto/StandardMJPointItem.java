package business.global.mj.extbussiness.dto;

import com.ddm.server.common.CommLogD;
import com.ddm.server.common.utils.Maps;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

public class StandardMJPointItem<T> implements Cloneable, Serializable {
    /**
     * 七对对子一样的个数
     */
    private int duiNum = 0;
    /**
     * 新的扩展字段
     */
    private Object otherFlag;
    /**
     * 牌型校验器
     */
    private Map<String, T> cardTypeMap = new HashMap<>();
    /**
     * 分数(累加)
     */
    private int point = 0;
    /**
     * 其他分数（累乘）
     */
    private int otherPoint = 1;
    /**
     * 其他分数（番数）
     */
    private int other1Point = 0;
    /**
     * 牌型
     */
    private Map<String, Integer> map = Maps.newMap();

    /**
     * 其他牌型（可叠加）
     */
    private Map<String, Integer> otherMap = Maps.newMap();

    /**
     * 其他牌型（可叠加）
     */
    private Map<String, Integer> other1Map = Maps.newMap();

    public StandardMJPointItem() {
    }

    public Map<String, T> getCardTypeMap() {
        return cardTypeMap;
    }

    /**
     * 添加胡牌类型
     *
     * @param type     类型
     * @param cardType 卡类型
     */
    public void addCardTypeMap(String type, T cardType) {
        cardTypeMap.put(type, cardType);
    }

    public Map<String, Integer> getMap() {
        return map;
    }

    public Map<String, Integer> getOtherMap() {
        return otherMap;
    }

    public Map<String, Integer> getOther1Map() {
        return other1Map;
    }

    public void addOpPointEnum(String oEnum, int value) {
        this.map.put(oEnum, value);
        this.point += value;
    }

    public void removeItemByMap(String name, int value,int type) {
        if(type==1){
            if(map.containsKey(name)){
                map.remove(name);
                point-=value;
            }
        }else if(type==2){
            if(otherMap.containsKey(name)){
                otherMap.remove(name);
                otherPoint/=value;
            }
        }else{
            if(other1Map.containsKey(name)){
                other1Map.remove(name);
                other1Point-=value;
            }
        }
    }

    public void addOtherOpPointEnum(String oEnum, int value) {
        this.otherMap.put(oEnum, value);
        this.otherPoint *= value;
    }

    public void addOther1OpPointEnum(String oEnum, int value) {
        this.other1Map.put(oEnum, value);
        this.other1Point+= value;
    }

    public int getPoint() {
        return point;
    }

    /**
     * 漏胡决定分（根据这个分数决定是否本次分数超过上次漏胡分数，解除漏胡）
     * @return
     */
    public int getLHPoint(){
        return point;
    }

    public int getAllPoint() {
        return point * otherPoint;
    }

    public int getOtherPoint() {
        return otherPoint;
    }

    public int getOther1Point() {
        return other1Point;
    }

    /**
     * 实现对象间的深度克隆【从外形到内在细胞，完完全全深度copy】
     *
     * @return
     */
    public StandardMJPointItem deepClone() {
        // Anything 都是可以用字节流进行表示，记住是任何！
        StandardMJPointItem cookBook = null;
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
            cookBook = (StandardMJPointItem) ois.readObject();

        } catch (Exception e) {
            CommLogD.error(e.getClass() + ":" + e.getMessage());
        }
        return cookBook;
    }

    public void setDuiNum(int duiNum) {
        this.duiNum = duiNum;
    }

    public int getDuiNum() {
        return duiNum;
    }

    public void setOther1Point(int other1Point) {
        this.other1Point = other1Point;
    }

    public void setPoint(int point) {
        this.point = point;
    }

    public void setOtherFlag(Object otherFlag) {
        this.otherFlag = otherFlag;
    }

    public Object getOtherFlag() {
        return otherFlag;
    }
}
