package core.db.entity.clarkGame;

import com.ddm.server.annotation.DataBaseField;
import com.ddm.server.annotation.TableName;
import com.ddm.server.common.utils.StringUtil;
import core.db.entity.BaseEntity;
import core.db.other.AsyncInfo;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@TableName(value = "family")
@Data
@NoArgsConstructor
public class FamilyBO extends BaseEntity<FamilyBO> {

    @DataBaseField(type = "bigint(20)", fieldname = "id", comment = "自增主key", indextype = DataBaseField.IndexType.Unique)
    private long id;
    @DataBaseField(type = "bigint(20)", fieldname = "familyID", comment = "代理ID")
    private long familyID;
    @DataBaseField(type = "varchar(120)", fieldname = "name", comment = "工会名")
    private String name = "";
    @DataBaseField(type = "bigint(20)", fieldname = "createTime", comment = "创建时间毫秒")
    private long createTime;
    @DataBaseField(type = "int(2)", fieldname = "status", comment = "1为正在使用,2为已撤销")
    private int status;
    @DataBaseField(type = "bigint(20)", fieldname = "totalRMB", comment = "累计充值")
    private long totalRMB;
    @DataBaseField(type = "bigint(20)", fieldname = "ownerID", comment = "会长ID")
    private long ownerID;
    @DataBaseField(type = "int(11)", fieldname = "fencheng", comment = "分成")
    private int fencheng;
    @DataBaseField(type = "varchar(255)", fieldname = "beizhu", comment = "备注")
    private String beizhu = "";
    @DataBaseField(type = "varchar(255)", fieldname = "haveYouxi", comment = "游戏表")
    private String haveYouxi = "";
    @DataBaseField(type = "bigint(20)", fieldname = "recommend", comment = "推荐代理ID")
    private long recommend;
    @DataBaseField(type = "int(11)", fieldname = "minTixian", comment = "最小提现")
    private int minTixian;
    @DataBaseField(type = "int(11)", fieldname = "roomcardNum", comment = "房卡数量")
    private int roomcardNum;
    @DataBaseField(type = "int(11)", fieldname = "higherLevel", comment = "给上级代理分成比")
    private int higherLevel;
    @DataBaseField(type = "int(11)", fieldname = "lowerLevel", comment = "给下级代理分成比")
    private int lowerLevel;
    @DataBaseField(type = "int(11)", fieldname = "clubLevel", comment = "给亲友圈代理分成比")
    private int clubLevel;
    @DataBaseField(type = "bigint(20)", fieldname = "clubTotalRMB", comment = "亲友圈累计充值")
    private long clubTotalRMB;
    @DataBaseField(type = "int(11)", fieldname = "clubCardNum", comment = "代理的圈卡数量")
    private int clubCardNum;
    @DataBaseField(type = "int(2)", fieldname = "vip", comment = "VIP级别")
    private int vip;
    @DataBaseField(type = "int(2)", fieldname = "power", comment = "权限1:有创建赛事权限")
    private int power;

    public void saveFamilyID(long familyID) {
        if (familyID == this.familyID) {
            return;
        }
        this.familyID = familyID;
        this.getBaseService().update("familyID", familyID, id, new AsyncInfo(id));
    }

    public void saveName(String name) {
        if (name.equals(this.name)) {
            return;
        }
        this.name = name;
        this.getBaseService().update("name", name, id, new AsyncInfo(id));
    }

    public void saveTotalRMB(long totalRMB) {
        if (totalRMB == this.totalRMB) {
            return;
        }
        this.totalRMB = totalRMB;
        this.getBaseService().update("totalRMB", totalRMB, id, new AsyncInfo(id));
    }

    public void saveTotalRMB_Sync(long totalRMB) {
        if (totalRMB == this.totalRMB) {
            return;
        }
        this.totalRMB = totalRMB;
        this.getBaseService().update("totalRMB", totalRMB, id);
    }

    public void saveClubTotalRMB(long clubTotalRMB) {
        if (clubTotalRMB == this.clubTotalRMB) {
            return;
        }
        this.clubTotalRMB = clubTotalRMB;
        this.getBaseService().update("clubTotalRMB", clubTotalRMB, id, new AsyncInfo(id));
    }

    public void saveClubTotalRMB_Sync(long clubTotalRMB) {
        if (clubTotalRMB == this.clubTotalRMB) {
            return;
        }
        this.clubTotalRMB = clubTotalRMB;
        this.getBaseService().update("clubTotalRMB", clubTotalRMB, id);
    }

    public void saveStatus(int status) {
        if (this.status == status) {
            return;
        }
        this.status = status;
        this.getBaseService().update("status", status, id, new AsyncInfo(id));
    }

    public void saveFencheng(int fencheng) {
        if (this.fencheng == fencheng) {
            return;
        }
        this.fencheng = fencheng;
        this.getBaseService().update("fencheng", fencheng, id, new AsyncInfo(id));
    }

    public void saveBeizhu(String beizhu) {
        if (this.beizhu.equals(beizhu)) {
            return;
        }
        this.beizhu = beizhu;
        this.getBaseService().update("beizhu", beizhu, id, new AsyncInfo(id));
    }

    public void saveRecommend(long recommend) {
        if (this.recommend == recommend) {
            return;
        }
        this.recommend = recommend;
        this.getBaseService().update("recommend", recommend, id, new AsyncInfo(id));
    }

    public void saveRoomcardNum(int roomcardNum) {
        if (this.roomcardNum == roomcardNum) {
            return;
        }
        this.roomcardNum = roomcardNum;
        this.getBaseService().update("roomcardNum", roomcardNum, id, new AsyncInfo(id));
    }

    public void saveRoomcardNum_Sync(int roomcardNum) {
        if (this.roomcardNum == roomcardNum) {
            return;
        }
        this.roomcardNum = roomcardNum;
        this.getBaseService().update("roomcardNum", roomcardNum, id);
    }

    public void saveClubCardNum_Sync(int clubCardNum) {
        if (this.clubCardNum == clubCardNum) {
            return;
        }
        this.clubCardNum = clubCardNum;
        this.getBaseService().update("clubCardNum", clubCardNum, id);
    }


    public static String getSql_TableCreate() {
        String sql = "CREATE TABLE IF NOT EXISTS `family` ("
                + "`id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '自增主键',"
                + "`familyID` bigint(20) NOT NULL DEFAULT '0' COMMENT '代理ID',"
                + "`name` varchar(120) NOT NULL DEFAULT '' COMMENT '工会名',"
                + "`createTime` bigint(20) NOT NULL DEFAULT '0' COMMENT '创建时间毫秒',"
                + "`status` int(2) NOT NULL DEFAULT '1' COMMENT '1为正在使用,2为已撤销',"
                + "`totalRMB` bigint(20) NOT NULL DEFAULT '0' COMMENT '累计充值',"
                + "`ownerID` bigint(20) NOT NULL DEFAULT '0' COMMENT '代理玩家ID',"
                + "`fencheng` int(11) NOT NULL DEFAULT '0' COMMENT '分成(固定40)',"
                + "`beizhu` varchar(255) NOT NULL DEFAULT '' COMMENT '备注',"
                + "`haveYouxi` varchar(255) NOT NULL DEFAULT '' COMMENT '游戏表',"
                + "`recommend` bigint(20) NOT NULL DEFAULT '0' COMMENT '推荐代理ID',"
                + "`minTixian` int(11) NOT NULL DEFAULT '0' COMMENT '最小提现',"
                + "`roomcardNum` int(11) NOT NULL DEFAULT '0' COMMENT '房卡数量',"
                + "`higherLevel` int(11) NOT NULL DEFAULT '0' COMMENT '给上级代理分成比',"
                + "`lowerLevel` int(11) NOT NULL DEFAULT '0' COMMENT '给下级代理分成比',"
                + "`clubLevel` int(11) NOT NULL DEFAULT '0' COMMENT '给亲友圈代理分成比',"
                + "`clubTotalRMB` bigint(20) NOT NULL DEFAULT '0' COMMENT '亲友圈累计充值',"
                + "`clubCardNum` int(11) NOT NULL DEFAULT '0' COMMENT '代理的圈卡数量',"
                + "`vip` int(2) NOT NULL DEFAULT '0' COMMENT 'VIP级别',"
                + "`power` int(2) NOT NULL DEFAULT '0' COMMENT '权限1:有创建赛事权限',"
                + "PRIMARY KEY (`id`),"
                + "KEY `name` (`name`),"
                + "UNIQUE KEY `familyID` (`familyID`)"
                + ") COMMENT='工会'  DEFAULT CHARSET=utf8 AUTO_INCREMENT=1";
        return sql;
    }

}
