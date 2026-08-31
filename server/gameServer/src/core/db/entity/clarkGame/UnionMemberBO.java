package core.db.entity.clarkGame;

import business.global.shareunion.ShareUnionMemberMgr;
import com.ddm.server.annotation.DataBaseField;
import com.ddm.server.annotation.TableName;
import com.ddm.server.common.Config;
import core.db.entity.BaseEntity;
import core.db.other.AsyncInfo;
import core.ioc.Constant;
import jsproto.c2s.cclass.union.UnionDefine;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.collections4.MapUtils;

import java.util.HashMap;

/**
 * 大赛事成员表
 *
 * @author Huaxing
 */
@TableName(value = "bigUnionMember")
@Data
@NoArgsConstructor
public class UnionMemberBO extends BaseEntity<UnionMemberBO> {

    @DataBaseField(type = "bigint(20)", fieldname = "id", comment = "自增主key", indextype = DataBaseField.IndexType.Unique)
    private long id;
    @DataBaseField(type = "bigint(20)", fieldname = "unionId", comment = "赛事ID")
    private long unionId;
    @DataBaseField(type = "bigint(20)", fieldname = "clubId", comment = "亲友圈ID")
    private long clubId;
    @DataBaseField(type = "bigint(20)", fieldname = "clubOwnerId", comment = "亲友圈创建者ID")
    private long clubOwnerId;
    @DataBaseField(type = "int(2)", fieldname = "type", comment = "成员职务类型")
    private int type;
    @DataBaseField(type = "int(4)", fieldname = "status", comment = "0x01未批准,0x02已拒绝加入,0x04为已加入,0x08为已踢出,0x10为已邀请,0x20为拒绝邀请,0x40已退出")
    private int status;
    @DataBaseField(type = "int(11)", fieldname = "createTime", comment = "时间")
    private int createTime;
    @DataBaseField(type = "int(11)", fieldname = "updateTime", comment = "处理时间")
    private int updateTime;
    @DataBaseField(type = "int(11)", fieldname = "deleteTime", comment = "踢出时间")
    private int deleteTime;
    @DataBaseField(type = "bigint(20)", fieldname = "invitationPid", comment = "发送邀请的玩家Pid")
    private long invitationPid;
    @DataBaseField(type = "bigint(20)", fieldname = "clubMemberId", comment = "亲友圈成员Id")
    private long clubMemberId;
    @DataBaseField(type = "int(11)", fieldname = "topTime", comment = "置顶时间")
    private int topTime;
    @DataBaseField(type = "int(2)", fieldname = "warnStatus", comment = "预警状态（0:不预警,1:预警）")
    private int warnStatus;
    @DataBaseField(type = "double(11,2)", fieldname = "sportsPointWarning", comment = "预警值")
    private double sportsPointWarning;
    @DataBaseField(type = "int(2)", fieldname = "alivePointStatus", comment = "生存积分状态（0:不开启,1:开启）")
    private int alivePointStatus = 0;
    @DataBaseField(type = "double(11,2)", fieldname = "alivePoint", comment = "生存积分")
    private double alivePoint = 0d;

    public UnionDefine.UNION_POST_TYPE getPostType() {
        return UnionDefine.UNION_POST_TYPE.valueOf(this.getType());
    }

    /**
     * 保存切换创建者
     */
    public void saveChangeCreate(long clubMemberId, long clubOwnerId) {
        HashMap<String, Object> map = new HashMap<>();
        if (this.clubMemberId != clubMemberId) {
            this.clubMemberId = clubMemberId;
            map.put("clubMemberId", this.clubMemberId);
        }
        if (this.clubOwnerId != clubOwnerId) {
            this.clubOwnerId = clubOwnerId;
            map.put("clubOwnerId", this.clubOwnerId);
        }
        if (MapUtils.isNotEmpty(map)) {
            getBaseService().update(map, id, new AsyncInfo(id));
            if (Config.isShare()) {
                ShareUnionMemberMgr.getInstance().updateField(this, map);
            }
        }

    }

    public void saveCreateTime(int createTime) {
        if (this.createTime == createTime) {
            return;
        }
        this.createTime = createTime;
        getBaseService().update("createTime", createTime, id, new AsyncInfo(id));
    }

    public void saveStatus(int status) {
        if (this.status == status) {
            return;
        }
        this.setStatus(status);
        getBaseService().update("status", status, id, new AsyncInfo(id));
        if (Config.isShare()) {
            ShareUnionMemberMgr.getInstance().updateField(this, "status");
        }
    }

    public void saveWarnStatus(int warnStatus) {
        if (warnStatus == this.warnStatus) {
            return;
        }
        this.warnStatus = warnStatus;
        getBaseService().update("warnStatus", warnStatus, id, new AsyncInfo(id));
        if (Config.isShare()) {
            ShareUnionMemberMgr.getInstance().updateField(this, "warnStatus");
        }
    }

    public void saveSportsPointWarning(double sportsPointWarning) {
        if (sportsPointWarning == this.sportsPointWarning) {
            return;
        }
        this.sportsPointWarning = sportsPointWarning;
        getBaseService().update("sportsPointWarning", sportsPointWarning, id, new AsyncInfo(id));
        if (Config.isShare()) {
            ShareUnionMemberMgr.getInstance().updateField(this, "sportsPointWarning");
        }
    }

    public void saveAlivePoint(double alivePoint) {
        if (alivePoint == this.alivePoint) {
            return;
        }
        this.alivePoint = alivePoint;
        getBaseService().update("alivePoint", alivePoint, id, new AsyncInfo(id));
        if (Config.isShare()) {
            ShareUnionMemberMgr.getInstance().updateField(this, "alivePoint");
        }
    }

    public void saveAlivePointStatus(int alivePointStatus) {
        if (alivePointStatus == this.alivePointStatus) {
            return;
        }
        this.alivePointStatus = alivePointStatus;
        getBaseService().update("alivePointStatus", alivePointStatus, id, new AsyncInfo(id));
        if (Config.isShare()) {
            ShareUnionMemberMgr.getInstance().updateField(this, "alivePointStatus");
        }
    }

    /**
     * 保存职务类型
     *
     * @param type
     */
    public void saveType(int type) {
        if (this.type == type) {
            return;
        }
        this.type = type;
        getBaseService().update("type", type, id, new AsyncInfo(id));
        if (Config.isShare()) {
            ShareUnionMemberMgr.getInstance().updateField(this, "type");
        }
    }


    public void updateStatus(long invitationPid) {
        HashMap<String, Object> map = new HashMap<>();
        map.put("status", this.getStatus());
        map.put("createTime", this.getCreateTime());
        map.put("updateTime", this.getUpdateTime());
        map.put("deleteTime", this.getDeleteTime());
        if (invitationPid > 0L) {
            map.put("invitationPid", invitationPid);
            this.setInvitationPid(invitationPid);
        }
        this.getBaseService().update(map, getId());
        if (Config.isShare()) {
            ShareUnionMemberMgr.getInstance().updateField(this, map);
        }
    }

    public static String getSql_TableCreate() {
        String sql = "CREATE TABLE IF NOT EXISTS `bigUnionMember` ("
                + "`id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '自增主键',"
                + "`unionId` bigint(20) NOT NULL DEFAULT '0' COMMENT '赛事ID',"
                + "`clubId` bigint(20) NOT NULL DEFAULT '0' COMMENT '亲友圈ID',"
                + "`clubOwnerId` bigint(20) NOT NULL DEFAULT '0' COMMENT '亲友圈创建者ID',"
                + "`type` int(2) NOT NULL DEFAULT '0' COMMENT '成员职务类型',"
                + "`status` int(4) NOT NULL DEFAULT '0' COMMENT '玩家状态 0x01未批准,0x02已拒绝加入,0x04为已加入,0x08为已踢出,0x10为已邀请,0x20为拒绝邀请,0x40已退出',"
                + "`createTime` int(11) NOT NULL DEFAULT '0' COMMENT '时间',"
                + "`updateTime` int(11) NOT NULL DEFAULT '0'  COMMENT '处理时间',"
                + "`deleteTime` int(11) NOT NULL DEFAULT '0'  COMMENT '踢出时间',"
                + "`invitationPid` bigint(20) NOT NULL DEFAULT 0  COMMENT '发送邀请的玩家Pid',"
                + "`clubMemberId` bigint(20) NOT NULL DEFAULT 0  COMMENT '亲友圈成员Id',"
                + "`topTime` int(11) NOT NULL DEFAULT '0'  COMMENT '置顶时间',"
                + "`warnStatus` int(2) NOT NULL DEFAULT 0  COMMENT '预警状态（0:不预警,1:预警）',"
                + "`sportsPointWarning` double(11,2) NOT NULL DEFAULT 0  COMMENT '预警值',"
                + "PRIMARY KEY (`id`),"
                + "UNIQUE KEY `c_id_unionId` (`unionId`,`clubId`) USING BTREE"
                + ") COMMENT='大赛事成员表'  DEFAULT CHARSET=utf8 AUTO_INCREMENT=" + (Constant.InitialID + 1);
        return sql;
    }

}
