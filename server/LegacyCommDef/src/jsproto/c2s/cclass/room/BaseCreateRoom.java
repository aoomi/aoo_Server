package jsproto.c2s.cclass.room;

import cenum.room.ClubCostType;
import cenum.room.PaymentRoomCardType;
import jsproto.c2s.cclass.BaseSendMsg;
import jsproto.c2s.cclass.union.UnionRoomSportsBigWinnerConsumeItem;
import lombok.Data;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * 房间创建时初始配置
 *
 * @author 房间初始配置
 */
@Data
public class BaseCreateRoom extends BaseSendMsg implements Serializable, Cloneable {
    /**
     * 只给客户端显示用，就是那么奢侈
     */
    private int jushu;
    /**
     * 只给客户端显示用，就是那么奢侈
     */
    private int renshu;
    /**
     * 只给客户端显示用，就是那么奢侈
     */
    private int fangfei;
    /**
     * 局数
     */
    private int setCount = 1;
    /**
     * 玩家最大人数
     */
    private int playerNum = 4;
    /**
     * 玩家最小人数
     */
    private int playerMinNum = 4;
    /**
     * 是否平分房卡
     */
    private int paymentRoomCardType = PaymentRoomCardType.PaymentRoomCardType_HomeOwerPay.value();
    /**
     * 玩法
     */
    private int wanfa = 0;
    /**
     * 可选玩法
     */
    private List<Integer> kexuanwanfa = new ArrayList<Integer>();
    /**
     * 俱乐部ID
     */
    private long clubId = 0L;
    /**
     * 创建房间类型
     */
    private int createType = 1;
    /**
     * 高级选项
     */
    private ArrayList<Integer> gaoji = new ArrayList<>();
    /**
     * 标记：0没有子游戏，
     */
    private int sign;
    /**
     * 房间：0：房间内切换人数
     */
    private ArrayList<Integer> fangjian = new ArrayList<>();
    /**
     * 不限制,3分钟出牌,5分钟出牌
     */
    private int xianShi;
    /**
     * 30秒,1分钟,3分钟,5分钟,不可解散
     */
    private int jiesan = 2;
    /**
     * 是不是俱乐部成员创建房间
     */
    private Boolean isClubMemberCreateRoom = false;
    /**
     * 俱乐部大赢家付消耗
     */
    private Integer clubWinnerPayConsume = 0;
    /**
     * 俱乐部消耗类型
     */
    private Integer clubCostType = ClubCostType.ROOM_CARD.ordinal();
    /**
     * 赛事ID
     */
    private long unionId;
    /**
     * 俱乐部一键创建房间设置 0 创建俱乐部房间 1：设置一键卡房设置 2：改变房间设置 Club_SetStatus
     */
    private long gameIndex = 0L;
    /**
     * 房间名称
     */
    private String roomName;
    /**
     * 房间竞技点门槛
     */
    private Double roomSportsThreshold;
    /**
     * 竞技点倍数
     */
    private Double sportsDouble;
    /**
     * 房间竞技点消耗类型
     */
    private Integer roomSportsType;
    /**
     * 自动解散
     */
    private Double autoDismiss;
    /**
     * 大赢家赢竞技点 >=
     */
    private Double geWinnerPoint;
    /**
     * 房间竞技点大赢家消耗
     */
    private Double roomSportsBigWinnerConsume;
    /**
     * 房间竞技点每人消耗
     */
    private Double roomSportsEveryoneConsume;
    /**
     * 大赢家比赛分列表
     */
    private List<UnionRoomSportsBigWinnerConsumeItem> bigWinnerConsumeList;
    /**
     * 百分比
     */
    private Double percentage;
    /**
     * 奖金池
     */
    private Double prizePool;

    /**
     * 密码
     */
    private String password;

    /**
     * 颜色
     */
    private String color;

    /**
     * 小局切牌
     */
    private int xiaojuqiepai;
    /**
     * 大局算分
     */
    private int dajusuanfen;

    private Integer beishu;

    private double leavePoint;

    /**
     *
     */
    private static final long serialVersionUID = 1L;

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


    public void setClubMemberCreateRoom(Boolean clubMemberCreateRoom) {
        if (Objects.isNull(clubMemberCreateRoom)) {
            this.isClubMemberCreateRoom = clubMemberCreateRoom;
        } else if (Objects.isNull(this.isClubMemberCreateRoom) && Objects.nonNull(clubMemberCreateRoom)) {
            this.isClubMemberCreateRoom = clubMemberCreateRoom;
        }
    }

    public void setClubWinnerPayConsume(Integer clubWinnerPayConsume) {
        if (Objects.isNull(clubWinnerPayConsume)) {
            this.clubWinnerPayConsume = clubWinnerPayConsume;
        } else if (Objects.isNull(this.clubWinnerPayConsume) && Objects.nonNull(clubWinnerPayConsume)) {
            this.clubWinnerPayConsume = clubWinnerPayConsume;
        }
    }

    public void setClubCostType(Integer clubCostType) {
        if (Objects.isNull(clubCostType)) {
            this.clubCostType = clubCostType;
        } else if (Objects.isNull(this.clubCostType) && Objects.nonNull(clubCostType)) {
            this.clubCostType = clubCostType;
        }
    }


    public void setRoomName(String roomName) {
        if (Objects.isNull(roomName)) {
            this.roomName = roomName;
        } else if (Objects.isNull(this.roomName) && Objects.nonNull(roomName)) {
            this.roomName = roomName;
        }
    }

    public void updateRoomName(String roomName) {
        this.roomName = roomName;
    }

    public void setRoomSportsThreshold(Double roomSportsThreshold) {
        if (Objects.isNull(roomSportsThreshold)) {
            this.roomSportsThreshold = roomSportsThreshold;
        } else {
            this.roomSportsThreshold = roomSportsThreshold;
        }
    }

    public void setSportsDouble(Double sportsDouble) {
        if (Objects.isNull(sportsDouble)) {
            this.sportsDouble = sportsDouble;
        } else {
            this.sportsDouble = sportsDouble;
        }

    }

    public void setRoomSportsType(Integer roomSportsType) {
        if (Objects.isNull(roomSportsType)) {
            this.roomSportsType = roomSportsType;
        } else if (Objects.isNull(this.roomSportsType) && Objects.nonNull(roomSportsType)) {
            this.roomSportsType = roomSportsType;
        }
    }

    public void setAutoDismiss(Double autoDismiss) {
        if (Objects.isNull(autoDismiss)) {
            this.autoDismiss = autoDismiss;
        } else {
            this.autoDismiss = autoDismiss;
        }
    }

    public void setRoomSportsBigWinnerConsume(Double roomSportsBigWinnerConsume) {
        if (Objects.isNull(roomSportsBigWinnerConsume)) {
            this.roomSportsBigWinnerConsume = roomSportsBigWinnerConsume;
        } else {
            this.roomSportsBigWinnerConsume = roomSportsBigWinnerConsume;
        }
    }

    public void setInitRoomName(String roomName) {
        if (Objects.nonNull(roomName)) {
            this.roomName = roomName;
        }
    }

    public void setRoomSportsEveryoneConsume(Double roomSportsEveryoneConsume) {
        if (Objects.isNull(roomSportsEveryoneConsume)) {
            this.roomSportsEveryoneConsume = roomSportsEveryoneConsume;
        } else {
            this.roomSportsEveryoneConsume = roomSportsEveryoneConsume;
        }
    }

    public void setBigWinnerConsumeList(List<UnionRoomSportsBigWinnerConsumeItem> bigWinnerConsumeList) {
        if (Objects.isNull(bigWinnerConsumeList)) {
            this.bigWinnerConsumeList = bigWinnerConsumeList;
        } else {
            this.bigWinnerConsumeList = bigWinnerConsumeList;
        }
    }

    public void setPrizePool(Double prizePool) {
        if (Objects.isNull(prizePool)) {
            this.prizePool = prizePool;
        } else {
            this.prizePool = prizePool;
        }
    }

    public int getSetCount() {
        return setCount;
    }

    public int getPlayerNum() {
        return playerNum;
    }

    public int getPlayerMinNum() {
        return playerMinNum;
    }

    public int getPaymentRoomCardType() {
        return paymentRoomCardType;
    }

    public int getWanfa() {
        return wanfa;
    }

    public List<Integer> getKexuanwanfa() {
        return kexuanwanfa;
    }

    public long getClubId() {
        return clubId;
    }

    public int getCreateType() {
        return createType;
    }

    public ArrayList<Integer> getGaoji() {
        return gaoji;
    }

    public int getSign() {
        return sign;
    }

    public ArrayList<Integer> getFangjian() {
        return fangjian;
    }

    public int getXianShi() {
        return xianShi;
    }

    public int getJiesan() {
        return jiesan;
    }

    public Boolean getClubMemberCreateRoom() {
        return isClubMemberCreateRoom;
    }

    public int getClubWinnerPayConsume() {
        return Objects.isNull(clubWinnerPayConsume) ? 0 : clubWinnerPayConsume.intValue();

    }

    public Integer getClubCostType() {
        return Objects.isNull(clubCostType) ? 0 : clubCostType.intValue();

    }

    public long getUnionId() {
        return unionId;
    }

    public Long getGameIndex() {
        return Objects.isNull(gameIndex) ? 0L : gameIndex;

    }

    public String getRoomName() {
        return Objects.isNull(roomName) ? "" : roomName;

    }

    public Double getRoomSportsThreshold() {
        return Objects.isNull(roomSportsThreshold) ? 0D : roomSportsThreshold.doubleValue();
    }

    public Double getSportsDouble() {
        return Objects.isNull(sportsDouble) ? 1D : sportsDouble.doubleValue();
    }

    public Integer getRoomSportsType() {
        return Objects.isNull(roomSportsType) ? 0 : roomSportsType;

    }

    public Double getAutoDismiss() {
        return Objects.isNull(autoDismiss) ? -1000D : autoDismiss.doubleValue();

    }

    public Double getRoomSportsBigWinnerConsume() {
        return Objects.isNull(roomSportsBigWinnerConsume) ? 0D : roomSportsBigWinnerConsume.doubleValue();

    }

    public Double getRoomSportsEveryoneConsume() {
        return Objects.isNull(roomSportsEveryoneConsume) ? 0D : roomSportsEveryoneConsume.doubleValue();
    }

    public List<UnionRoomSportsBigWinnerConsumeItem> getBigWinnerConsumeList() {
        return Objects.isNull(bigWinnerConsumeList) ? Collections.emptyList() : bigWinnerConsumeList;
    }

    /**
     * 奖金池
     *
     * @return
     */
    public Double getPrizePool() {
        return Objects.isNull(prizePool) ? 0D : Math.max(0D, prizePool.doubleValue());
    }

    public int getBeishu() {
        if (null == beishu) {
            return 1;
        }
        return Math.max(beishu, 1);
    }

    public void setBeishu(Integer beishu) {
        this.beishu = beishu;
    }

    /**
     * 对象之间的浅克隆【只负责copy对象本身，不负责深度copy其内嵌的成员对象】
     *
     * @return
     */
    @Override
    public BaseCreateRoom clone() {
        try {
            return (BaseCreateRoom) super.clone();
        } catch (CloneNotSupportedException ex) { throw new IllegalStateException("Clone contract violated", ex); }
    }

    /**
     * 实现对象间的深度克隆【从外形到内在细胞，完完全全深度copy】
     *
     * @return
     */
    public BaseCreateRoom deepClone() {
        // Anything 都是可以用字节流进行表示，记住是任何！
        BaseCreateRoom cookBook = null;
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
            cookBook = (BaseCreateRoom) ois.readObject();

        } catch (Exception e) { throw new IllegalStateException("Deep clone failed", e); }
        return cookBook;
    }
}
