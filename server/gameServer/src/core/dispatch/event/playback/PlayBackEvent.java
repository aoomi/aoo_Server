package core.dispatch.event.playback;

import BaseCommon.CommLog;
import cenum.DispatcherComponentEnum;
import com.ddm.server.common.utils.CommFile;
import com.ddm.server.dispatcher.executor.BaseExecutor;
import com.google.gson.Gson;
import core.db.entity.clarkGame.PlayerPlayBackBO;
import lombok.Data;

import java.io.IOException;

@Data
public class PlayBackEvent implements BaseExecutor {
    /**
     * 保存文件地址
     */
    private String path;
    /**
     * 回放信息
     */
    private PlayerPlayBackBO playBackInfo;

    public PlayBackEvent(String path, PlayerPlayBackBO playBackInfo) {
        this.path = path;
        this.playBackInfo = playBackInfo;
    }

    @Override
    public void invoke() {
        try {
            StringBuilder builder = new StringBuilder();
            builder.append(getPlayBackInfo().getGameType());
            builder.append("-");
            builder.append(new Gson().toJson(getPlayBackInfo()));
            CommFile.Write(getPath(), builder.toString());
//            SharePlayBackKeyMgr.getInstance().setDatePlayBackKey(Integer.parseInt(String.valueOf(getPlayBackInfo().getPlayBackCode()).substring(1)));
//            savePlayBackServer(getPlayBackInfo().getPlayBackCode());
        } catch (IOException e) {
            CommLog.error("PlayBackEvent invoke Path:{},Msg:{}", getPath(), e.getMessage());
        }
    }

//    /**
//     * 保存回放码和服务器关系映射
//     * @param playBackCode
//     */
//    private void savePlayBackServer(int playBackCode) {
//        DateTime nowTime = new DateTime();
//        String nowToString = nowTime.toString("yyyyMMdd");
//        int week = nowTime.getDayOfWeek();
//        PlayBackServerBO playBackServerBO = new PlayBackServerBO();
//        playBackServerBO.setPlayBackCode(Integer.parseInt(String.valueOf(playBackCode).substring(1)));
//        playBackServerBO.setWeekDay(week);
//        playBackServerBO.setDateDay(Integer.parseInt(nowToString));
//        playBackServerBO.setGameServerIP(Config.nodeIp());
//        playBackServerBO.setGameServerPort(Config.nodePort());
//        playBackServerBO.insert();
//    }

    @Override
    public int threadId() {
        return DispatcherComponentEnum.PLAY_BACK.id();
    }

    @Override
    public int bufferSize() {
        return DispatcherComponentEnum.PLAY_BACK.bufferSize();
    }
}
