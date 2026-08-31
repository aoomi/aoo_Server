package jsproto.c2s.cclass.union;


/**
 * 大赛事定义
 *
 * @author
 **/
public class UnionDefine {

    /**
     * 赛事 状态 1未批准,2已拒绝,3为已加入,4为已踢出,5已邀请,6邀请被拒,7退出,8关闭赛事踢出
     */
    public enum UNION_PLAYER_STATUS {
        /**
         * 默认状态
         */
        PLAYER_NOMARL(0),
        /**
         * 未批准
         */
        PLAYER_WEIPIZHUN(1),
        /**
         * 已拒绝
         */
        PLAYER_JUJIE(2),
        /**
         * 为已加入
         */
        PLAYER_JIARU(3),
        /**
         * 为已踢出
         */
        PLAYER_TICHU(4),
        /**
         * 已邀请
         */
        PLAYER_YAOQING(5),
        /**
         * 邀请被拒
         */
        PLAYER_JUJIEYAOQING(6),
        /**
         * 已退出
         */
        PLAYER_TUICHU(7),
        /**
         * 关闭赛事踢出
         */
        PLAYER_TICHU_CLOSE(8),
        /**
         * 退出申请
         */
        PLAYER_TUICHU_SHENQING(9),
        /**
         * 所有
         */
        PLAYER_ALL(10),
        /**
         * 以下的状态不能设置数据库中,只能做通知用
         * 成为管理员
         */
        PLAYER_BECOME_MGR(11),
        /**
         * 取消管理员
         */
        PLAYER_CANCEL_MGR(12),
        /**
         * 禁止游戏
         */
        PLAYER_BECOME_BAN(13),
        /**
         * 取消禁止游戏
         */
        PLAYER_CANCEL_BAN(14),
        ;

        private int value;

        UNION_PLAYER_STATUS(int value) {
            this.value = value;
        }

        public int value() {
            return this.value;
        }

        public static UnionDefine.UNION_PLAYER_STATUS getClubStatus(String value) {
            String gameTypyName = value.toUpperCase();
            for (UnionDefine.UNION_PLAYER_STATUS flow : UnionDefine.UNION_PLAYER_STATUS.values()) {
                if (flow.toString().equals(gameTypyName)) {
                    return flow;
                }
            }
            return UnionDefine.UNION_PLAYER_STATUS.PLAYER_NOMARL;
        }

        public static UnionDefine.UNION_PLAYER_STATUS valueOf(int value) {
            for (UnionDefine.UNION_PLAYER_STATUS flow : UnionDefine.UNION_PLAYER_STATUS.values()) {
                if (flow.value == value) {
                    return flow;
                }
            }
            return UnionDefine.UNION_PLAYER_STATUS.PLAYER_NOMARL;
        }

        /**
         * 成员初始化加载
         * 未批准、已加入、退出申请、已邀请
         *
         * @param value 值
         * @return
         */
        public static boolean initMember(int value) {
            return PLAYER_WEIPIZHUN.value() == value || PLAYER_JIARU.value() == value || PLAYER_TUICHU_SHENQING.value() == value || PLAYER_YAOQING.value() == value;
        }

        /**
         * 是否已加入
         * 已加入、退出申请
         *
         * @param value 值
         * @return
         */
        public static boolean getStatus(int playerValue, int value) {
            if (value == playerValue) {
                return true;
            }
            // 搜索已加入的玩家
            if (PLAYER_JIARU.value() == value) {
                // 已加入、退出申请 == 已加入
                return PLAYER_JIARU.value() == playerValue || PLAYER_TUICHU_SHENQING.value() == playerValue;
            }

            if (PLAYER_TUICHU.value() == value) {
                // 退出
                return PLAYER_TUICHU.value() == playerValue || PLAYER_TICHU.value() == playerValue || PLAYER_TICHU_CLOSE.value() == playerValue;
            }

            // 搜索所有状态值的玩家
            if (PLAYER_ALL.value() == value) {
                // > 0 都符合
                return valueOf(playerValue).value() > 0;
            }
            return false;
        }


    }

    ;


    /**
     * 状态 0为默认状态,1为正常，2禁用3结算，4启用，5修改，6服务器重启，7后台 ,8游戏回退
     */
    public enum UNION_OPERATION_STATUS {
        /**
         * 默认状态
         */
        UNION_OPERATION_STATUS_NOMARL(0),
        /**
         * 正常
         */
        UNION_OPERATION_STATUS_CREATE(1),
        /**
         * 禁用
         */
        UNION_OPERATION_STATUS_DISABLE(2),
        /**
         * 解算
         */
        UNION_OPERATION_STATUS_DELETE(3),
        /**
         * 启用
         */
        UNION_OPERATION_STATUS_RESTART(4),
        /**
         * 修改
         */
        UNION_OPERATION_STATUS_CHANGE(5),
        /**
         * 服务器重启
         */
        UNION_OPERATION_STATUS_SERVER_RESTART(6),
        /**
         * 后台
         */
        UNION_OPERATION_STATUS_BACKGROUND(7),
        /**
         * 游戏回退
         */
        UNION_OPERATION_STATUS_GAME(8),
        /**
         * 玩家转入转出
         */
        UNION_OPERATION_STATUS_ROLL_IN(9),
        /**
         * 赛事关闭
         */
        UNION_OPERATION_STATUS_UNION_CLOSE(10),
        /**
         * 游戏 消耗错误先回退后再消耗
         */
        UNION_OPERATION_STATUS_GAME_CONSUME(11),
        ;

        private int value;

        UNION_OPERATION_STATUS(int value) {
            this.value = value;
        }

        public int value() {
            return this.value;
        }

        public static UNION_OPERATION_STATUS getUNIONSTATUS(String value) {
            String gameTypyName = value.toUpperCase();
            for (UNION_OPERATION_STATUS flow : UNION_OPERATION_STATUS.values()) {
                if (flow.toString().equals(gameTypyName)) {
                    return flow;
                }
            }
            return UNION_OPERATION_STATUS.UNION_OPERATION_STATUS_NOMARL;
        }

        public static UNION_OPERATION_STATUS valueOf(int value) {
            for (UNION_OPERATION_STATUS flow : UNION_OPERATION_STATUS.values()) {
                if (flow.value == value) {
                    return flow;
                }
            }
            return UNION_OPERATION_STATUS.UNION_OPERATION_STATUS_NOMARL;
        }
    }

    ;


    /**
     * 状态 1为正常,2为已解散
     */
    public enum UNION_STATUS {
        /**
         * 初始状态
         */
        UNION_STATUS_NOMARL(0),
        /**
         * 正常
         */
        UNION_STATUS_OPEN(1),
        /**
         * 已解散
         */
        UNION_STATUS_CLOSE(2),
        ;

        private int value;

        UNION_STATUS(int value) {
            this.value = value;
        }

        public int value() {
            return this.value;
        }

        public static UnionDefine.UNION_STATUS getUnionStatus(String value) {
            String gameTypyName = value.toUpperCase();
            for (UnionDefine.UNION_STATUS flow : UnionDefine.UNION_STATUS.values()) {
                if (flow.toString().equals(gameTypyName)) {
                    return flow;
                }
            }
            return UnionDefine.UNION_STATUS.UNION_STATUS_NOMARL;
        }

        public static UnionDefine.UNION_STATUS valueOf(int value) {
            for (UnionDefine.UNION_STATUS flow : UnionDefine.UNION_STATUS.values()) {
                if (flow.value == value) {
                    return flow;
                }
            }
            return UnionDefine.UNION_STATUS.UNION_STATUS_NOMARL;
        }
    }

    ;


    /**
     * 状态 0为正常,1禁用,2为已解散
     */
    public enum UNION_CREATE_GAME_SET_STATUS {
        /**
         * 空
         */
        UNION_CRATE_GAME_SET_STATUS_NOT(-1),
        /**
         * 正常
         */
        UNION_CRATE_GAME_SET_STATUS_NOMARL(0),
        /**
         * 禁用
         */
        UNION_CRATE_GAME_SET_STATUS_DISABLE(1),
        /**
         * 解算
         */
        UNION_CRATE_GAME_SET_STATUS_DELETE(2),
        ;

        private int value;

        UNION_CREATE_GAME_SET_STATUS(int value) {
            this.value = value;
        }

        public int value() {
            return this.value;
        }

        public static UNION_CREATE_GAME_SET_STATUS getUNIONSTATUS(String value) {
            String gameTypyName = value.toUpperCase();
            for (UNION_CREATE_GAME_SET_STATUS flow : UNION_CREATE_GAME_SET_STATUS.values()) {
                if (flow.toString().equals(gameTypyName)) {
                    return flow;
                }
            }
            return UNION_CREATE_GAME_SET_STATUS.UNION_CRATE_GAME_SET_STATUS_NOMARL;
        }

        public static UNION_CREATE_GAME_SET_STATUS valueOf(int value) {
            for (UNION_CREATE_GAME_SET_STATUS flow : UNION_CREATE_GAME_SET_STATUS.values()) {
                if (flow.value == value) {
                    return flow;
                }
            }
            return UNION_CREATE_GAME_SET_STATUS.UNION_CRATE_GAME_SET_STATUS_NOT;
        }
    }

    ;

    /**
     * 状态 0成员不可以创建房间,1成员可以创建房间
     */
    public enum UNION_DISSOLVEROOM_STATUS {
        /**
         * 默认状态
         */
        UNION_DISSOLVEROOM_STATUS_NORMAL(0),
        /**
         * 赛事关闭
         */
        UNION_DISSOLVEROOM_STATUS_CLOSE(1),
        /**
         * 修改赛事房间设置
         */
        UNION_DISSOLVEROOM_STATUS_CHANGE_ROOMCRG(2),
        /**
         * 服务器维护
         */
        UNION_DISSOLVEROOM_STATUS_GAMESERVER_MAINTENACE(3),
        /**
         * 真实房间没有人关闭创建虚拟的房间
         */
        UNION_DISSOLVEROOM_STATUS_NO_PLAYEER(4),
        /**
         * 修改赛事房间key
         */
        UNION_DISSOLVEROOM_STATUS_CHANGE_ROOMKEY(5),
        ;

        private int value;

        UNION_DISSOLVEROOM_STATUS(int value) {
            this.value = value;
        }

        public int value() {
            return this.value;
        }

        public static UnionDefine.UNION_DISSOLVEROOM_STATUS getDissolveroom(String value) {
            String gameTypyName = value.toUpperCase();
            for (UnionDefine.UNION_DISSOLVEROOM_STATUS flow : UnionDefine.UNION_DISSOLVEROOM_STATUS.values()) {
                if (flow.toString().equals(gameTypyName)) {
                    return flow;
                }
            }
            return UnionDefine.UNION_DISSOLVEROOM_STATUS.UNION_DISSOLVEROOM_STATUS_NORMAL;
        }

        public static UnionDefine.UNION_DISSOLVEROOM_STATUS valueOf(int value) {
            for (UnionDefine.UNION_DISSOLVEROOM_STATUS flow : UnionDefine.UNION_DISSOLVEROOM_STATUS.values()) {
                if (flow.value == value) {
                    return flow;
                }
            }
            return UnionDefine.UNION_DISSOLVEROOM_STATUS.UNION_DISSOLVEROOM_STATUS_NORMAL;
        }
    }

    ;


    /**
     * 联赛动态
     *
     * @author Administrator
     */
    public enum UNION_EXEC_TYPE {
        /**
         * 默认状态
         */
        UNION_EXEC_NOT(0),
        /**
         * 竞技点增加
         */
        UNION_EXEC_SPORTS_POINT_ADD(1),
        /**
         * 竞技点减去
         */
        UNION_EXEC_SPORTS_POINT_MINUS(2),
        /**
         * 收益百分比
         */
        UNION_EXEC_SCORE_PERCENT(3),
        /**
         * 有人审核加入
         */
        UNION_EXEC_JIARU(4),
        /**
         * 有人审核退出
         */
        UNION_EXEC_TUICHU(5),
        /**
         * 踢出
         */
        UNION_EXEC_TICHU(6),
        /**
         * 邀请加入
         */
        UNION_EXEC_YAOQING(7),
        /**
         * 创建赛事
         */
        UNION_EXEC_CREATER(8),
        /**
         * 赛事解散
         */
        UNION_EXEC_DISSOLVE(9),
        /**
         * 赛事启动
         */
        UNION_EXEC_START_UP(10),
        /**
         * 赛事停用
         */
        UNION_EXEC_STOP_USING(11),
        /**
         * 魔法表情使用
         */
        UNION_EXEC_EXPRESSION_START_UP(12),
        /**
         * 魔法表情停止
         */
        UNION_EXEC_EXPRESSION_STOP_USING(13),
        /**
         * 成员管理员
         */
        UNION_EXEC_BECOME_MGR(14),
        /**
         * 取消管理员
         */
        UNION_EXEC_CANCEL_MGR(15),
        /**
         * 创建房间
         */
        UNION_EXEC_CREATE_ROOM(16),
        /**
         * 修改房间
         */
        UNION_EXEC_UPDATE_ROOM(17),
        /**
         * 解散房间
         */
        UNION_EXEC_DISMISS_ROOM(18),
        /**
         * 开启加入审核
         */
        UNION_EXEC_JOIN_OPEN(19),
        /**
         * 关闭加入审核
         */
        UNION_EXEC_JOIN_STOP(20),
        /**
         * 开启退出审核
         */
        UNION_EXEC_QUIT_OPEN(21),
        /**
         * 加入退出审核
         */
        UNION_EXEC_QUIT_STOP(22),
        /**
         * 竞技点清零设置,0不清零
         */
        UNION_EXEC_NO_CLEAR(23),
        /**
         * 竞技点清零设置,1每天清零
         */
        UNION_EXEC_CLEAR_DAILY(24),
        /**
         * 竞技点清零设置,2每周清零
         */
        UNION_EXEC_CLEAR_WEEKLY(25),
        /**
         * 竞技点清零设置,3每月清零
         */
        UNION_EXEC_CLEAR_MONTHLY(26),
        /**
         * 赛事名称更新
         */
        UNION_EXEC_UPDATE_NAME(27),
        /**
         * 赛事总竞技点增加
         */
        UNION_EXEC_ADD(28),
        /**
         * 赛事总竞技点减少
         */
        UNION_EXEC_MINUS(29),
        /**
         * 不需要审核加入
         * 直接加入
         */
        UNION_EXEC_JIARU_NOT(30),
        /**
         * 不需要审核审核
         * 直接退出
         */
        UNION_EXEC_TUICHU_NOT(31),

        /**
         * 裁判力度
         */
        UNION_EXEC_INIT_SPORTS(32),

        /**
         * 比赛频率
         */
        UNION_EXEC_MATCH_RATE(33),

        /**
         * 奖励
         */
        UNION_EXEC_REWARD(34),

        /**
         * 淘汰值
         */
        UNION_EXEC_OUT_SPORTS(35),

        /**
         * 禁止参与游戏
         */
        UNION_EXEC_BAN_GAME(36),
        /**
         * 取消禁止参与游戏
         */
        UNION_EXEC_CANCEL_BAN_GAME(37),

        /**
         * 开启允许亲友圈添加同赛事玩家
         */
        UNION_EXEC_JOIN_CLUB_SAME_UNION_OPEN(38),
        /**
         * 关闭允许亲友圈添加同赛事玩家
         */
        UNION_EXEC_JOIN_CLUB_SAME_UNION_STOP(39),

        /**
         * 预警值关闭
         */
        UNION_EXEC_SPORTS_WARNING_CLOSE(40),
        /**
         * 预警值修改
         */
        UNION_EXEC_SPORTS_WARNING_CHANGE(41),
        /**
         * 聯盟预警值关闭
         */
        UNION_CLUB_EXEC_SPORTS_WARNING_CLOSE(42),
        /**
         * 聯盟预警值修改
         */
        UNION_CLUB_EXEC_SPORTS_WARNING_CHANGE(43),
        /**
         * 联赛增加禁止游戏人员
         */
        UNION_CLUB_BAN_GAME_PLAYER_ADD(44),
        /**
         * 联赛删除禁止游戏人员
         */
        UNION_CLUB_BAN_GAME_PLAYER_DELETE(45),
        /**
         * 个人预警值关闭
         */
        UNION_EXEC_PERSONAL_SPORTS_WARNING_CLOSE(46),
        /**
         * 个人预警值修改
         */
        UNION_EXEC_PERSONAL_SPORTS_WARNING_CHANGE(47),

        /**
         * 预留值修改
         */
        UNION_EXEC_RESERVED_VALUE_CHANGE(48),
        /**
         * 亲友圈收益变为区间分成
         */
        UNION_EXEC_SHARE_SECTION(49),
        /**
         * 推广员区间分成修改
         */
        PROMOTION_EXEC_SHARE_SECTION_CHANGE(50),
        /**
         * 推广员区间分成分牌修改
         */
        PROMOTION_EXEC_SHARE_SECTION_ALLOW_CHANGE(51),
        /**
         * 推广员归属变更
         */
        PROMOTION_BELONG_CHANGE(52),

        /**
         * 默认状态
         */
        Club_EXEC_NOT(0),
        /**
         * 加入
         */
        Club_EXEC_JIARU(101),
        /**
         * 退出
         */
        Club_EXEC_TUICHU(102),
        /**
         * 踢出
         */
        Club_EXEC_TICHU(103),
        /**
         * 成员管理员
         */
        Club_EXEC_BECOME_MGR(104),
        /**
         * 取消管理员
         */
        Club_EXEC_CANCEL_MGR(105),

        /**
         * 成员创建者
         */
        Club_EXEC_CREATER(106),
        /**
         * 任命合伙人
         */
        Club_EXEC_APPOINT_PARTNER(107),
        /**
         * 卸任合伙人
         */
        Club_EXEC_CANCEL_PARTNER(108),
        /**
         * 创建房间
         */
        Club_EXEC_CREATE_ROOM(109),
        /**
         * 禁用房间
         */
        Club_EXEC_BAN_ROOM(110),
        /**
         * 修改房间
         */
        Club_EXEC_UPDATE_ROOM(111),
        /**
         * 解散房间
         */
        Club_EXEC_DISMISS_ROOM(112),
        /**
         * 正常房间
         */
        Club_EXEC_NOMARL_ROOM(113),
        /**
         * 竞技点亲友圈增加
         */
        UNION_CLUB_EXEC_SPORTS_POINT_ADD(114),
        /**
         * 竞技点亲友圈减去
         */
        UNION_CLUB_EXEC_SPORTS_POINT_MINUS(115),

        /**
         * 申请复赛操作
         */
        UNION_APPLY_REMATCH_AGREE(116),
        /**
         * 退赛申请操作
         */
        UNION_BACK_OFF_AGREE(117),
        /**
         * 退赛拒绝操作
         */
        UNION_BACK_OFF_REFUSE(118),
        /**
         * 报名费减少
         */
        UNION_ROOM_EXEC_SPORTS_POINT_ENTRY_FEE_MINUS(119),
        /**
         * 比赛分增加
         */
        UNION_ROOM_EXEC_SPORTS_POINT_ADD(120),
        /**
         * 比赛分减少
         */
        UNION_ROOM_EXEC_SPORTS_POINT_MINUS(121),

        /**
         * 授权增加比赛分
         */
        UNION_EXEC_EMPOWER_SPORTS_POINT_ADD(122),
        /**
         * 授权减少比赛分
         */
        UNION_EXEC_EMPOWER_SPORTS_POINT_MINUS(123),

        /**
         * 补偿增加比赛分
         */
        UNION_EXEC_COMPENSATE_SPORTS_POINT_ADD(124),
        /**
         * 补偿减少比赛分
         */
        UNION_EXEC_COMPENSATE_SPORTS_POINT_MINUS(125),

        /**
         * 报名费增加
         */
        UNION_ROOM_EXEC_SPORTS_POINT_ENTRY_FEE_ADD(126),

        /**
         * 成为赛事管理员
         */
        PLAYER_BECOME_UNIONMGR(127),

        /**
         * 成为推广员管理员
         */
        PLAYER_BECOME_PROMOTIONMGR(128),
        /**
         * 切牌消耗
         */
        UNION_ROOM_QIEPAI_CONSUME(129),
        /**
         * 切牌收入
         */
        UNION_ROOM_QIEPAI_INCOME(130),
        /**
         * 授权增加比赛分
         */
        UNION_EXEC_EMPOWER_SPORTS_POINT_ADD_SELF(131),
        /**
         * 授权减少比赛分
         */
        UNION_EXEC_EMPOWER_SPORTS_POINT_MINUS_SELF(132),

        /**
         * 补偿增加比赛分
         */
        UNION_EXEC_COMPENSATE_SPORTS_POINT_ADD_SELF(133),
        /**
         * 补偿减少比赛分
         */
        UNION_EXEC_COMPENSATE_SPORTS_POINT_MINUS_SELF(134),
        /**
         * 取消赛事管理员
         */
        PLAYER_CANCEL_UNIONMGR(135),
        /**
         * 取消推广员管理员
         */
        PLAYER_CANCEL_PROMOTIONMGR(136),
        /**
         * 发起审核增加比赛分
         */
        UNION_EXEC_EXAMINE_SPORTS_POINT_ADD_EXE(137),
        /**
         * 发起审核减少比赛分
         */
        UNION_EXEC_EXAMINE_SPORTS_POINT_MINUS_EXE(138),
        /**
         * 被审核增加比赛分
         */
        UNION_EXEC_EXAMINE_SPORTS_POINT_ADD_OP(139),
        /**
         * 被审核减少比赛分
         */
        UNION_EXEC_EXAMINE_SPORTS_POINT_MINUS_OP(140),
        /**
         * 设置为推广员
         */
        CLUB_PROMOTION_DYNAMIC_SET(1001),
        /**
         * 上任了推广员
         */
        CLUB_PROMOTION_DYNAMIC_APPOINT(1002),
        /**
         * 卸任了推广员
         */
        CLUB_PROMOTION_DYNAMIC_LEAVE_OFFICE(1003),
        /**
         * 删除推广员
         */
        CLUB_PROMOTION_DYNAMIC_DELETE(1004),
        /**
         * 收益固定
         */
        UNION_EXEC_SCORE_FIXES(1005),
        /**
         * 推广员战绩收入
         */
        UNION_EXEC_SHARE_INCOME(1006),
        /**
         * 修改桌子显示数量
         */
        UNION_EXEC_CHANGE_TABLENUM(1007),
        /**
         * 推广员竞技点收益收入分成
         */
        UNION_EXEC_PROMOTION_SHARE_INCOME(1008),
        /**
         * 玩家保险箱分数增加
         */
        PLAYER_CASE_SPORTS_POINT_ADD(1009),
        /**
         * 玩家保险箱分数减少
         */
        PLAYER_CASE_SPORTS_POINT_SUB(1010),
        /**
         * 保险箱功能关闭
         */
        UNION_CASE_SPORTS_POINT_CLOSE(1011),
        /**
         * 踢出有保险箱的人
         */
        UNION_CASE_SPORTS_POINT_TICHU(1012),
        /**
         * 推广员竞技点收益收入分成到保险箱
         */
        UNION_EXEC_PROMOTION_SHARE_INCOME_CASEPOINT(1013),
        /**
         * 报名费新
         */
        UNION_ENTREE_FEE_NEW(1014),
        /**
         * 报名费新 详细
         */
        UNION_ENTREE_FEE_NEW_DETAIL(1015),
        /**
         * 生存积分关闭
         */
        UNION_ALIVE_SPORTS_CLOSE(1016),
        /**
         * 生存积分开启
         */
        UNION_ALIVE_SPORTS_OPEN(1017),
        /**
         * 生存积分变换
         */
        UNION_ALIVE_SPORTS_CHANGE(1018),
        /**
         * 个人淘汰分变换
         */
        CLUB_ELIMINATE_POINT_CHANGE(1019),
        /**
         * 跨级变换增加
         */
        CLUB_KUAJI_SPOINTCHANGE_ADD(1020),
        /**
         * 跨级变换减少
         */
        CLUB_KUAJI_SPOINTCHANGE_SUB(1021),
        /**
         * 直属玩家被踢出
         */
        CLUB_ZHI_SHU_TICHU(1022),
        /**
         * 直属玩家加入
         */
        CLUB_ZHI_SHU_JIARU(1023),
        /**
         * 直属玩家被修改归属
         */
        CLUB_ZHI_SHU_CHANGE_BELONG(1024),
        ;


        private int value;

        private UNION_EXEC_TYPE(int value) {
            this.value = value;
        }

        public int value() {
            return this.value;
        }

        public static UnionDefine.UNION_EXEC_TYPE valueOf(int value) {
            for (UnionDefine.UNION_EXEC_TYPE flow : UnionDefine.UNION_EXEC_TYPE.values()) {
                if (flow.value == value) {
                    return flow;
                }
            }
            return UnionDefine.UNION_EXEC_TYPE.UNION_EXEC_NOT;
        }

        /**
         * @param isManage
         * @return
         */
        public static UNION_EXEC_TYPE getUnionExecTypeGaneral(boolean isManage, boolean isAdd) {
            if (isManage) {
                return isAdd ? UNION_EXEC_TYPE.UNION_EXEC_EMPOWER_SPORTS_POINT_ADD : UNION_EXEC_TYPE.UNION_EXEC_EMPOWER_SPORTS_POINT_MINUS;
            } else {
                return isAdd ? UNION_EXEC_TYPE.UNION_EXEC_COMPENSATE_SPORTS_POINT_ADD : UNION_EXEC_TYPE.UNION_EXEC_COMPENSATE_SPORTS_POINT_MINUS;
            }
        }

        /**
         * @param
         * @return
         */
        public static UNION_EXEC_TYPE getUnionExecTypeGaneralKuaJi(boolean isAdd) {
            return isAdd ? UNION_EXEC_TYPE.CLUB_KUAJI_SPOINTCHANGE_ADD : UNION_EXEC_TYPE.CLUB_KUAJI_SPOINTCHANGE_SUB;

        }

        /**
         * @param
         * @return
         */
        public static UNION_EXEC_TYPE getUnionExecTypeExamineOp(boolean isAdd) {
            return isAdd ? UNION_EXEC_TYPE.UNION_EXEC_EXAMINE_SPORTS_POINT_ADD_OP : UNION_EXEC_TYPE.UNION_EXEC_EXAMINE_SPORTS_POINT_MINUS_OP;
        }

        /**
         * @param isManage
         * @return
         */
        public static UNION_EXEC_TYPE getUnionExecTypeGaneralSelf(boolean isManage, boolean isAdd) {
            if (isManage) {
                return isAdd ? UNION_EXEC_TYPE.UNION_EXEC_EMPOWER_SPORTS_POINT_ADD_SELF : UNION_EXEC_TYPE.UNION_EXEC_EMPOWER_SPORTS_POINT_MINUS_SELF;
            } else {
                return isAdd ? UNION_EXEC_TYPE.UNION_EXEC_COMPENSATE_SPORTS_POINT_ADD_SELF : UNION_EXEC_TYPE.UNION_EXEC_COMPENSATE_SPORTS_POINT_MINUS_SELF;
            }
        }

        public static boolean isPlayerName(int value) {
            return !isNotPlayerName(value) || value >= 100 && value < 122 || value == 126 || value >= 1000;
        }

        public static boolean isNotPlayerName(int value) {
            // 直接加入 || 直接退出 || 有人审核加入 || 有人审核退出 || 踢出 || 邀请||联盟预警值关闭||联盟预警值修改
            return UNION_EXEC_JIARU_NOT.value() == value || UNION_EXEC_TUICHU_NOT.value() == value || UNION_EXEC_JIARU.value() == value
                    || UNION_EXEC_TUICHU.value() == value || UNION_EXEC_TICHU.value() == value || UNION_EXEC_YAOQING.value() == value
                    || UNION_CLUB_EXEC_SPORTS_WARNING_CLOSE.value() == value || UNION_CLUB_EXEC_SPORTS_WARNING_CHANGE.value() == value;
        }


        public static boolean isShowClubName(int value) {
            // 直接加入
            return UNION_EXEC_JIARU_NOT.value() == value
                    // 直接退出
                    || UNION_EXEC_TUICHU_NOT.value() == value
                    // 有人审核加入
                    || UNION_EXEC_JIARU.value() == value
                    // 有人审核退出
                    || UNION_EXEC_TUICHU.value() == value
                    // 踢出
                    || UNION_EXEC_TICHU.value() == value
                    // 邀请
                    || UNION_EXEC_YAOQING.value() == value
                    // 增加竞技点
                    || UNION_EXEC_SPORTS_POINT_ADD.value() == value
                    // 减少竞技点
                    || UNION_EXEC_SPORTS_POINT_MINUS.value() == value
                    // 授权增加比赛分
                    || UNION_EXEC_EMPOWER_SPORTS_POINT_ADD.value() == value
                    // 授权减少比赛分
                    || UNION_EXEC_EMPOWER_SPORTS_POINT_MINUS.value() == value
                    // 补偿增加比赛分
                    || UNION_EXEC_COMPENSATE_SPORTS_POINT_ADD.value() == value
                    // 补偿减少比赛分
                    || UNION_EXEC_COMPENSATE_SPORTS_POINT_MINUS.value() == value
                    // 联赛授权增加比赛分
                    || UNION_EXEC_EMPOWER_SPORTS_POINT_ADD_SELF.value() == value
                    // 联赛授权减少比赛分
                    || UNION_EXEC_EMPOWER_SPORTS_POINT_MINUS_SELF.value() == value
                    // 联赛补偿增加比赛分
                    || UNION_EXEC_COMPENSATE_SPORTS_POINT_ADD_SELF.value() == value

                    // 联赛补偿减少比赛分
                    || UNION_EXEC_COMPENSATE_SPORTS_POINT_MINUS_SELF.value() == value
                    // 修改积分分成
                    || UNION_EXEC_SCORE_PERCENT.value() == value
                    // 审核增加比赛分
                    || UNION_EXEC_EXAMINE_SPORTS_POINT_ADD_EXE.value() == value
                    // 审核减少比赛分
                    || UNION_EXEC_EXAMINE_SPORTS_POINT_MINUS_EXE.value() == value
                    // 审核增加比赛分
                    || UNION_EXEC_EXAMINE_SPORTS_POINT_ADD_OP.value() == value
                    // 审核减少比赛分
                    || UNION_EXEC_EXAMINE_SPORTS_POINT_MINUS_OP.value() == value
                    // 设为管理员
                    || UNION_EXEC_BECOME_MGR.value() == value
                    // 取消管理员
                    || UNION_EXEC_CANCEL_MGR.value() == value
                    // 联盟预警值关闭
                    || UNION_CLUB_EXEC_SPORTS_WARNING_CLOSE.value() == value
                    // 联盟预警值修改
                    || UNION_CLUB_EXEC_SPORTS_WARNING_CHANGE.value() == value
                    // 预留值修改
                    || UNION_EXEC_RESERVED_VALUE_CHANGE.value() == value
                    //  生存积分变换
                    || UNION_ALIVE_SPORTS_CHANGE.value() == value
                    // 个人淘汰分变换
                    || CLUB_ELIMINATE_POINT_CHANGE.value() == value
                    // 预留值修改
                    || UNION_EXEC_RESERVED_VALUE_CHANGE.value() == value
                    //切牌收入
                    || UNION_ROOM_QIEPAI_INCOME.value() == value
                    //跨级变换增加
                    || CLUB_KUAJI_SPOINTCHANGE_ADD.value() == value
                    //跨级变换减少
                    || CLUB_KUAJI_SPOINTCHANGE_SUB.value() == value
                    //直属玩家被踢出
                    || CLUB_ZHI_SHU_TICHU.value() == value
                    //直属玩家加入
                    || CLUB_ZHI_SHU_JIARU.value() == value
                    //直属玩家被修改归属
                    || CLUB_ZHI_SHU_CHANGE_BELONG.value() == value;
        }


        public static boolean isShowPreValue(int value) {

            return

                    // 授权增加比赛分
                    UNION_EXEC_EMPOWER_SPORTS_POINT_ADD.value() == value
                            // 授权减少比赛分
                            || UNION_EXEC_EMPOWER_SPORTS_POINT_MINUS.value() == value
                            // 补偿增加比赛分
                            || UNION_EXEC_COMPENSATE_SPORTS_POINT_ADD.value() == value
                            // 补偿减少比赛分
                            || UNION_EXEC_COMPENSATE_SPORTS_POINT_MINUS.value() == value
                            // 被审核增加比赛分
                            || UNION_EXEC_EXAMINE_SPORTS_POINT_ADD_OP.value() == value
                            // 被审核增加比赛分
                            || UNION_EXEC_EXAMINE_SPORTS_POINT_MINUS_OP.value() == value;
        }

    }

    /**
     * 赛事职务类型
     */
    public enum UNION_POST_TYPE {
        /**
         * 赛事创建者
         */
        UNION_CREATE(3),
        /**
         * 赛事管理员
         */
        UNION_MANAGE(2),
        /**
         * 赛事亲友圈创造者
         */
        UNION_CLUB(1),
        /**
         * 赛事普通成员
         */
        UNION_GENERAL(0);

        private int value;

        UNION_POST_TYPE(int value) {
            this.value = value;
        }

        public int value() {
            return this.value;
        }

        public static UnionDefine.UNION_POST_TYPE valueOf(int value) {
            for (UnionDefine.UNION_POST_TYPE flow : UnionDefine.UNION_POST_TYPE.values()) {
                if (flow.value == value) {
                    return flow;
                }
            }
            return UnionDefine.UNION_POST_TYPE.UNION_CLUB;
        }
    }

    /**
     * 赛事：加入申请
     */
    public enum UNION_JOIN {
        /**
         * 0需要审核
         */
        UNION_JOIN_NEED_AUDIT,
        /**
         * 1不需要审核
         */
        UNION_JOIN_NO_NEED_AUDIT,
        ;

        public static UnionDefine.UNION_JOIN valueOf(int value) {
            for (UnionDefine.UNION_JOIN flow : UnionDefine.UNION_JOIN.values()) {
                if (flow.ordinal() == value) {
                    return flow;
                }
            }
            return UnionDefine.UNION_JOIN.UNION_JOIN_NEED_AUDIT;
        }
    }

    /**
     * 赛事：退出申请
     */
    public enum UNION_QUIT {
        /**
         * 0需要审核
         */
        UNION_QUIT_NEED_AUDIT,
        /**
         * 1不需要审核
         */
        UNION_QUIT_NO_NEED_AUDIT,
        ;

        public static UnionDefine.UNION_QUIT valueOf(int value) {
            for (UnionDefine.UNION_QUIT flow : UnionDefine.UNION_QUIT.values()) {
                if (flow.ordinal() == value) {
                    return flow;
                }
            }
            return UnionDefine.UNION_QUIT.UNION_QUIT_NEED_AUDIT;
        }
    }


    /**
     * 赛事：加入申请
     */
    public enum UNION_JOIN_CLUB_SAME_UNION {
        /**
         * 0需要审核
         */
        UNION_JOIN_NEED_AUDIT,
        /**
         * 1不需要审核
         */
        UNION_JOIN_NO_NEED_AUDIT,
        ;

        public static UnionDefine.UNION_JOIN_CLUB_SAME_UNION valueOf(int value) {
            for (UnionDefine.UNION_JOIN_CLUB_SAME_UNION flow : UnionDefine.UNION_JOIN_CLUB_SAME_UNION.values()) {
                if (flow.ordinal() == value) {
                    return flow;
                }
            }
            return UnionDefine.UNION_JOIN_CLUB_SAME_UNION.UNION_JOIN_NEED_AUDIT;
        }
    }

    /**
     * 联赛显示的桌子数量
     */
    public enum UNION_QUIT_TABLENUM {
        /**
         * 全部
         */
        TABLENUM_ALL(0),
        /**
         * 5
         */
        TABLENUM_FIVE(5),
        /**
         * 5
         */
        TABLENUM_TEN(10),
        /**
         * 5
         */
        TABLENUM_TWENTI(20),
        ;
        private int value;

        UNION_QUIT_TABLENUM(int value) {
            this.value = value;
        }

        public int value() {
            return this.value;
        }

        public static UnionDefine.UNION_QUIT_TABLENUM valueOf(int value) {
            for (UnionDefine.UNION_QUIT_TABLENUM flow : UnionDefine.UNION_QUIT_TABLENUM.values()) {
                if (flow.ordinal() == value) {
                    return flow;
                }
            }
            return UnionDefine.UNION_QUIT_TABLENUM.TABLENUM_ALL;
        }
    }

    /**
     * 赛事：魔法表情
     */
    public enum UNION_EXPRESSION {
        /**
         * 0可以使用
         */
        UNION_EXPRESSION_USE,
        /**
         * 1不可以使用
         */
        UNION_EXPRESSION_NO_USE,
        ;

        public static UnionDefine.UNION_EXPRESSION valueOf(int value) {
            for (UnionDefine.UNION_EXPRESSION flow : UnionDefine.UNION_EXPRESSION.values()) {
                if (flow.ordinal() == value) {
                    return flow;
                }
            }
            return UnionDefine.UNION_EXPRESSION.UNION_EXPRESSION_USE;
        }
    }

    /**
     * 赛事：状态
     */
    public enum UNION_STATE {
        /**
         * 0启用
         */
        UNION_STATE_ENABLE,
        /**
         * 1停用
         */
        UNION_STATE_STOP,
        /**
         * 2奖励不足
         */
        UNION_STATE_NOT_ENOUGH_REWARD;

        public static UnionDefine.UNION_STATE valueOf(int value) {
            for (UnionDefine.UNION_STATE flow : UnionDefine.UNION_STATE.values()) {
                if (flow.ordinal() == value) {
                    return flow;
                }
            }
            return UnionDefine.UNION_STATE.UNION_STATE_ENABLE;
        }

        /**
         * 启用状态
         *
         * @param value
         * @return
         */
        public static boolean isEnable(int value) {
            return value == UNION_STATE_ENABLE.ordinal();

        }

    }

    /**
     * 赛事：竞技点
     */
    public enum UNION_SPORTS {
        /**
         * 0不清零
         */
        UNION_SPORTS_NO_CLEAR,
        /**
         * 1每天清零
         */
        UNION_SPORTS_CLEAR_DAILY,
        /**
         * 2每周清零
         */
        UNION_SPORTS_CLEAR_WEEKLY,
        /**
         * 3每月清零
         */
        UNION_SPORTS_CLEAR_MONTHLY,
        ;

        public static UnionDefine.UNION_SPORTS valueOf(int value) {
            for (UnionDefine.UNION_SPORTS flow : UnionDefine.UNION_SPORTS.values()) {
                if (flow.ordinal() == value) {
                    return flow;
                }
            }
            return UnionDefine.UNION_SPORTS.UNION_SPORTS_NO_CLEAR;
        }
    }


    /**
     * 赛事竞技点
     */
    public enum UNION_SPORTS_POINT {
        /**
         * 0增加
         */
        UNION_SPORTS_POINT_ADD,
        /**
         * 1减去
         */
        UNION_SPORTS_POINT_MINUS;

        public static UnionDefine.UNION_SPORTS_POINT valueOf(int value) {
            for (UnionDefine.UNION_SPORTS_POINT flow : UnionDefine.UNION_SPORTS_POINT.values()) {
                if (flow.ordinal() == value) {
                    return flow;
                }
            }
            return UnionDefine.UNION_SPORTS_POINT.UNION_SPORTS_POINT_ADD;
        }
    }

    /**
     * 赛事通知类型
     */
    public enum UNION_NOTIFY_TYPE {
        /**
         * 默认0
         */
        UNION_NOTIFY_NOT(0),
        /**
         * 加入
         */
        UNION_NOTIFY_JIARU(1),
        /**
         * 退出
         */
        UNION_NOTIFY_TUICHU(2),
        /**
         * 踢出
         */
        UNION_NOTIFY_TICHU(3),
        /**
         * 邀请
         */
        UNION_NOTIFY_YAOQING(4),
        /**
         * 拒绝
         */
        UNION_NOTIFY_JUJIE(5),

        ;

        private int value;

        UNION_NOTIFY_TYPE(int value) {
            this.value = value;
        }

        public int value() {
            return this.value;
        }

        public static UnionDefine.UNION_NOTIFY_TYPE valueOf(int value) {
            for (UnionDefine.UNION_NOTIFY_TYPE flow : UnionDefine.UNION_NOTIFY_TYPE.values()) {
                if (flow.value() == value) {
                    return flow;
                }
            }
            return UnionDefine.UNION_NOTIFY_TYPE.UNION_NOTIFY_NOT;
        }
    }

    public enum UNION_ROOM_SPORTS_TYPE {
        /**
         * 大赢家
         */
        BIG_WINNER,
        /**
         * 每人消耗
         */
        EVERYONE,
        /**
         * 赢家消耗
         */
        EVERYWIN
    }

    /**
     * 联赛竞技点不足
     */
    public enum UNION_SPORTS_POINT_NOT_ENOUGH {
        /**
         * 竞技点不足
         */
        SPORTS_POINT_NOT_ENOUGH,
        /**
         * 竞技点足够
         */
        SPORTS_POINT_ENOUGH
    }

    /**
     * 类型查看
     */
    public enum UNION_DYNAMIC_CHOOSE_TYPE {
        /**
         * 全部
         */
        ALL(0),
        /**
         * 异常操作
         */
        ERROR(1),
        /**
         * 对局输赢
         */
        WINLOSE(2),
        /**
         * 报名费
         */
        ENTRYFEE(3),
        /**
         * 洗牌费用
         */
        XiPaiCost(4),
        /**
         * 分成修改
         */
        FenChengChange(5),
        /**
         * 保险箱分数变化
         */
        CasePointChange(6),
        /**
         * 报名费(新)
         */
        ENTRYFEE_NEW(7),
        /**
         * 生存任务
         */
        ALIVE_MISSION(8),
        /**
         * 淘汰分 跨级异常操作
         */
        TaoTaiFen(9),
        /**
         * 跨级异常操作
         */
        KuaJi(10),
        /**
         * 人员变动
         */
        RenYuanBianDong(11),
        ;
        private int value;

        UNION_DYNAMIC_CHOOSE_TYPE(int value) {
            this.value = value;
        }

        public int value() {
            return this.value;
        }

        public static UNION_DYNAMIC_CHOOSE_TYPE valueOf(int value) {
            for (UnionDefine.UNION_DYNAMIC_CHOOSE_TYPE flow : UnionDefine.UNION_DYNAMIC_CHOOSE_TYPE.values()) {
                if (flow.value() == value) {
                    return flow;
                }
            }
            return UnionDefine.UNION_DYNAMIC_CHOOSE_TYPE.ALL;
        }

    }

    /**
     *
     */
    public enum UNION_CREATE_LIMIT {
        /**
         * 排名最高限制50
         */
        RANKING_LIMIT(0, 50),
        /**
         * 名字限制
         */
        NAME_LIMIT(1, 16),
        /**
         * 加入申请
         */
        JOIN_LIMIT(0, 1),
        /**
         * 退出申请
         */
        QUIT_LIMIT(0, 1),
        /**
         * 赛事状态
         */
        STATE_LIMIT(0, 1),
        /**
         * 裁判力度
         */
        INIT_SPORTS_LIMIT(0, -1),
        /**
         * 比赛频率
         */
        MATCH_RATE_LIMIT(0, 2),
        /**
         * 消耗类型
         */
        PRIZE_TYPE_LIMIT(1, 2),
        /**
         * 数量限制
         */
        VALUE_LIMIT(0, -1),
        /**
         * 允许亲友圈添加同赛事玩家
         */
        JOIN_CLUB_SAME_UNION(0, 1);
        private int min;
        private int max;

        UNION_CREATE_LIMIT(int min, int max) {
            this.min = min;
            this.max = max;
        }

        public int getMin() {
            return min;
        }

        public int getMax() {
            return max;
        }

        public final static boolean checkLimit(UNION_CREATE_LIMIT limit, int value) {
            if (limit.max == -1) {
                return value >= limit.min;
            }
            return value >= limit.min && value <= limit.max;
        }
    }

    /**
     * 联赛比赛频率
     */
    public enum UNION_MATCH_RATE {
        /**
         * 30天
         */
        DAY_30(30),
        /**
         * 7天
         */
        DAY_7(7),
        /**
         * 1天
         */
        DAY_1(1),
        ;
        private int value;

        UNION_MATCH_RATE(int value) {
            this.value = value;
        }

        public int value() {
            return value;
        }

        public static UnionDefine.UNION_MATCH_RATE valueOf(int value) {
            for (UnionDefine.UNION_MATCH_RATE flow : UnionDefine.UNION_MATCH_RATE.values()) {
                if (flow.ordinal() == value) {
                    return flow;
                }
            }
            return UnionDefine.UNION_MATCH_RATE.DAY_30;
        }
    }


    /**
     * 联赛比赛状态
     */
    public enum UNION_MATCH_STATE {
        /**
         * 比赛进行中
         */
        MATCH_PLAYING(1),
        /**
         * 复赛申请中
         */
        APPLY_REMATCH(2),
        /**
         * 退赛申请中
         */
        BACK_OFF(3),
        ;
        private int value;

        UNION_MATCH_STATE(int value) {
            this.value = value;
        }

        public int value() {
            return value;
        }

        public static UnionDefine.UNION_MATCH_STATE valueOf(int value) {
            for (UnionDefine.UNION_MATCH_STATE flow : UnionDefine.UNION_MATCH_STATE.values()) {
                if (flow.value == value) {
                    return flow;
                }
            }
            return UnionDefine.UNION_MATCH_STATE.MATCH_PLAYING;
        }
    }

    /**
     * 赛事审核类型
     */
    public enum UNION_EXAMINE_TYPE {
        /**
         * 审核加入
         */
        EXAMINE_JOIN,
        /**
         * 审核退出
         */
        EXAMINE_QUIT,
        /**
         * 审核退赛
         */
        EXAMINE_BACK_OFF,
        /**
         * 审核复赛
         */
        EXAMINE_APPLY_REMATCH;

        public static boolean checkJoinOrQuit(int value) {
            return EXAMINE_JOIN.ordinal() == value || EXAMINE_QUIT.ordinal() == value;
        }

        public static boolean checkMatch(int value) {
            return EXAMINE_BACK_OFF.ordinal() == value || EXAMINE_APPLY_REMATCH.ordinal() == value;
        }
    }


    /**
     * 赛事回合状态
     */
    public enum UNION_ROUND_STATE {
        /**
         * 当前回合
         */
        ROUND_CUR,
        /**
         * 上回合
         */
        ROUND_UP;

    }

    /**
     * 赛事排名类型
     */
    public enum UNION_RANKING_TYPE {
        /**
         * 当前回合
         */
        ROUND_CUR,
        /**
         * 上回合
         */
        ROUND_UP;

    }

    /**
     * 赛事分成类型
     */
    public enum UNION_SHARE_TYPE {
        /**
         * 百分比
         */
        PERCENT,
        /**
         * 固定值
         */
        FIXED,
        /**
         * 区间
         */
        SECTION,
        ;

        public static UnionDefine.UNION_SHARE_TYPE valueOf(int value) {
            for (UnionDefine.UNION_SHARE_TYPE flow : UnionDefine.UNION_SHARE_TYPE.values()) {
                if (flow.ordinal() == value) {
                    return flow;
                }
            }
            return null;
        }

    }

    /**
     * 赛事预警类型
     */
    public enum UNION_WARN_EXAMINE {
        /**
         * 关闭
         */
        CLOSE,
        /**
         * 开启
         */
        OPEN,
        /**
         * 自动
         */
        AUTO;

        public static UnionDefine.UNION_WARN_EXAMINE valueOf(int value) {
            for (UnionDefine.UNION_WARN_EXAMINE flow : UnionDefine.UNION_WARN_EXAMINE.values()) {
                if (flow.ordinal() == value) {
                    return flow;
                }
            }
            return null;
        }

    }

    /**
     * 赛事预警类型
     */
    public enum UNION_WARN_STATUS {
        /**
         * 关闭
         */
        CLOSE,
        /**
         * 开启
         */
        OPEN;

        public static UnionDefine.UNION_WARN_STATUS valueOf(int value) {
            for (UnionDefine.UNION_WARN_STATUS flow : UnionDefine.UNION_WARN_STATUS.values()) {
                if (flow.ordinal() == value) {
                    return flow;
                }
            }
            return null;
        }

    }

    /**
     * 赛事预警类型
     */
    public enum UNION_CASE_STATUS {
        /**
         * 关闭
         */
        CLOSE,
        /**
         * 开启
         */
        OPEN,
        /**
         * 开启并分成到保险箱
         */
        OPENSHARE;

        public static UnionDefine.UNION_CASE_STATUS valueOf(int value) {
            for (UnionDefine.UNION_CASE_STATUS flow : UnionDefine.UNION_CASE_STATUS.values()) {
                if (flow.ordinal() == value) {
                    return flow;
                }
            }
            return null;
        }

    }

    /**
     * 联赛分成区间
     */
    public enum UNION_SHARE_SECTION {
        SECTION_1(0, 1),
        SECTION_2(1, 2),
        SECTION_3(2, 3),
        SECTION_4(3, 4),
        SECTION_5(4, 5),
        SECTION_6(5, 6),
        SECTION_7(6, 7),
        SECTION_8(7, 8),
        SECTION_9(8, 9),
        SECTION_10(9, 10),
        SECTION_11(10, 20),
        SECTION_12(20, 50),
        SECTION_13(50, 100),
        SECTION_14(100, 500),
        SECTION_15(500, 1000),
        SECTION_16(1000, 1000),
        ;
        private int beginValue;
        private int endValue;

        UNION_SHARE_SECTION(int beginValue, int endValue) {
            this.beginValue = beginValue;
            this.endValue = endValue;
        }

        public int beginValue() {
            return beginValue;
        }

        public int endValue() {
            return endValue;
        }

    }

    /**
     * 联赛类型
     */
    public enum UNION_TYPE {
        /**
         * 正常
         */
        NORMAL(0),
        /**
         * 中至
         */
        ZhongZhi(1),

        ;
        private int value;

        UNION_TYPE(int value) {
            this.value = value;
        }

        public int value() {
            return value;
        }

        public static UnionDefine.UNION_TYPE valueOf(int value) {
            for (UnionDefine.UNION_TYPE flow : UnionDefine.UNION_TYPE.values()) {
                if (flow.value == value) {
                    return flow;
                }
            }
            return UnionDefine.UNION_TYPE.NORMAL;
        }
    }

    /**
     * 联赛皮肤类型
     */
    public enum UNION_SKIN_TYPE {
        /**
         * 皮肤一
         */
        SKIN_ONE(0),
        /**
         * 皮肤二
         */
        SKIN_TWO(1),
        /**
         * 中至皮肤
         */
        ZhongZhi(2),

        ;
        private int value;

        UNION_SKIN_TYPE(int value) {
            this.value = value;
        }

        public int value() {
            return value;
        }

        public static UnionDefine.UNION_SKIN_TYPE valueOf(int value) {
            for (UnionDefine.UNION_SKIN_TYPE flow : UnionDefine.UNION_SKIN_TYPE.values()) {
                if (flow.value == value) {
                    return flow;
                }
            }
            return UnionDefine.UNION_SKIN_TYPE.SKIN_ONE;
        }
    }

    /**
     * 联赛关闭开启
     */
    public enum UNION_CLOSE_OPEN {
        /**
         * 关闭
         */
        CLOSE(0),
        /**
         * 开启
         */
        OPEN(1),

        ;
        private int value;

        UNION_CLOSE_OPEN(int value) {
            this.value = value;
        }

        public int value() {
            return value;
        }

        public static UnionDefine.UNION_CLOSE_OPEN valueOf(int value) {
            for (UnionDefine.UNION_CLOSE_OPEN flow : UnionDefine.UNION_CLOSE_OPEN.values()) {
                if (flow.value == value) {
                    return flow;
                }
            }
            return UNION_CLOSE_OPEN.CLOSE;
        }
    }

    /**
     * 中至排行榜数据
     * 参与房间数、参与小局数、积分(房间输赢分)、大赢家(大赢家次数)、比赛最高分(单局最高分)。
     */
    public enum UNION_ZHONGZHI_RANKED_ITEM {
        /**
         * 参与房间数
         */
        ROOM_NUM(0),
        /**
         * 参与小局数
         */
        SET_NUM(1),
        /**
         * 积分
         */
        WIN_LOSE_POINT(2),
        /**
         * 大赢家
         */
        BIG_WINER(3),
        /**
         * 比赛最高分
         */
        MAX_WIN_LOSE_POINT(4),

        ;
        private int value;

        UNION_ZHONGZHI_RANKED_ITEM(int value) {
            this.value = value;
        }

        public int value() {
            return value;
        }

        public static UnionDefine.UNION_ZHONGZHI_RANKED_ITEM valueOf(int value) {
            for (UnionDefine.UNION_ZHONGZHI_RANKED_ITEM flow : UnionDefine.UNION_ZHONGZHI_RANKED_ITEM.values()) {
                if (flow.value == value) {
                    return flow;
                }
            }
            return UNION_ZHONGZHI_RANKED_ITEM.ROOM_NUM;
        }
    }
}
