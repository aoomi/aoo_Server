package business.player.feature;

import business.player.Player;
import cenum.ItemFlow;
import cenum.PrizeType;
import cenum.SignInType;
import com.ddm.server.common.utils.CommTime;
import com.ddm.server.websocket.def.ErrorCode;
import com.ddm.server.websocket.handler.requset.WebSocketRequest;
import core.config.refdata.RefDataMgr;
import core.config.refdata.ref.RefSignIn;
import core.db.entity.clarkGame.PlayerSignBO;
import core.db.other.Restrictions;
import core.db.service.clarkGame.PlayerSignBOService;
import core.ioc.ContainerMgr;
import jsproto.c2s.iclass.SPlayer_Sign;

import java.util.List;

/**
 * 玩家签到
 *
 * @author Huaxing
 */
public class PlayerSign extends Feature {
    public static final int SIGN_NUM = 1000;
    public static final String Query = "QUERY";
    public static final String Reward = "REWARD";
    public static final int Query_Int = 1;
    public static final int Reward_Int = 2;

    public PlayerSign(Player player) {
        super(player);
    }

    @Override
    public void loadDB() {
    }

    /**
     * 查询玩家签到记录
     */
    public void querySignRecord(WebSocketRequest request) {
        //更新玩家Pid 查询玩家的签到记录
        PlayerSignBO signBO = ContainerMgr.get().getComponent(PlayerSignBOService.class).findOne(Restrictions.eq("pid", player.getPid()), null);
        if (signBO == null) {
            request.response(SPlayer_Sign.make(Query, 1, false));
            return;
        }
        int dayCount = signBO.getSignCount() + 1;
        if (dayCount >= RefSignIn.DailySignInMaxDay) {
            dayCount = 1;
        }
        request.response(SPlayer_Sign.make(Query, dayCount, CommTime.daysBetween(signBO.getSignTime(), CommTime.nowSecond()) == 0));
    }


    /**
     * 获取签到奖励
     */
    public void getSignReward(WebSocketRequest request) {
        //签到
        PlayerSignBO signBO = ((PlayerSignBOService) ContainerMgr.get().getComponent(PlayerSignBOService.class)).saveIgnoreOrUpDate(player.getPid(), RefSignIn.DailySignInMaxDay);
        //1、通过用户ID 查找签到记录
        if (null == signBO) {
            request.error(ErrorCode.NotAllow, "has Sign");
            return;
        }
        if (signBO.getRewardState() != 1) {
            request.response();
            return;
        }
        //检查是否有领取奖励
        int signCount = signBO.getSignCount();
        int signNum = SIGN_NUM + signBO.getSignCount();
        RefSignIn signIn = RefDataMgr.get(RefSignIn.class, signNum);
        if (null == signIn) {
            request.response();
            return;
        }
        if (signIn.Day != signCount || !SignInType.SignIn.equals(signIn.Type)) {
            request.response();
            return;
        }
        List<Integer> uniformitemIdList = signIn.getUniformitemIdList();
        List<Integer> countList = signIn.getCountList();

        int signIdSize = uniformitemIdList.size();
        int countSize = countList.size();
        if (signIdSize != countSize) {
            request.response();
            return;
        }
        for (int signId = 0; signId < signIdSize; signId++) {
            player.getFeature(PlayerCurrency.class).gainItemFlow(PrizeType.valueOf(uniformitemIdList.get(signId)), countList.get(signId), ItemFlow.SignIn);
        }
        request.response(SPlayer_Sign.make(Reward, signCount, false));
        signBO.saveReward();
    }
}
