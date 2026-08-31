package business.rocketmq.constant;

/**
 * @author : xushaojun
 * create at:  2020-08-12  11:46
 * @description: mq订阅主题
 */
public class MqTopic {
    //创建亲友圈房间
    public static final String CLUB_CREATE_ROOM = "clubcreateroom";
    //创建房间返回大厅
    public static final String CLUB_CREATE_ROOM_BACK = "clubcreateroomback";
    //亲友圈会员通知
    public static final String CLUB_MEMBER_NOTIFY = "clubmembernotify";
    //赛事会员通知
    public static final String UNION_ALL_BY_UNION_NOTIFY = "notify2AllByUnion";
    //亲友圈会员通知
    public static final String CLUB_ALL_BY_CLUB_NOTIFY = "notify2allbyclub";
    //创建普通房间
    public static final String BASE_CREATE_ROOM = "basecreateroom";
    //创建普通房间返回大厅
    public static final String BASE_CREATE_ROOM_BACK = "basecreateroomback";
    //继续房间
    public static final String BASE_CONTINUE_ROOM = "basecontinueroom";
    //继续房间返回大厅
    public static final String BASE_CONTINUE_ROOM_BACK = "basecontinueroomback";
    //创建玩家
    public static final String PLAYER_CREATE = "playercreate";
    //进入房间
    public static final String BASE_ENTER_ROOM = "baseenterroom";
    //换房间
    public static final String BASE_CHANGE_ROOM = "basechangeroom";
    //退出房间
    public static final String BASE_EXIT_ROOM = "baseexitroom";
    //进入房间返回大厅
    public static final String BASE_ENTER_ROOM_BACK = "baseenterroomback";
    public static final String BASE_SEAT_ROOM_BACK = "baseseatroomback";
    public static final String BASE_LEAVE_POS_BACK = "baseleaveposback";
    //亲友圈会员更新通知
    public static final String CLUB_UPDATE_MEMBER_BO_NOTIFY = "clubupdatememberbonotify";
    //邀请在线好友
    public static final String ROOM_INVITATION_OPERATION_NOTIFY = "roominvitationoperationnotify";
    //联盟踢出玩家
    public static final String UNION_KICK_NOTIFY = "unionkicknotify";
    //亲友圈踢出玩家
    public static final String CLUB_KICK_NOTIFY = "clubkicknotify";
    //所有返回大厅的请求
    public static final String ALL_HALL_BACK = "allhallback";
    //亲友圈解散房间通知
    public static final String CLUB_DISSOLVE_ROOM_NOTIFY = "clubdissolveroomnotify";
    //赛事解散房间通知
    public static final String UNION_DISSOLVE_ROOM_NOTIFY = "uniondissolveroomnotify";
    //玩家是否丢掉连接
    public static final String PLAYER_LOST_CONNECT_NOTIFY = "playerlostconnectnotify";
    //玩家是否丢掉连接
    public static final String GAME_START_TO_HALL_NOTIFY = "gamestarttohallnotify";
    //玩家信息变化通知
    public static final String PLAYER_CHANGE_NOTIFY = "playerchangenotify";
    //重新加载游戏配置
    public static final String HTTP_RELOAD_GAME_LIST_CONFIG = "httpreloadgamelistconfig";
    //重新加载禁止城市
    //重新加载字典配置
    public static final String HTTP_RELOAD_DICTIONARIES_CONFIG = "HTTP_RELOAD_DICTIONARIES_CONFIG";
    //重新加载配置表
    public static final String HTTP_RELOAD_CONFIG = "httpreloadconfig";
    //玩家推送通知
    public static final String PLAYER_PUSH_PROTO = "playerpushproto";
    //紧急维护
    public static final String URGENT_MAINTAIN_SERVER = "urgentmaintainserver";
    //设置维护时间
    public static final String SET_MAINTAIN_SERVER = "setmaintainserver";
    //维护游戏
    public static final String DO_MAINTAIN_SERVER = "domaintainserver";
    //关闭节点
    public static final String STOP_SERVER = "stopserver";
    //玩家踢出节点
    public static final String KICK_OUT_SERVER = "kickoutserver";
    //玩家踢出游戏
    public static final String KICK_OUT_GAME = "kickoutgame";
    //后台提出房间
    public static final String ON_GM_EXIT_ROOM = "ongmexitroom";
    //解散房间通知
    public static final String DISSOLVE_ROOM_NOTIFY = "dissolveroomnotify";
    //比赛分修改通知
    public static final String ROOM_SPORTS_POINT_NOTIFY = "roomsportspointnotify";
    //亲友圈所有成员本地数据更新
    public static final String CLUB_MEMBER_ALL_UPDATE = "clubmemberallupdate";
    //亲友圈会员删除
    public static final String CLUB_DELETE_MEMBER_BO_NOTIFY = "clubdeletememberbonotify";
    //亲友圈会员通知管理者大厅
    public static final String CLUB_ALL_BY_CLUB_MINISTER_NOTIFY = "cluballbyclubminsternotify";
    //亲友圈会员通知管理者游戏大厅
    public static final String CLUB_ALL_MINISTER_BY_CLUB_NOTIFY = "cluballministerbyclubnotify";
    //通知管理员和玩家
    public static final String CLUB_ALL_MINISTER_AND_PID_BY_CLUB_NOTIFY = "cluballministerandpidbyclubnotify";
    //通知赛事玩家
    public static final String UNION_CLUB_ALL_MEMBER_NOTIFY = "UNIONCLUBALLMEMBERNOTIFY";
    //亲友圈会员添加
    public static final String CLUB_INSERT_MEMBER_BO_NOTIFY = "clubinsertmemberbonotify";
    //本地亲友圈会员添加
    public static final String LOCAL_CLUB_MEMBER_ADD = "localclubmemberadd";
    //本地亲友圈会员删除
    public static final String LOCAL_CLUB_MEMBER_REMOVE = "localclubmemberremove";
    //本地房间添加
    public static final String LOCAL_ROOM_ADD = "localroomadd";
    //本地房间删除
    public static final String LOCAL_ROOM_REMOVE = "localroomremove";
    //本地玩家添加
    public static final String LOCAL_PLAYER_ADD = "localplayeradd";
    //本地玩家在线添加
    public static final String LOCAL_ONLINE_PLAYER_ADD = "localonlineplayeradd";
    //本地在线玩家删除
    public static final String LOCAL_ONLINE_PLAYER_REMOVE = "localonlineplayerremove";
    //解散指定赛事
    public static final String UNION_DISSOLVE_INIT_ROOM = "unionDissolveInitRoom";
    // 限制指定玩家登陆
    public static final String PLAYER_BANNED_LOGIN_NOTIFY = "playerbannedloginnotify";
    // 停止共享节点
    public static final String STOP_SHARE_NODE = "STOP_SHARE_NODE";
    // IP 服务节点变更通知
    public static final String IP_SERVER_NODE_SET_NOTIFY = "IP_SERVER_NODE_SET_NOTIFY";


}
