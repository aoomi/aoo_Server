package business.player.feature;

import business.global.room.PlayBackMgr;
import business.player.Player;
import business.py.c2s.cclass.PYPlayBackPlayerInFo;
import com.ddm.server.common.utils.CommFile;
import com.ddm.server.websocket.def.ErrorCode;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import core.config.server.GameTypeMgr;
import core.db.entity.clarkGame.PlayerPlayBackBO;
import core.network.http.proto.SData_Result;
import jsproto.c2s.iclass.SPlayer_PlayBack;
import org.apache.commons.lang3.StringUtils;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Objects;

/**
 * 刨幺扑克，大厅回放
 * 2人场回放显示4个玩家的回放
 */
public class PYPlayerRecord extends PlayerRecord {

    @Override
    public void loadDB() {
    }

    public PYPlayerRecord(Player player) {
        super(player);
    }

    /**
     * 最大的数据包长度
     */
    private final static int MAX_DATA_LENGTH = 10000;

    /**
     * 检查回放码是否存在，返回游戏类型
     *
     * @param playBackCode 回放码
     * @return 游戏类型
     */
    private SData_Result chekcPlayBackCodeResult(int playBackCode) {

        String hand = CommFile.getHandFromFile(PlayBackMgr.getInstance().getFile(playBackCode));
        if (StringUtils.isEmpty(hand) || !StringUtils.isNumeric(hand)) {
            return SData_Result.make(ErrorCode.PlayBack_Error, "error playBackCode");
        }
        return SData_Result.make(ErrorCode.Success, GameTypeMgr.getInstance().gameType(Integer.parseInt(hand)));
    }

    /**
     * 玩家回放记录
     */
    @Override
    public SData_Result playerPlayBack(int playBackCode, boolean chekcPlayBackCode) {
        if (playBackCode <= 0) {
            return SData_Result.make(ErrorCode.PlayBack_Error, "playBackCode:{%d}", playBackCode);
        }
        if (chekcPlayBackCode) {
            return this.chekcPlayBackCodeResult(playBackCode);
        }
        String content = CommFile.getContentFromFile(PlayBackMgr.getInstance().getFile(playBackCode));
        if (StringUtils.isEmpty(content)) {
            return SData_Result.make(ErrorCode.PlayBack_Error, "content Empty");
        }
        PlayerPlayBackBO playBackBO = new Gson().fromJson(content, PlayerPlayBackBO.class);

        StringBuilder playBack = new StringBuilder(playBackBO.getPlayBackRes().toString());
        if (Objects.nonNull(playBack)) {
            // 获取最大长度
            int maxLength = playBack.length();
            // 可拆得包数量
            int number = (maxLength / MAX_DATA_LENGTH);
            // 共下发多少包
            int playBackNum = maxLength % MAX_DATA_LENGTH == 0 ? number : number + 1;
            subPlayBack(0, 0, maxLength, playBack, playBackNum);
        }
        // 解析List类型的json字符串
        Type type = new TypeToken<List<PYPlayBackPlayerInFo>>() {
        }.getType();
        // 回放玩家信息列表
        List<PYPlayBackPlayerInFo> pYPlayBackPlayerInFoList = new Gson().fromJson(playBackBO.getPlayerList(), type);
        if (pYPlayBackPlayerInFoList.size() == 2) {
            // 2人场
            // pos2的回放玩家信息
            PYPlayBackPlayerInFo pyPlayBackPlayerInFo2 = new PYPlayBackPlayerInFo();
            // pos3的回放玩家信息
            PYPlayBackPlayerInFo pyPlayBackPlayerInFo3 = new PYPlayBackPlayerInFo();
            for (PYPlayBackPlayerInFo pyPlayBackPlayerInFo : pYPlayBackPlayerInFoList) {
                if (pyPlayBackPlayerInFo.getPos() == 0) {
                    pyPlayBackPlayerInFo2.setPid(pyPlayBackPlayerInFo.getPid());
                    pyPlayBackPlayerInFo2.setPos(2);
                    pyPlayBackPlayerInFo2.setName(pyPlayBackPlayerInFo.getName());
                    pyPlayBackPlayerInFo2.setIconUrl(pyPlayBackPlayerInFo.getIconUrl());
                    pyPlayBackPlayerInFo2.setSex(pyPlayBackPlayerInFo.getSex());
                    pyPlayBackPlayerInFo2.setPoint(pyPlayBackPlayerInFo.getPoint());
                } else if (pyPlayBackPlayerInFo.getPos() == 1) {
                    pyPlayBackPlayerInFo3.setPid(pyPlayBackPlayerInFo.getPid());
                    pyPlayBackPlayerInFo3.setPos(3);
                    pyPlayBackPlayerInFo3.setName(pyPlayBackPlayerInFo.getName());
                    pyPlayBackPlayerInFo3.setIconUrl(pyPlayBackPlayerInFo.getIconUrl());
                    pyPlayBackPlayerInFo3.setSex(pyPlayBackPlayerInFo.getSex());
                    pyPlayBackPlayerInFo3.setPoint(pyPlayBackPlayerInFo.getPoint());
                }
            }
            // 更新回放玩家信息列表
            pYPlayBackPlayerInFoList.add(pyPlayBackPlayerInFo2);
            pYPlayBackPlayerInFoList.add(pyPlayBackPlayerInFo3);
        }
        // 回放玩家信息列表json字符串
        String pYPlayBackPlayerInFoListString = new Gson().toJson(pYPlayBackPlayerInFoList);
        return SData_Result.make(ErrorCode.Success, SPlayer_PlayBack.make(playBackBO.getRoomID(), playBackBO.getSetID(), playBackBO.getEndTime(), playBackBO.getDPos(), pYPlayBackPlayerInFoListString, playBackBO.getSetCount(), playBackBO.getRoomKey(), playBackBO.getGameType(), playBackBO.getSetID()));
    }


}