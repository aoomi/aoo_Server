package server.aoo.dao.entity.game;
import com.baidu.bjf.remoting.protobuf.annotation.Protobuf;


import com.ddm.server.common.utils.CommLog;
import lombok.Builder;
import lombok.Data;
import server.aoo.dao.entity.IdEntity;

import jakarta.persistence.*;

/**
 * 玩家表
 * @author
 */
@Data
@Entity
@Table(name = "db_player", indexes = {@Index(columnList = "account_id", unique = true), @Index(columnList = "name"),@Index(columnList = "realReferer"),@Index(columnList = "phone"), @Index(columnList = "sid,name"), @Index(columnList = "createTime,id"), @Index(columnList = "lastLogin,id")})
public class DbPlayer extends IdEntity {

    /**
     * 玩家账号ID
     */
    @Column(columnDefinition = "bigint default 0 unique COMMENT '玩家账号ID'")
    @Protobuf(order = 1)
    private long accountId;

    /**
     * 服务器id
     */
    @Column(columnDefinition = "int(11) default 0 COMMENT '服务器id'")
    @Protobuf(order = 2)
    private int sid;

    /**
     * 玩家名称/昵称
     */
    @Column(columnDefinition = "varchar(50) default ''  COMMENT '玩家名称/昵称'")
    @Protobuf(order = 3)
    private String name;

    /**
     * 头像
     */
    @Column(columnDefinition = "int(2) default 0  COMMENT '头像'")
    @Protobuf(order = 4)
    private int icon;

    /**
     * 性别
     */
    @Column(columnDefinition = "int(2) default 0 COMMENT '性别'")
    @Protobuf(order = 5)
    private int sex;

    /**
     * 头像url地址
     */
    @Column(columnDefinition = "varchar(300) default '' COMMENT '头像url地址'")
    @Protobuf(order = 6)
    private String headImageUrl;

    /**
     * 账号创建时间毫秒
     */
    @Column(columnDefinition = "bigint default 0 COMMENT '账号创建时间毫秒'")
    @Protobuf(order = 7)
    private long createTime;

    /**
     * 等级
     */
    @Column(columnDefinition = "int(2) default 0  COMMENT '等级'")
    @Protobuf(order = 8)
    private int lv;

    /**
     * gm权限
     */
    @Column(columnDefinition = "int(2) default 0 COMMENT 'gm权限'")
    @Protobuf(order = 9)
    private int gmLevel;

    /**
     * vip等级
     */
    @Column(columnDefinition = "int(11) default 0 COMMENT 'vip等级'")
    @Protobuf(order = 10)
    private int vipLevel;

    /**
     * vip经验
     */
    @Column(columnDefinition = "int(11) default 0 COMMENT 'vip经验'")
    @Protobuf(order = 11)
    private int vipExp;
    /**
     * 累计充值, 统计玩家充值LB
     */
    @Column(columnDefinition = "int default 0 COMMENT '累计充值, 统计玩家充值LB'")
    @Protobuf(order = 12)
    private int totalRecharge;

    /**
     * 卢比
     */
    @Column(columnDefinition = "int default 0 COMMENT '卢比'")
    @Protobuf(order = 13)
    private int gold;

    /**
     * 作弊次数
     */
    @Column(columnDefinition = "int default 0 COMMENT '作弊次数'")
    @Protobuf(order = 14)
    private int cheatTimes;

    /**
     * 活跃记录时间
     */
    @Column(columnDefinition = "int default 0  COMMENT '活跃记录时间'")
    @Protobuf(order = 15)
    private int activeRecordingTime;

    /**
     * 最近一次游戏时间
     */
    @Column(columnDefinition = "int default 0  COMMENT '最近一次游戏时间'")
    @Protobuf(order = 16)
    private int lastGameTime;

    /**
     * 禁登过期时间
     */
    @Column(columnDefinition = "int default 0 COMMENT '禁登过期时间'")
    @Protobuf(order = 17)
    private int bannedLoginExpiredTime;

    /**
     * 封号次数
     */
    @Column(columnDefinition = "int default 0 COMMENT '封号次数'")
    @Protobuf(order = 18)
    private int bannedTimes;

    /**
     * 最近一次登陆時間
     */
    @Column(columnDefinition = "int default 0 COMMENT '最近一次登陆時間'")
    @Protobuf(order = 19)
    private int lastLogin;

    /**
     * 最近一次登出時間
     */
    @Column(columnDefinition = "int default 0 COMMENT '最近一次登出時間'")
    @Protobuf(order = 20)
    private int lastLogout;

    /**
     * 真实名字
     */
    @Column(columnDefinition = "varchar(12) default '' COMMENT '真实名字'")
    @Protobuf(order = 23)
    private String realName;

    /**
     * 真实号码
     */
    @Column(columnDefinition = "varchar(50) default ''  COMMENT '真实号码'")
    @Protobuf(order = 24)
    private String realNumber;

    /**
     * 直接推荐人
     */
    @Column(columnDefinition = "bigint default 0 COMMENT '直接推荐人'")
    @Protobuf(order = 25)
    private long realReferer;


    /**
     * 电话号码
     */
    @Column(columnDefinition = "varchar(25) default '' COMMENT '电话号码'")
    @Protobuf(order = 28)
    private String phone;

    /**
     * app操作系统类型(0:未知或者网页,1:android,2:ios)
     */
    @Column(columnDefinition = "int default 0  COMMENT 'app操作系统类型(0:未知或者网页,1:android,2:ios)'")
    @Protobuf(order = 30)
    private int os;

    /**
     * 最大筹码
     */
    @Column(columnDefinition="double(11,2) default 0.00 COMMENT '最大筹码'")
    @Protobuf(order = 31)
    private double maxChip;
    /**
     * 总局数
     */
    @Column(columnDefinition = "int default 0 COMMENT '总局数'")
    @Protobuf(order = 32)
    private int totalSetCount;
    /**
     * 胜利局数
     */
    @Column(columnDefinition = "int default 0 COMMENT '胜利局数'")
    @Protobuf(order = 33)
    private int winSetCount;
    /**
     * 玩家是否设置“隐藏成就”
     */
    @Column(columnDefinition = "int default 0 COMMENT '0不显示,1显示'")
    @Protobuf(order = 34)
    private int showProfile;
    /**
     * 刷新时间
     */
    @Column(columnDefinition = "int default 0 COMMENT '刷新时间'")
    private int refreshTime;
    /**
     * 修改名称时间
     */
    @Column(columnDefinition = "int default 0 COMMENT '修改名称时间'")
    private int nameCoolingTime;
    /**
     * 绑定邮箱
     */
    @Column(columnDefinition = "varchar(255) default '' COMMENT '绑定邮箱'")
    @Protobuf(order = 35)
    private String emailAdress;

    /**
     * 绑定奖励
     */
    @Column(columnDefinition = "int(2) default 0 COMMENT '绑定'")
    @Protobuf(order = 36)
    private int binding;

    /**
     * 推广员的奖励次数
     */
    @Column(columnDefinition="int(4) default 0.00 COMMENT '推广员的奖励次数'")
    @Protobuf(order = 37)
    private int referralNumber;

    /**
     * 推广员的奖金
     */
    @Column(columnDefinition="double(11,2) default 0.00 COMMENT '推广员的奖金'")
    @Protobuf(order = 38)
    private double referralBonus;

    /**
     * 第一次登录方式
     */
    @Column(columnDefinition = "int(2) default 0 COMMENT '第一次登录方式'")
    @Protobuf(order = 39)
    private int loginMode;



    private static final long serialVersionUID = 1L;

    public DbPlayer() {
    }

    @Override
    public void prepareForInsert() {

    }

    @Builder(toBuilder = true)
    public DbPlayer(long id, long accountId, int sid, String name, int icon, int sex, String headImageUrl, long createTime, int lv, int gmLevel, int vipLevel, int vipExp, int totalRecharge, int gold, int cheatTimes, int activeRecordingTime,int lastGameTime, int bannedLoginExpiredTime, int bannedTimes, int lastLogin, int lastLogout, String realName, String realNumber, long realReferer, String phone, int os,int refreshTime,int loginMode) {
        this.id = id;
        this.setAccountId(accountId);
        this.sid = sid;
        this.name = name;
        this.icon = icon;
        this.sex = sex;
        this.headImageUrl = headImageUrl;
        this.createTime = createTime;
        this.lv = lv;
        this.gmLevel = gmLevel;
        this.vipLevel = vipLevel;
        this.vipExp = vipExp;
        this.totalRecharge = totalRecharge;
        this.gold = gold;
        this.cheatTimes = cheatTimes;
        this.activeRecordingTime = activeRecordingTime;
        this.lastGameTime = lastGameTime;
        this.bannedLoginExpiredTime = bannedLoginExpiredTime;
        this.bannedTimes = bannedTimes;
        this.lastLogin = lastLogin;
        this.lastLogout = lastLogout;
        this.realName = realName;
        this.realNumber = realNumber;
        this.realReferer = realReferer;
        this.phone = phone;
        this.os = os;
        this.refreshTime = refreshTime;
        this.loginMode = loginMode;
    }

    public void setAccountId(long accountId) {
        if (this.accountId > 0L) {
            CommLog.error("error setAccountId this.accountId:{},accountId:{}",this.accountId,accountId);
            return;
        }
        this.accountId = accountId;
    }



}
