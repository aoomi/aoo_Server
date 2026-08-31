package jsproto.c2s.cclass.union;

import lombok.Data;

import java.io.*;

/**
 * 赛事亲友圈圈主报表
 */
@Data
public class UnionClubReportFormItem implements Serializable {
    /**
     * 日期
     */
    private String dateTime;
    /**
     * 玩家数
     */
    private int sizePlayer;
    /**
     * 局数
     */
    private int setCount;
    /**
     * 报名费
     */
    private double entryFee;
    /**
     * 分成
     */
    private String shareValue = "";
    /**
     * 活跃度(中至:活跃积分)
     */
    private double scorePoint;
    /**
     * 消耗钻石
     */
    private int consume;
    /**
     * 输赢比赛分
     */
    private double sportsPointConsume;
    /**
     * 个人比赛分
     */
    private double personalSportsPoint;
    /**
     * 总比赛分
     */
    private double sumSportsPoint;
    /**
     * 房间数
     */
    private int roomSize;

    /**
     * 成员总积分和
     */
    private double zhongZhiTotalPoint;
    /**
     * 最终积分
     */
    private double zhongZhiFinalTotalPoint;
    /**
     * 实际报名费 玩家实际出的报名费 存在大赢家情况下不出或者全出
     */
    private double actualEntryFee;

    /**
     * 推广员战绩分成(中至:活跃积分)
     */
    private double promotionShareValue;


    public static String getItemsName() {
        return "date_time as dateTime,sizePlayer,sum(setCount) as setCount,sum(roomAvgSportsPointConsume) as entryFee,shareValue,sum(scorePoint) AS scorePoint,sum(consume) as consume,sum(sportsPointConsume) as sportsPointConsume,personalSportsPoint,sum(sportsPoint) as sumSportsPoint,sum(roomSize) as roomSize,sum(roomSportsPointConsume) as actualEntryFee,sum(promotionShareValue) as `promotionShareValue`";
    }

    public static String getItemsNameClubLevelRoomLog() {
        return "date_time as dateTime,sum(setCount) as setCount,sum(roomAvgSportsPointConsume) as entryFee,sum(consume) as consume,sum(sportsPointConsume) as sportsPointConsume,sum(promotionShareValue) as shareValue,sum(sportsPointConsume) as sportsPointConsume,sum(roomSportsPointConsume) as actualEntryFee";
    }

    /**
     * 实现对象间的深度克隆【从外形到内在细胞，完完全全深度copy】
     *
     * @return
     */
    public UnionClubReportFormItem deepClone() {
        // Anything 都是可以用字节流进行表示，记住是任何！
        UnionClubReportFormItem cookBook = null;
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
            cookBook = (UnionClubReportFormItem) ois.readObject();

        } catch (Exception e) { throw new IllegalStateException("Deep clone failed", e); }
        return cookBook;
    }
}
