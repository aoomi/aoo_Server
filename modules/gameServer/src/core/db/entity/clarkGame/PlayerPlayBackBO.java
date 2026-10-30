package core.db.entity.clarkGame;

import com.ddm.server.annotation.DataBaseField;
import com.ddm.server.annotation.TableName;
import com.ddm.server.common.CommLogD;
import core.db.entity.BaseEntity;
import core.db.other.AsyncInfo;
import core.ioc.Constant;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/**
 * 玩家游戏记录回放
 *
 * @author Huaxing
 */
@Data
@NoArgsConstructor
@TableName("PlayerPlayBack")
public class PlayerPlayBackBO extends BaseEntity<PlayerPlayBackBO> {

    @DataBaseField(type = "bigint(20)", fieldname = "id", comment = "自增主key", indextype = DataBaseField.IndexType.Unique)
    private long id;
    @DataBaseField(type = "bigint(20)", fieldname = "roomID", comment = "房间ID")
    private long roomID;
    @DataBaseField(type = "int(11)", fieldname = "setID", comment = "局数ID")
    private int setID;
    @DataBaseField(type = "int(11)", fieldname = "endTime", comment = "结束时间")
    private int endTime;
    @DataBaseField(type = "mediumtext", fieldname = "playBackRes", comment = "回放数据JSON")
    private Object playBackRes = "";
    @DataBaseField(type = "int(4)", fieldname = "dPos", comment = "庄家位置")
    private int dPos = -1;
    @DataBaseField(type = "text", fieldname = "playerList", comment = "用户列表JSON")
    private String playerList = "";
    @DataBaseField(type = "int(11)", fieldname = "playBackCode", comment = "回放编码")
    private int playBackCode = 0;

    @DataBaseField(type = "int(11)", fieldname = "setCount", comment = "房间总局数")
    private int setCount = 0;
    @DataBaseField(type = "varchar(25)", fieldname = "roomKey", comment = "房间号")
    private String roomKey = "";

    @DataBaseField(type = "int(4)", fieldname = "gameType", comment = "游戏类型")
    private int gameType = -1;

    @DataBaseField(type = "int(11)", fieldname = "tabId", comment = "标识Id")
    private int tabId = 0;


    /**
     * 实现对象间的深度克隆【从外形到内在细胞，完完全全深度copy】
     *
     * @return
     */
    public PlayerPlayBackBO deepClone() {
        // Anything 都是可以用字节流进行表示，记住是任何！
        PlayerPlayBackBO cookBook = null;
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
            cookBook = (PlayerPlayBackBO) ois.readObject();

        } catch (Exception e) {
            CommLogD.error(e.getClass() + ":" + e.getMessage());
        }
        return cookBook;
    }

    public void saveRoomID(long roomID) {
        if (roomID == this.roomID) {
            return;
        }
        this.roomID = roomID;
        getBaseService().update("roomID", roomID, id, new AsyncInfo(id));
    }

    public void saveSetID(int setID) {
        if (setID == this.setID) {
            return;
        }
        this.setID = setID;
        getBaseService().update("setID", setID, id, new AsyncInfo(id));
    }

    public void saveEndTime(int endTime) {
        if (endTime == this.endTime) {
            return;
        }
        this.endTime = endTime;
        getBaseService().update("endTime", endTime, id, new AsyncInfo(id));
    }

    public void savePlayBackRes(StringBuilder playBackRes) {
        if (this.playBackRes == playBackRes) {
            return;
        }
        this.playBackRes = playBackRes;
        getBaseService().update("playBackRes", playBackRes, id, new AsyncInfo(id));
    }

    public void savedPos(int dPos) {
        if (this.dPos == dPos) {
            return;
        }
        this.dPos = dPos;
        getBaseService().update("dPos", dPos, id, new AsyncInfo(id));
    }

    public void savePlayerList(String playerList) {
        if (StringUtils.isEmpty(playerList)) {
            return;
        }
        if (playerList.equals(this.playerList)) {
            return;
        }
        this.playerList = playerList;
        getBaseService().update("playerList", playerList, id, new AsyncInfo(id));
    }

    public void savePlayBackCode(int playBackCode) {
        if (this.playBackCode == playBackCode) {
            return;
        }
        this.playBackCode = playBackCode;
        getBaseService().update("playBackCode", playBackCode, id, new AsyncInfo(id));
    }

    public void saveSetCount(int setCount) {
        if (this.setCount == setCount) {
            return;
        }
        this.setCount = setCount;
        getBaseService().update("setCount", setCount, id, new AsyncInfo(id));
    }

    public void saveRoomKey(String roomKey) {
        if (StringUtils.isEmpty(roomKey)) {
            return;
        }
        if (roomKey.equals(this.roomKey)) {
            return;
        }
        this.roomKey = roomKey;
        getBaseService().update("roomKey", roomKey, id, new AsyncInfo(id));
    }

    public void saveGameType(int gameType) {
        if (this.gameType == gameType) {
            return;
        }
        this.gameType = gameType;
        getBaseService().update("gameType", gameType, id, new AsyncInfo(id));
    }

    public static String getSql_TableCreate() {
        String sql = "CREATE TABLE IF NOT EXISTS `PlayerPlayBack` ("
                + "`id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '自增主键',"
                + "`roomID` bigint(20) NOT NULL DEFAULT '0' COMMENT '房间ID',"
                + "`setID` int(11) NOT NULL DEFAULT '0' COMMENT '局数ID',"
                + "`endTime` int(11) NOT NULL DEFAULT '0' COMMENT '结束时间',"
                + "`playBackRes`  mediumtext CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '回放数据JSON' ,"
                + "`dPos` int(4) NOT NULL DEFAULT '-1' COMMENT '庄家位置',"
                + "`playerList`  text CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '玩家列表JSON' ,"
                + "`playBackCode` int(11) NOT NULL DEFAULT '0' COMMENT '回放编码',"
                + "`tabId` int(11) NOT NULL DEFAULT '0' COMMENT '标识Id',"

                + "`setCount` int(11) NOT NULL DEFAULT '0' COMMENT '房间总局数',"
                + "`roomKey` varchar(25) NOT NULL DEFAULT '' COMMENT '房间号',"

                + "`gameType` int(4) NOT NULL DEFAULT '-1' COMMENT '游戏类型',"


                + "PRIMARY KEY (`id`),"
                + "INDEX `playBackCode` (`playBackCode`) USING BTREE,"
                + "KEY `roomID` (`roomID`),"
                + "KEY `endTime` (`endTime`)"
                + ") COMMENT='玩家游戏记录回放'  DEFAULT CHARSET=utf8 AUTO_INCREMENT=" + (Constant.InitialID + 1);

        return sql;
    }


}
