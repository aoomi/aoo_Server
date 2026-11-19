package core.db.entity.dbZle;

import com.ddm.server.annotation.DataBaseField;
import com.ddm.server.annotation.TableName;
import core.db.entity.BaseEntity;
import core.db.other.AsyncInfo;
import lombok.Data;
import lombok.NoArgsConstructor;

@TableName(value = "noticeinfo")
@Data
@NoArgsConstructor
public class NoticeBO extends BaseEntity<NoticeBO> {
    @DataBaseField(type = "bigint(20)", fieldname = "id", comment = "", indextype = DataBaseField.IndexType.Unique)
    private long id;//公告ID
    private int clientType;//公告类型
    private int interval;//公告次数
    private long beginTime;//开始时间毫秒
    private long endTime;//结束时间毫秒
    private long lastSendTime;//最后活动时间毫秒
    private String mainTitle = "";//公告主标题
    private String title = "";//公告标题
    private String content = "";//公告内容

    public void saveClientType(int clientType) {
        if (clientType == this.clientType) {
            return;
        }
        this.clientType = clientType;
        getBaseService().update("clientType", clientType, id, new AsyncInfo(id));
    }

    public void saveInterval(int interval) {
        if (this.interval == interval) {
            return;
        }
        this.interval = interval;
        getBaseService().update("interval", interval, id, new AsyncInfo(id));
    }

    public void saveBeginTime(long beginTime) {
        if (beginTime == this.beginTime) {
            return;
        }
        this.beginTime = beginTime;
        getBaseService().update("beginTime", beginTime, id, new AsyncInfo(id));
    }

    public void saveEndTime(long endTime) {
        if (endTime == this.endTime) {
            return;
        }
        this.endTime = endTime;
        getBaseService().update("endTime", endTime, id, new AsyncInfo(id));
    }

    public void saveLastSendTime(long lastSendTime) {
        if (lastSendTime == this.lastSendTime) {
            return;
        }
        this.lastSendTime = lastSendTime;
        getBaseService().update("lastSendTime", lastSendTime, id, new AsyncInfo(id));
    }

    public void saveMainTitle(String mainTitle) {
        if (mainTitle.equals(this.mainTitle)) {
            return;
        }
        this.mainTitle = mainTitle;
        getBaseService().update("mainTitle", mainTitle, id, new AsyncInfo(id));
    }

    public void saveTitle(String title) {
        if (title.equals(this.title)) {
            return;
        }
        this.title = title;
        getBaseService().update("title", title, id, new AsyncInfo(id));
    }

    public void saveContent(String content) {
        if (content.equals(this.content)) {
            return;
        }
        this.content = content;
        getBaseService().update("content", content, id, new AsyncInfo(id));
    }

    public static String getCreateTableSQL() {
        String sql = "CREATE TABLE `noticeinfo` (\n" +
                "  `id` int(11) NOT NULL AUTO_INCREMENT,\n" +
                "  `title` varchar(255) DEFAULT NULL,\n" +
                "  `content` varchar(255) DEFAULT NULL,\n" +
                "  `mainTitle` varchar(255) DEFAULT NULL,\n" +
                "  `clientType` int(11) DEFAULT NULL,\n" +
                "  `beginTime` timestamp NULL DEFAULT '0000-00-00 00:00:00',\n" +
                "  `endTime` timestamp NULL DEFAULT '0000-00-00 00:00:00',\n" +
                "  `interval` int(11) DEFAULT NULL,\n" +
                "  `lastSendTime` timestamp NULL DEFAULT NULL,\n" +
                "  PRIMARY KEY (`id`)\n" +
                ") ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8";
        return sql;
    }

}
