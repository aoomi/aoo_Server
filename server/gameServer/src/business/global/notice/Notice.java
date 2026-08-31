package business.global.notice;

import com.ddm.server.common.utils.Maps;
import jsproto.c2s.cclass.NoticeInfo;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * 公告列表
 *
 * @author liyan
 */
@Data
@NoArgsConstructor
public class Notice {

    public long id;// 公告ID
    public String mainTitle;// 公告主标题
    public String title;// 公告副标题
    public String content;// 公告内容
    public int clientType;// 公告类型
    public long beginTime;// 公告开始时间
    public long endTime;// 公告结束时间
    public int interval;// 公告发布次数
    public long lastSendTime;// 公告当前显示时间

    // 所有公告<pid,Notice>
    public final Map<Long, Notice> allNoticeMap = Maps.newConcurrentMap();

    public NoticeInfo getNoticeInfo() {
        NoticeInfo info = new NoticeInfo();
        info.id = this.id;
        info.mainTitle = this.mainTitle;
        info.title = this.title;
        info.content = this.content;
        info.clientType = this.clientType;
        info.beginTime = this.beginTime;
        info.endTime = this.endTime;

        return info;
    }
}
