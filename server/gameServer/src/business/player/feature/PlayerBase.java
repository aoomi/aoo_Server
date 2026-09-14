package business.player.feature;

import business.player.Player;
import com.ddm.server.common.utils.CommTime;
import core.db.entity.clarkGame.PlayerBO;
import jsproto.c2s.iclass.S1005_PlayerInfo;

import java.util.List;

/**
 * 不是你的模块，请咨询作者，弄清楚逻辑再动
 *
 * @date 2016年1月12日
 */
public class PlayerBase extends Feature {

    public PlayerBase(Player data) {
        super(data);
    }

    // 已经online - 登陆事件
    public void onConnect() {

    }

    // =========================== 接口 ========================
    public void sendMsg(String key) {
    }

    public void sendMsg(String key, List<String> msgList) {
    }

    public void sendSysMessage(String msg) {
    }

    public void sendPopupMessage(String msg) {
    }

    // 改这里了，记得改下面的worldProto
    public S1005_PlayerInfo fullInfo(boolean needSign) {
        S1005_PlayerInfo info = new S1005_PlayerInfo();
        PlayerBO bo = player.getPlayerBO();
        info.pid = bo.getId();

        info.accountID = bo.getAccountID();
        info.name = bo.getName();
        info.icon = bo.getIcon();
        info.sex = bo.getSex();
        info.headImageUrl = bo.getHeadImageUrl();
        info.createTime = bo.getCreateTime();

        info.lv = bo.getLv();
        info.gmLevel = bo.getGmLevel();
        info.vipLv = bo.getVipLevel();
        info.vipExp = bo.getVipExp();
        info.totalRecharge = bo.getTotalRecharge();
        info.roomCard = getPlayer().getRoomCard();
        info.fastCard = bo.getFastCard();
        info.crystal = bo.getCrystal();
        info.gold = bo.getGold();
        info.realName = bo.getRealName();
        info.realNumber = bo.getRealNumber();
        info.currentGameType = bo.getCurrentGameType();
        info.time = CommTime.nowMS();
        info.startServerTime = CommTime.nowMS();
        if (needSign) {
            info.sign = getPlayer().getUUID();
        }
        return info;
    }


    @Override
    public void loadDB() {
    }
}
