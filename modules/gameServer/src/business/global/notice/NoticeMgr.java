package business.global.notice;

import com.ddm.server.common.CommLogD;
import com.ddm.server.common.utils.CommTime;
import com.ddm.server.common.utils.Lists;
import com.ddm.server.http.client.HttpUtil;
import com.ddm.server.http.server.HttpUtils;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import core.db.entity.dbZle.NoticeBO;
import core.db.service.dbZle.NoticeBOService;
import core.ioc.ContainerMgr;
import jsproto.c2s.cclass.NoticeInfo;
import jsproto.c2s.iclass.S2226_InitNotice;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * 公告管理
 *
 * @author liyan
 */
public class NoticeMgr {

    private static NoticeMgr instance = new NoticeMgr();

    private NoticeBOService noticeBOService;

    public static NoticeMgr getInstance() {
        return instance;
    }

    public NoticeMgr() {
        noticeBOService = ContainerMgr.get().getComponent(NoticeBOService.class);
    }

    // 公告列表
    public LinkedList<Notice> NoticeList = Lists.newLinkedList();

    public void init() {
        // 下载公告
        try {
            List<NoticeBO> allPlayers = noticeBOService.findAll(null, "");
            for (NoticeBO bo : allPlayers) {
                Notice notice = new Notice();
                notice.setId(bo.getId());
                notice.setMainTitle(bo.getMainTitle());
                notice.setTitle(bo.getTitle());
                notice.setContent(bo.getContent());
                notice.setBeginTime(bo.getBeginTime());
                notice.setEndTime(bo.getEndTime());
                notice.setClientType(bo.getClientType());

                // 添加缓存
                NoticeList.add(notice);

            }

        } catch (Exception e) {
            CommLogD.error("downMarqueeFromPHP发生错误:" + e.toString());
        }
    }

    /**
     * 下载跑马灯公告列表
     *
     * @throws Exception
     */
    public void downMarqueeFromPHP() throws Exception {
        // 请求参数

        String url = System.getProperty("JavaServerUrl");
        String gameSid = System.getProperty("game_sid");

        url = url + "?op=GetGmNotice&server_id=" + gameSid;
        // 下载公告
        String webBody = HttpUtil.sendHttpPost2Web(3000, 3000, url, "", "");
        // 编码处理
        webBody = HttpUtil.decodeUnicode(webBody);
        // 公告列表
        JsonArray array = new JsonParser().parse(webBody).getAsJsonArray();
        // 循环处理
        LinkedList<Notice> noticeList = Lists.newLinkedList();
        int noticeID = 1;
        long nowMS = CommTime.nowMS();

        for (JsonElement element : array) {
            JsonObject obj = element.getAsJsonObject();

            long endTime = HttpUtils.getLong(obj, "endtime");
            // 时间已经结束过滤
            if (endTime < nowMS) {
                continue;
            }
            // 公告信息
            Notice notice = new Notice();
            notice.setId(noticeID);
            notice.setMainTitle(HttpUtils.getString(obj, "mainTitle"));
            notice.setTitle(HttpUtils.getString(obj, "title"));
            notice.setContent(HttpUtils.getString(obj, "content"));
            notice.setBeginTime(HttpUtils.getLong(obj, "starttime"));
            notice.setEndTime(endTime);
            notice.setClientType(HttpUtils.getInt(obj, "contenttype"));
            // 添加缓存
            noticeList.add(notice);

            noticeID += 1;
        }
        synchronized (this) {
            this.NoticeList.clear();
            this.NoticeList = noticeList;
        }
    }

    /**
     * GM后台添加跑马灯公告信息
     *
     * @param id
     * @param content
     * @param beginTime
     * @param endTime
     * @param interval
     * @param clientType
     * @return
     */
    public String gmAddMarqueeNotice(int id, String content, int beginTime, int endTime, int interval, int clientType) {
        Notice bo = new Notice();
        bo.setId(id);
        bo.setContent(content);
        bo.setBeginTime(beginTime);
        bo.setEndTime(endTime);
        bo.setInterval(interval);
        bo.setClientType(clientType);
        synchronized (this) {
            this.NoticeList.add(bo);
        }
        return "添加成功";
    }

    public S2226_InitNotice getNoticeInfoList() {

        List<NoticeInfo> noticeInfoList = new ArrayList<>();

        for (Notice notice : this.NoticeList) {
            noticeInfoList.add(notice.getNoticeInfo());
        }

        return S2226_InitNotice.make(noticeInfoList);
    }
}
