package jsproto.c2s.cclass.club;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * 俱乐部
 *
 * @author zaf
 **/
public class Club_define {


    /**
     * 俱乐部 状态 1未批准,2已拒绝,4为已加入,8为已踢出,16已邀请,32邀请被拒,64退出
     */
    public enum Club_Player_Status {
        /**
         * 默认状态
         */
        PLAYER_NOMARL(0x00),
        /**
         * 未批准
         */
        PLAYER_WEIPIZHUN(0x01),
        /**
         * 已拒绝
         */
        PLAYER_JUJIE(0x02),
        /**
         * 为已加入
         */
        PLAYER_JIARU(0x04),

        /**
         * 为已踢出
         */
        PLAYER_TICHU(0x08),
        /**
         * 已邀请
         */
        PLAYER_YAOQING(0x10),
        /**
         * 邀请被拒
         */
        PLAYER_JUJIEYAOQING(0x20),
        /**
         * 已退出
         */
        PLAYER_TUICHU(0x40),
        /**
         * 退出未批准
         */
        PLAYER_TUICHU_WEIPIZHUN(0x50),
        /**
         * 关闭俱乐部踢出
         */
        PLAYER_TICHU_CLOSE(0x80),
        /**
         * 所有
         */
        PLAYER_ALL(0xFF),
        /**
         * 以下的状态不能设置数据库中,只能做通知用
         * <p>
         * 退出被拒绝
         */
        PLAYER_TUICHU_JUJUE(0x42),
        /**
         * 退出已提交
         */
        PLAYER_TUICHU_TICHU(0x43),
        /**
         * 成为管理员
         */
        PLAYER_BECOME_MGR(0x90),
        /**
         * 取消管理员
         */
        PLAYER_CANCEL_MGR(0x91),
        /**
         * 禁止游戏
         */
        PLAYER_BECOME_BAN(0x92),
        /**
         * 取消禁止游戏
         */
        PLAYER_CANCEL_BAN(0x93),
        /**
         * 成为赛事管理员
         */
        PLAYER_BECOME_UNIONMGR(0x94),
        ;

        private int value;

        Club_Player_Status(int value) {
            this.value = value;
        }

        public int value() {
            return this.value;
        }

        public static Club_Player_Status getClubStatus(String value) {
            String gameTypyName = value.toUpperCase();
            for (Club_Player_Status flow : Club_Player_Status.values()) {
                if (flow.toString().equals(gameTypyName)) {
                    return flow;
                }
            }
            return Club_Player_Status.PLAYER_NOMARL;
        }

        public static Club_Player_Status valueOf(int value) {
            for (Club_Player_Status flow : Club_Player_Status.values()) {
                if (flow.value == value) {
                    return flow;
                }
            }
            return Club_Player_Status.PLAYER_NOMARL;
        }
    }

    ;

    /**
     * 状态 1为正常,2为已解散
     */
    public enum Club_Status {
        /**
         * 初始状态
         */
        CLUB_STATUS_NOMARL(0),
        /**
         * 正常
         */
        CLUB_STATUS_OPEN(1),
        /**
         * 已解散
         */
        CLUB_STATUS_CLOSE(2),
        ;

        private int value;

        Club_Status(int value) {
            this.value = value;
        }

        public int value() {
            return this.value;
        }

        public static Club_Status getClubStatus(String value) {
            String gameTypyName = value.toUpperCase();
            for (Club_Status flow : Club_Status.values()) {
                if (flow.toString().equals(gameTypyName)) {
                    return flow;
                }
            }
            return Club_Status.CLUB_STATUS_NOMARL;
        }

        public static Club_Status valueOf(int value) {
            for (Club_Status flow : Club_Status.values()) {
                if (flow.value == value) {
                    return flow;
                }
            }
            return Club_Status.CLUB_STATUS_NOMARL;
        }
    }

    ;

    /**
     * 状态 0为正常,1禁用,2为已解散
     */
    public enum Club_CreateGameSetStatus {
        /**
         * 空
         */
        CLUB_CRATE_GAME_SET_STATUS_NOT(-1),
        /**
         * 正常
         */
        CLUB_CRATE_GAME_SET_STATUS_NOMARL(0),
        /**
         * 禁用
         */
        CLUB_CRATE_GAME_SET_STATUS_DISABLE(1),
        /**
         * 解算
         */
        CLUB_CRATE_GAME_SET_STATUS_DELETE(2),
        ;

        private int value;

        Club_CreateGameSetStatus(int value) {
            this.value = value;
        }

        public int value() {
            return this.value;
        }

        public static Club_CreateGameSetStatus getClubStatus(String value) {
            String gameTypyName = value.toUpperCase();
            for (Club_CreateGameSetStatus flow : Club_CreateGameSetStatus.values()) {
                if (flow.toString().equals(gameTypyName)) {
                    return flow;
                }
            }
            return Club_CreateGameSetStatus.CLUB_CRATE_GAME_SET_STATUS_NOMARL;
        }

        public static Club_CreateGameSetStatus valueOf(int value) {
            for (Club_CreateGameSetStatus flow : Club_CreateGameSetStatus.values()) {
                if (flow.value == value) {
                    return flow;
                }
            }
            return Club_CreateGameSetStatus.CLUB_CRATE_GAME_SET_STATUS_NOT;
        }
    }

    ;

    /**
     * 状态 1为正常,2为已解散
     */
    public enum Club_MINISTER {
        /**
         * 普通成员
         */
        Club_MINISTER_GENERAL(0),
        /**
         * 管理者
         */
        Club_MINISTER_MGR(1),
        /**
         * 创建
         */
        Club_MINISTER_CREATER(2),
        /**
         * 赛事管理
         */
        Club_MINISTER_UNIONMGR(3),
        ;

        private int value;

        Club_MINISTER(int value) {
            this.value = value;
        }

        public int value() {
            return this.value;
        }

        public static Club_MINISTER getClubStatus(String value) {
            String gameTypyName = value.toUpperCase();
            for (Club_MINISTER flow : Club_MINISTER.values()) {
                if (flow.toString().equals(gameTypyName)) {
                    return flow;
                }
            }
            return Club_MINISTER.Club_MINISTER_GENERAL;
        }

        public static Club_MINISTER valueOf(int value) {
            for (Club_MINISTER flow : Club_MINISTER.values()) {
                if (flow.value == value) {
                    return flow;
                }
            }
            return Club_MINISTER.Club_MINISTER_GENERAL;
        }
    }

    ;

    /**
     * 合伙人状态枚举
     */
    public enum Club_PARTNER {
        /**
         * 不是合伙人
         */
        Club_PARTNER_NULL(0),
        /**
         * 合伙人
         */
        Club_PARTNER_ONE(1),
        ;
        private int value;

        Club_PARTNER(int value) {
            this.value = value;
        }

        public int value() {
            return this.value;
        }

        public static Club_PARTNER getClubStatus(String value) {
            String gameTypyName = value.toUpperCase();
            for (Club_PARTNER flow : Club_PARTNER.values()) {
                if (flow.toString().equals(gameTypyName)) {
                    return flow;
                }
            }
            return Club_PARTNER.Club_PARTNER_NULL;
        }

        public static Club_PARTNER valueOf(int value) {
            for (Club_PARTNER flow : Club_PARTNER.values()) {
                if (flow.value == value) {
                    return flow;
                }
            }
            return Club_PARTNER.Club_PARTNER_NULL;
        }
    }

    ;

    /**
     * 状态 1为正常,2为已解散
     */
    public enum Club_ROOMCARD_CHANGE_TYPE {
        /**
         * 拨卡
         */
        Club_ROOMCARD_CHANGE_TYPE_GAIN(1),
        /**
         * 撤回
         */
        Club_ROOMCARD_CHANGE_TYPE_LOSE(2),
        ;

        private int value;

        Club_ROOMCARD_CHANGE_TYPE(int value) {
            this.value = value;
        }

        public int value() {
            return this.value;
        }

        public static Club_ROOMCARD_CHANGE_TYPE getClubStatus(String value) {
            String gameTypyName = value.toUpperCase();
            for (Club_ROOMCARD_CHANGE_TYPE flow : Club_ROOMCARD_CHANGE_TYPE.values()) {
                if (flow.toString().equals(gameTypyName)) {
                    return flow;
                }
            }
            return Club_ROOMCARD_CHANGE_TYPE.Club_ROOMCARD_CHANGE_TYPE_LOSE;
        }

        public static Club_ROOMCARD_CHANGE_TYPE valueOf(int value) {
            for (Club_ROOMCARD_CHANGE_TYPE flow : Club_ROOMCARD_CHANGE_TYPE.values()) {
                if (flow.value == value) {
                    return flow;
                }
            }
            return Club_ROOMCARD_CHANGE_TYPE.Club_ROOMCARD_CHANGE_TYPE_LOSE;
        }
    }

    ;


    /**
     * 状态 0为默认状态,1为正常，2禁用3结算，4启用，5修改，6服务器重启，7后台 ,8游戏回退
     */
    public enum Club_OperationStatus {
        /**
         * 默认状态
         */
        CLUB_OPERATION_STATUS_NOMARL(0),
        /**
         * 正常
         */
        CLUB_OPERATION_STATUS_CREATE(1),
        /**
         * 禁用
         */
        CLUB_OPERATION_STATUS_DISABLE(2),
        /**
         * 解算
         */
        CLUB_OPERATION_STATUS_DELETE(3),
        /**
         * 启用
         */
        CLUB_OPERATION_STATUS_RESTART(4),
        /**
         * 修改
         */
        CLUB_OPERATION_STATUS_CHANGE(5),
        /**
         * 服务器重启
         */
        CLUB_OPERATION_STATUS_SERVER_RESTART(6),
        /**
         * 后台
         */
        CLUB_OPERATION_STATUS_BACKGROUND(7),
        /**
         * 游戏回退
         */
        CLUB_OPERATION_STATUS_GAME(8),
        /**
         * 玩家转入转出
         */
        CLUB_OPERATION_STATUS_ROLL_IN(9),
        /**
         * 俱乐部关闭
         */
        CLUB_OPERATION_STATUS_CLUB_CLOSE(10),
        /**
         * 游戏 消耗错误先回退后再消耗
         */
        CLUB_OPERATION_STATUS_GAME_CONSUME(11),
        ;

        private int value;

        Club_OperationStatus(int value) {
            this.value = value;
        }

        public int value() {
            return this.value;
        }

        public static Club_OperationStatus getClubStatus(String value) {
            String gameTypyName = value.toUpperCase();
            for (Club_OperationStatus flow : Club_OperationStatus.values()) {
                if (flow.toString().equals(gameTypyName)) {
                    return flow;
                }
            }
            return Club_OperationStatus.CLUB_OPERATION_STATUS_NOMARL;
        }

        public static Club_OperationStatus valueOf(int value) {
            for (Club_OperationStatus flow : Club_OperationStatus.values()) {
                if (flow.value == value) {
                    return flow;
                }
            }
            return Club_OperationStatus.CLUB_OPERATION_STATUS_NOMARL;
        }
    }

    ;


    /**
     * 状态 0月度,1年度
     */
    public static enum Club_CostRoomCard_Rank {
        /**
         * 月度
         */
        CLUB_COST_ROOMCARD_RANK_MONTH(0),
        /**
         * 年度
         */
        CLUB_COST_ROOMCARD_RANK_YEAR(1),
        ;

        private int value;

        Club_CostRoomCard_Rank(int value) {
            this.value = value;
        }

        public int value() {
            return this.value;
        }

        public static Club_CostRoomCard_Rank getClubStatus(String value) {
            String gameTypyName = value.toUpperCase();
            for (Club_CostRoomCard_Rank flow : Club_CostRoomCard_Rank.values()) {
                if (flow.toString().equals(gameTypyName)) {
                    return flow;
                }
            }
            return Club_CostRoomCard_Rank.CLUB_COST_ROOMCARD_RANK_MONTH;
        }

        public static Club_CostRoomCard_Rank valueOf(int value) {
            for (Club_CostRoomCard_Rank flow : Club_CostRoomCard_Rank.values()) {
                if (flow.value == value) {
                    return flow;
                }
            }
            return Club_CostRoomCard_Rank.CLUB_COST_ROOMCARD_RANK_MONTH;
        }
    }

    ;


    /**
     * 获取时间 0今天,1昨天,2最近三天
     */
    public enum Club_Record_Get_Type {
        /**
         * 今天
         */
        CLUB_RECORD_GET_TYPE_TODAY(0),
        /**
         * 昨天
         */
        CLUB_RECORD_GET_TYPE_YESTERDAY(1),
        /**
         * 最近三天
         */
        CLUB_RECORD_GET_TYPE_LAST_THREE_DAYS(2),
        /**
         * 近一个月
         */
        CLUB_RECORD_GET_TYPE_MONTH(3),
        ;

        private int value;

        Club_Record_Get_Type(int value) {
            this.value = value;
        }

        public int value() {
            return this.value;
        }

        public static Club_Record_Get_Type getRecordType(String value) {
            String gameTypyName = value.toUpperCase();
            for (Club_Record_Get_Type flow : Club_Record_Get_Type.values()) {
                if (flow.toString().equals(gameTypyName)) {
                    return flow;
                }
            }
            return Club_Record_Get_Type.CLUB_RECORD_GET_TYPE_TODAY;
        }

        public static Club_Record_Get_Type valueOf(int value) {
            for (Club_Record_Get_Type flow : Club_Record_Get_Type.values()) {
                if (flow.value == value) {
                    return flow;
                }
            }
            return Club_Record_Get_Type.CLUB_RECORD_GET_TYPE_TODAY;
        }
    }

    ;


    /**
     * 状态 1为不可以自动创建,2为不可以自动创建
     */
    public enum Club_AUTOROOMCREATION {
        /**
         * 可以自动创建
         */
        Club_AUTOROOMCREATION_YES(0),
        /**
         * 不可以自动创建
         */
        Club_AUTOROOMCREATION_NOT(1),
        ;

        private int value;

        Club_AUTOROOMCREATION(int value) {
            this.value = value;
        }

        public int value() {
            return this.value;
        }

        public static Club_AUTOROOMCREATION getAutoRoomCreation(String value) {
            String gameTypyName = value.toUpperCase();
            for (Club_AUTOROOMCREATION flow : Club_AUTOROOMCREATION.values()) {
                if (flow.toString().equals(gameTypyName)) {
                    return flow;
                }
            }
            return Club_AUTOROOMCREATION.Club_AUTOROOMCREATION_YES;
        }

        public static Club_AUTOROOMCREATION valueOf(int value) {
            for (Club_AUTOROOMCREATION flow : Club_AUTOROOMCREATION.values()) {
                if (flow.value == value) {
                    return flow;
                }
            }
            return Club_AUTOROOMCREATION.Club_AUTOROOMCREATION_YES;
        }
    }

    ;


    /**
     * 状态 0成员不可以创建房间,1成员可以创建房间
     */
    public enum Club_MEMBERCREATIONROOM {
        /**
         * 成员不可以创建房间
         */
        Club_MEMBERCREATIONROOM_NOT(0),
        /**
         * 成员可以创建房间
         */
        Club_MEMBERCREATIONROOM_YES(1),
        ;

        private int value;

        Club_MEMBERCREATIONROOM(int value) {
            this.value = value;
        }

        public int value() {
            return this.value;
        }

        public static Club_MEMBERCREATIONROOM getMemeberCreationRoom(String value) {
            String gameTypyName = value.toUpperCase();
            for (Club_MEMBERCREATIONROOM flow : Club_MEMBERCREATIONROOM.values()) {
                if (flow.toString().equals(gameTypyName)) {
                    return flow;
                }
            }
            return Club_MEMBERCREATIONROOM.Club_MEMBERCREATIONROOM_NOT;
        }

        public static Club_MEMBERCREATIONROOM valueOf(int value) {
            for (Club_MEMBERCREATIONROOM flow : Club_MEMBERCREATIONROOM.values()) {
                if (flow.value == value) {
                    return flow;
                }
            }
            return Club_MEMBERCREATIONROOM.Club_MEMBERCREATIONROOM_NOT;
        }
    }

    ;

    /**
     * 状态 0成员不可以创建房间,1成员可以创建房间
     */
    public enum Club_DISSOLVEROOM_STATUS {
        /**
         * 默认状态
         */
        Club_DISSOLVEROOM_STATUS_NORMAL(0),
        /**
         * 俱乐部关闭
         */
        Club_DISSOLVEROOM_STATUS_CLOSE(1),
        /**
         * 修改俱乐部房间设置
         */
        Club_DISSOLVEROOM_STATUS_CHANGE_ROOMCRG(2),
        /**
         * 服务器维护
         */
        Club_DISSOLVEROOM_STATUS_GAMESERVER_MAINTENACE(3),
        /**
         * 真实房间没有人关闭创建虚拟的房间
         */
        Club_DISSOLVEROOM_STATUS_NO_PLAYEER(4),
        /**
         * 修改俱乐部房间key
         */
        Club_DISSOLVEROOM_STATUS_CHANGE_ROOMKEY(5),
        ;

        private int value;

        Club_DISSOLVEROOM_STATUS(int value) {
            this.value = value;
        }

        public int value() {
            return this.value;
        }

        public static Club_DISSOLVEROOM_STATUS getDissolveroom(String value) {
            String gameTypyName = value.toUpperCase();
            for (Club_DISSOLVEROOM_STATUS flow : Club_DISSOLVEROOM_STATUS.values()) {
                if (flow.toString().equals(gameTypyName)) {
                    return flow;
                }
            }
            return Club_DISSOLVEROOM_STATUS.Club_DISSOLVEROOM_STATUS_NORMAL;
        }

        public static Club_DISSOLVEROOM_STATUS valueOf(int value) {
            for (Club_DISSOLVEROOM_STATUS flow : Club_DISSOLVEROOM_STATUS.values()) {
                if (flow.value == value) {
                    return flow;
                }
            }
            return Club_DISSOLVEROOM_STATUS.Club_DISSOLVEROOM_STATUS_NORMAL;
        }
    }

    ;


    /**
     * 亲友圈-成员类型
     *
     * @author Administrator
     */
    public enum Club_MEMBER_TYPE {
        /**
         * 空
         */
        NOT,
        /**
         * 绑定合伙人的成员
         */
        PARTNER,
        /**
         * 普通亲友圈成员
         */
        GENERAL,
        ;
    }

    /**
     * 亲友圈-合伙人状态
     *
     * @author Administrator
     */
    public enum Club_PARTNER_STATUS {
        /**
         * 没有
         */
        NOT,
        /**
         * 任命
         */
        APPOINT,
        /**
         * 卸任
         */
        CANCEL,
        ;

        public static Club_PARTNER_STATUS valueOf(int value) {
            for (Club_PARTNER_STATUS flow : Club_PARTNER_STATUS.values()) {
                if (flow.ordinal() == value) {
                    return flow;
                }
            }
            return Club_PARTNER_STATUS.NOT;
        }
    }


    /**
     * 亲友圈-基础设置
     *
     * @author Administrator
     */
    public enum Club_BASICS {
        /**
         * 进入亲友圈需审批
         */
        APPROVAL,
        /**
         * 自动开始
         */
        AUTO_START,
        ;
    }

    /**
     * 亲友圈-审核状态
     *
     * @author Administrator
     */
    public enum Club_Examine {
        /**
         * 不需要审核
         */
        Not,
        /**
         * 未审核
         */
        Examine_Not,
        /**
         * 已审核
         */
        Examine_Already,
        ;
    }

    ;

    /**
     * 亲友圈-踢出房间
     *
     * @author Administrator
     */
    public enum Club_KICK_OUT {
        /**
         * 30秒未准备
         */
        T30S(30),
        /**
         * 60秒未准备（1分钟）
         */
        T60S(60),
        /**
         * 300秒未准备（5分钟）
         */
        T300S(300),
        /**
         * 没限制
         */
        NOT_LIMIT(0),
        ;
        private int value;

        Club_KICK_OUT(int value) {
            this.value = value;
        }

        public int value() {
            return this.value;
        }

        public static Club_KICK_OUT valueOf(int value) {
            for (Club_KICK_OUT flow : Club_KICK_OUT.values()) {
                if (flow.ordinal() == value) {
                    return flow;
                }
            }
            return Club_KICK_OUT.NOT_LIMIT;
        }
    }

    ;


    /**
     * 亲友圈-解散设置
     *
     * @author Administrator
     */
    public enum Club_DISSOLVE_SET {
        /**
         * 全部同意
         */
        ALL,
        /**
         * 一半同意
         */
        HALF,
        /**
         * 不允许申请解散
         */
        NOT,
        ;

        public static Club_DISSOLVE_SET valueOf(int value) {
            for (Club_DISSOLVE_SET flow : Club_DISSOLVE_SET.values()) {
                if (flow.ordinal() == value) {
                    return flow;
                }
            }
            return Club_DISSOLVE_SET.HALF;
        }
    }

    ;

    /**
     * 亲友圈-解散时间
     *
     * @author Administrator
     */
    public enum Club_DISSOLVE_TIME {
        /**
         * 60秒
         */
        T1M(60),
        /**
         * 120秒
         */
        T2M(120),
        /**
         * 180秒
         */
        T3M(180),
        /**
         * 300秒
         */
        T5M(300),
        ;
        private int value;

        Club_DISSOLVE_TIME(int value) {
            this.value = value;
        }

        public int value() {
            return this.value;
        }

        public static Club_DISSOLVE_TIME valueOf(int value) {
            for (Club_DISSOLVE_TIME flow : Club_DISSOLVE_TIME.values()) {
                if (flow.ordinal() == value) {
                    return flow;
                }
            }
            return Club_DISSOLVE_TIME.T2M;
        }
    }

    ;


    /**
     * 亲友圈-操作疲劳值
     *
     * @author Administrator
     */
    public enum Club_OP_FATIGUE_VALUE {
        /**
         * 错误操作类型
         */
        ERROR(0),
        /**
         * 加操作
         */
        PLUS(1),
        /**
         * 减操作
         */
        REDUCE(2),
        ;
        private int value;

        Club_OP_FATIGUE_VALUE(int value) {
            this.value = value;
        }

        public int value() {
            return this.value;
        }

        public static Club_OP_FATIGUE_VALUE valueOf(int value) {
            for (Club_OP_FATIGUE_VALUE flow : Club_OP_FATIGUE_VALUE.values()) {
                if (flow.value() == value) {
                    return flow;
                }
            }
            return Club_OP_FATIGUE_VALUE.ERROR;
        }
    }

    ;

    /**
     * 推广员状态
     */
    public enum Club_PROMOTION {
        /**
         * 不是推广员
         */
        NOT,
        /**
         * 任命
         */
        APPOINT,
        /**
         * 卸任
         */
        LEAVE_OFFICE;


        /**
         * 检查是否期待值
         *
         * @param promotion 期待推广员状态
         * @param value     值
         * @return
         */
        public static boolean CheckExpectedValue(Club_PROMOTION promotion, int value) {
            return promotion.ordinal() == value;
        }

    }

    /**
     * 推广员动态
     */
    public enum Club_PROMOTION_DYNAMIC {
        /**
         * 设置为推广员
         */
        PROMOTION_DYNAMIC_SET(1),
        /**
         * 上任了推广员
         */
        PROMOTION_DYNAMIC_APPOINT(2),
        /**
         * 卸任了推广员
         */
        PROMOTION_DYNAMIC_LEAVE_OFFICE(3),
        /**
         * 删除推广员
         */
        PROMOTION_DYNAMIC_DELETE(4),

        /**
         * 推广员活跃度异常补偿
         */
        PROMOTION_DYNAMIC_ACTIVE_COMPENSATE(5),

        /**
         * 推广员活跃度异常扣除
         */
        PROMOTION_DYNAMIC_ACTIVE_DEDUCT(6),

        /**
         * 推广员从属发生改变
         */
        PROMOTION_DYNAMIC_CHANGE(7),

        /**
         * 对局获得活跃度
         */
        PROMOTION_DYNAMIC_ACTIVE_GET(8),

        /**
         * 推广员从属发生改变
         */
        PROMOTION_DYNAMIC_CHANGE_ALL(9),
        ;
        private int value;

        Club_PROMOTION_DYNAMIC(int value) {
            this.value = value;
        }

        public int value() {
            return value;
        }

        public static Club_PROMOTION_DYNAMIC getPromotionDynamicActive(int type) {
            return type == 0 ? PROMOTION_DYNAMIC_ACTIVE_COMPENSATE : PROMOTION_DYNAMIC_ACTIVE_DEDUCT;
        }
    }

    /**
     * 推广员明细类型
     */
    public enum Club_PROMOTION_DYNAMIC_TYPE {
        /**
         * 全部
         */
        ALL,
        /**
         * 异常补偿
         */
        COMPENSATE,
        /**
         * 异常扣除
         */
        DEDUCT,
        /**
         * 赛事房间
         */
        UNION,
        /**
         * 亲友圈房间
         */
        CLUB,
        /**
         * 下属玩家变更
         */
        CHANGE,
        ;

        public static Club_PROMOTION_DYNAMIC_TYPE valueOf(int value) {
            for (Club_PROMOTION_DYNAMIC_TYPE flow : Club_PROMOTION_DYNAMIC_TYPE.values()) {
                if (flow.ordinal() == value) {
                    return flow;
                }
            }
            return Club_PROMOTION_DYNAMIC_TYPE.ALL;
        }


        public static List<Integer> valueOf(Club_PROMOTION_DYNAMIC_TYPE type, boolean isCreate) {
            if (Club_PROMOTION_DYNAMIC_TYPE.ALL.equals(type)) {
                return Collections.emptyList();
            } else if (Club_PROMOTION_DYNAMIC_TYPE.CHANGE.equals(type)) {
                if (isCreate) {
                    return Arrays.asList(Club_PROMOTION_DYNAMIC.PROMOTION_DYNAMIC_CHANGE_ALL.value());
                } else {
                    return Arrays.asList(Club_PROMOTION_DYNAMIC.PROMOTION_DYNAMIC_CHANGE.value());
                }
            } else if (Club_PROMOTION_DYNAMIC_TYPE.CLUB.equals(type) || Club_PROMOTION_DYNAMIC_TYPE.UNION.equals(type)) {
                return Arrays.asList(Club_PROMOTION_DYNAMIC.PROMOTION_DYNAMIC_ACTIVE_GET.value());
            } else if (Club_PROMOTION_DYNAMIC_TYPE.DEDUCT.equals(type)) {
                return Arrays.asList(Club_PROMOTION_DYNAMIC.PROMOTION_DYNAMIC_ACTIVE_DEDUCT.value());
            } else if (Club_PROMOTION_DYNAMIC_TYPE.COMPENSATE.equals(type)) {
                return Arrays.asList(Club_PROMOTION_DYNAMIC.PROMOTION_DYNAMIC_ACTIVE_COMPENSATE.value());
            }
            return Collections.emptyList();
        }

    }


    /**
     * 推广员状态 等级
     */
    public enum Club_PROMOTION_LEVEL {
        /**
         * 不是推广员
         */
        NOT,
        /**
         * 任命
         */
        APPOINT,
        ;

    }

    /**
     * 推广员状态 等级
     */
    public enum Club_PROMOTION_LEVEL_POWER {
        /**
         * 亲友圈创建者(查询所有)
         */
        CREATE,
        /**
         * 亲友圈推广员
         */
        PROMOTION_LEVEL,

        /**
         * 亲友圈普通成员
         */
        GENERAL,
        ;
    }

    /**
     * 推广员状态删除
     */
    public enum Club_PROMOTION_LEVEL_DELETE {
        /**
         * 移除、删除推广员（转移数据）
         */
        DELETE,
        /**
         * 卸任
         */
        CANCEL,

        /**
         * 卸任顶级
         */
        CANCEL_TOP_LEVEL,
        ;
    }

    /**
     * 亲友圈：加入申请
     */
    public enum CLUB_JOIN {
        /**
         * 0需要审核
         */
        CLUB_JOIN_NEED_AUDIT,
        /**
         * 1不需要审核
         */
        CLUB_JOIN_NO_NEED_AUDIT,
        ;

        public static CLUB_JOIN valueOf(int value) {
            for (CLUB_JOIN flow : CLUB_JOIN.values()) {
                if (flow.ordinal() == value) {
                    return flow;
                }
            }
            return CLUB_JOIN.CLUB_JOIN_NEED_AUDIT;
        }
    }

    /**
     * 亲友圈：加入申请
     */
    public enum CLUB_QUIT {
        /**
         * 0需要审核
         */
        CLUB_QUIT_NEED_AUDIT,
        /**
         * 1不需要审核
         */
        CLUB_QUIT_NO_NEED_AUDIT,
        ;

        public static CLUB_QUIT valueOf(int value) {
            for (CLUB_QUIT flow : CLUB_QUIT.values()) {
                if (flow.ordinal() == value) {
                    return flow;
                }
            }
            return CLUB_QUIT.CLUB_QUIT_NEED_AUDIT;
        }
    }

    /**
     * 获取时间 0今天,1昨天,2最近三天
     */
    public enum CLUB_PROMOTION_TIME_TYPE {
        /**
         * 例子：6.7号
         * 今天
         */
        RECORD_GET_TYPE_TODAY(0),
        /**
         * 昨天 6.6号
         */
        RECORD_GET_TYPE_YESTERDAY(1),
        /**
         * 前天 6.5号
         */
        RECORD_GET_TYPE_TWO(2),
        /**
         * 6.4号
         */
        RECORD_GET_TYPE_THREE(3),

        /**
         * 6.3号
         */
        RECORD_GET_TYPE_FOUR(4),
        /**
         * 6.2号
         */
        RECORD_GET_TYPE_FIVE(5),
        /**
         * 6.1号
         */
        RECORD_GET_TYPE_SIX(6),

        ;

        private int value;

        CLUB_PROMOTION_TIME_TYPE(int value) {
            this.value = value;
        }

        public int value() {
            return this.value;
        }

        public static CLUB_PROMOTION_TIME_TYPE getRecordType(String value) {
            String gameTypyName = value.toUpperCase();
            for (CLUB_PROMOTION_TIME_TYPE flow : CLUB_PROMOTION_TIME_TYPE.values()) {
                if (flow.toString().equals(gameTypyName)) {
                    return flow;
                }
            }
            return CLUB_PROMOTION_TIME_TYPE.RECORD_GET_TYPE_TODAY;
        }

        public static CLUB_PROMOTION_TIME_TYPE valueOf(int value) {
            for (CLUB_PROMOTION_TIME_TYPE flow : CLUB_PROMOTION_TIME_TYPE.values()) {
                if (flow.value == value) {
                    return flow;
                }
            }
            return CLUB_PROMOTION_TIME_TYPE.RECORD_GET_TYPE_TODAY;
        }
    }

    ;

    /**
     * 保险箱分数类型
     */
    public enum Club_CASE_SPORTS_TYPE {
        /**
         * 增加
         */
        ADD,
        /**
         * 减少
         */
        SUB,
        ;

    }

    /**
     * 亲友圈动态
     *
     * @author Administrator
     */
    public enum CLUB_EXEC_TYPE {
        /**
         * 默认状态
         */
        CLUB_EXEC_NOT(0),
        /**
         * 设置管理员
         */
        CLUB_EXEC_BECOME_MGR(1),
        /**
         * 取消管理员
         */
        CLUB_EXEC_CANCEL_MGR(2),
        /**
         * 设置推广员管理员
         */
        CLUB_EXEC_BECOME_PROMOTION_MGR(3),
        /**
         * 取消推广员管理员
         */
        CLUB_EXEC_CANCEL_PROMOTION_MGR(4),
        /**
         * 设置推广员
         */
        CLUB_EXEC_BECOME_PROMOTION(5),
        /**
         * 取消推广员
         */
        CLUB_EXEC_CANCEL_PROMOTION(6),
        /**
         * 设置赛事管理员
         */
        CLUB_EXEC_BECOME_UNION_MGR(7),
        ;
        private int value;

        private CLUB_EXEC_TYPE(int value) {
            this.value = value;
        }

        public int value() {
            return this.value;
        }
    }

    /**
     * 亲友圈房间竞技动态详细表状态
     *
     * @author Administrator
     */
    public enum CLUB_Room_Promotion_Point_TYPE {
        /**
         * 默认状态
         */
        DEFAULT(0),
        /**
         * 上级分成
         */
        UP_LEVEL(1),
        /**
         * 下级分成
         */
        DAWN_LEVEL(2),
        /**
         * 房费
         */
        ROOM_CONSUME(3),
        ;
        private int value;

        private CLUB_Room_Promotion_Point_TYPE(int value) {
            this.value = value;
        }

        public int value() {
            return this.value;
        }
    }

    /**
     * 计算七天总和 和客户端约定 使用-2
     * 中至 成员排行列表和客户端约定 使用1
     */
    public enum CLUB_COUNT_RECORD {

        CLUB_COUNT_RECORD(-2),

        CLUB_COUNT_RECORD_Seven(7),


        ;

        private int value;

        CLUB_COUNT_RECORD(int value) {
            this.value = value;
        }

        public int value() {
            return this.value;
        }


        public static CLUB_COUNT_RECORD valueOf(int value) {
            for (CLUB_COUNT_RECORD flow : CLUB_COUNT_RECORD.values()) {
                if (flow.value == value) {
                    return flow;
                }
            }
            return CLUB_COUNT_RECORD.CLUB_COUNT_RECORD;
        }
    }

    ;

    /**
     * 推广员级别限制
     */
    public enum CLUB_LEVEL_MAX {
        //2022.3.23 分包等级限制改回 由30 改回3 剑锋要求  3.30 3改成5
        CLUB_LEVEL_NORMAL(5),

        CLUB_LEVEL_ZHONG(1),


        ;

        private int value;

        CLUB_LEVEL_MAX(int value) {
            this.value = value;
        }

        public int value() {
            return this.value;
        }


        public static CLUB_LEVEL_MAX valueOf(int value) {
            for (CLUB_LEVEL_MAX flow : CLUB_LEVEL_MAX.values()) {
                if (flow.value == value) {
                    return flow;
                }
            }
            return CLUB_LEVEL_MAX.CLUB_LEVEL_NORMAL;
        }
    }

    ;

    /**
     * 在线人数查看
     */
    public enum CLUB_PLAYERNUM_SHOW {
        NORMAL(0),//都可见
        PROMOTION_NOT_SEE(1),//推广员不可见


        ;

        private int value;

        CLUB_PLAYERNUM_SHOW(int value) {
            this.value = value;
        }

        public int value() {
            return this.value;
        }


        public static CLUB_PLAYERNUM_SHOW valueOf(int value) {
            for (CLUB_PLAYERNUM_SHOW flow : CLUB_PLAYERNUM_SHOW.values()) {
                if (flow.value == value) {
                    return flow;
                }
            }
            return CLUB_PLAYERNUM_SHOW.NORMAL;
        }
    }

    ;

    /**
     * 职位
     * 0 普通成员
     * 1 推广员管理员
     * 2 推广员
     * 3 圈主
     */
    public enum Club_ZHONGZHI_POSISTION {
        /**
         * 普通成员
         */
        Club_MINISTER_GENERAL(0),
        /**
         * 推广员管理员
         */
        Club_PROMOTION_MANAGER(1),
        /**
         * 推广员
         */
        Club_PROMOTION(2),
        /**
         * 创建
         */
        Club_CREATER(3),
        ;

        private int value;

        Club_ZHONGZHI_POSISTION(int value) {
            this.value = value;
        }

        public int value() {
            return this.value;
        }

        public static Club_ZHONGZHI_POSISTION getClubStatus(String value) {
            String gameTypyName = value.toUpperCase();
            for (Club_ZHONGZHI_POSISTION flow : Club_ZHONGZHI_POSISTION.values()) {
                if (flow.toString().equals(gameTypyName)) {
                    return flow;
                }
            }
            return Club_ZHONGZHI_POSISTION.Club_MINISTER_GENERAL;
        }

        public static Club_ZHONGZHI_POSISTION valueOf(int value) {
            for (Club_ZHONGZHI_POSISTION flow : Club_ZHONGZHI_POSISTION.values()) {
                if (flow.value == value) {
                    return flow;
                }
            }
            return Club_ZHONGZHI_POSISTION.Club_MINISTER_GENERAL;
        }
    }

    /**
     * 是否有战绩
     * 0 所有成员
     * 1 最近一天无战绩
     * 2 最近三天无战绩
     * 3 最近七天无战绩
     */
    public enum Club_HAS_RECORD {
        /**
         * 所有成员
         */
        Club_HAS_RECORD_0(0),
        /**
         * 最近一天无战绩
         */
        Club_HAS_RECORD_1(1),
        /**
         * 最近三天无战绩
         */
        Club_HAS_RECORD_2(2),
        /**
         * 最近七天无战绩
         */
        Club_HAS_RECORD_3(3),
        ;

        private int value;

        Club_HAS_RECORD(int value) {
            this.value = value;
        }

        public int value() {
            return this.value;
        }

        public static Club_HAS_RECORD getClubStatus(String value) {
            String gameTypyName = value.toUpperCase();
            for (Club_HAS_RECORD flow : Club_HAS_RECORD.values()) {
                if (flow.toString().equals(gameTypyName)) {
                    return flow;
                }
            }
            return Club_HAS_RECORD.Club_HAS_RECORD_0;
        }

        public static Club_HAS_RECORD valueOf(int value) {
            for (Club_HAS_RECORD flow : Club_HAS_RECORD.values()) {
                if (flow.value == value) {
                    return flow;
                }
            }
            return Club_HAS_RECORD.Club_HAS_RECORD_0;
        }
    }
}
