package business.player.feature;

import business.player.Player;
import cenum.ConstEnum.ResOpType;
import cenum.ItemFlow;
import cenum.PrizeType;
import com.ddm.server.common.CommLogD;
import com.ddm.server.websocket.def.ErrorCode;
import core.config.refdata.RefDataMgr;
import core.config.refdata.ref.RefDiamondStore;
import core.logger.flow.FlowLogger;

/**
 * 不是你的模块，请咨询作者，弄清楚逻辑再动
 *
 * @date 2016年1月21日
 */
public class PlayerCurrency extends Feature {


    public PlayerCurrency(Player data) {
        super(data);
    }

    @Override
    public void loadDB() {
    }

    /**
     * 检测充值货币是否足够
     *
     * @param type  消耗类型
     * @param count 消耗值
     * @return
     */
    public boolean check(PrizeType type, int count) {
        switch (type) {
            case RoomCard:
                return player.getFeature(PlayerWallet.class).check(count);
            case Gold:
                return player.getPlayerBO().getGold() >= count;
            default:
                CommLogD.warn("类型{}未指定货币或商品，无法check", type);
                return true;
        }
    }


    /**
     * 检查并消耗房卡
     *
     * @param type   消耗类型
     * @param count  消耗值
     * @param reason 选择项
     * @return
     */
    public boolean checkAndConsumeItemFlow(PrizeType type, int count, ItemFlow reason) {
        // 检测充值货币是否足够
        if (this.check(type, count)) {
            consume(type, count, -1, reason, ResOpType.Lose);
            return true;
        }
        return false;
    }

    /**
     * 获取奖励
     *
     * @param type   消耗类型
     * @param count  消耗值
     * @param reason 选择项
     * @return
     */
    public void gainItemFlow(PrizeType type, int count, ItemFlow reason) {
        this.gain(type, count, -1, reason, ResOpType.Gain);
    }


    /**
     * 获取奖励
     *
     * @param type   消耗类型
     * @param count  消耗值
     * @param reason 选择项
     * @return
     */
    public void gainItemFlow(PrizeType type, int count, ItemFlow reason, ResOpType resOpType) {
        this.gain(type, count, -1, reason, resOpType);
    }


    /**
     * 检查玩家金币是否符合条件
     *
     * @param min 最小值
     * @param max 最大值
     * @return
     */
    public ErrorCode checkGold(int min, int max) {
        int gold = player.getPlayerBO().getGold();
        if (gold >= min && max == 0) {
            // 玩家金币不足
            return ErrorCode.Success;
        } else if (gold < min) {
            // 玩家金币不足
            return ErrorCode.NotEnough_Gold;
        } else if (gold > max) {
            // 玩家金币过高
            return ErrorCode.NotEnough_GoldHigh;
        }
        return ErrorCode.Success;
    }

    /**
     * 练习场-结算
     *
     * @param count    结算分数
     * @param baseMark 底分场费 类型
     * @param gameId   游戏类型
     */
    public void goldRoomEnd(int count, int baseMark, int gameId) {
        if (count < 0) {
            // 消耗货币
            this.consume(PrizeType.Gold, Math.abs(count), gameId, ItemFlow.GoldRoom, ResOpType.Lose);
        } else if (count > 0) {
            // 获得货币
            this.gain(PrizeType.Gold, count, gameId, ItemFlow.GoldRoom, ResOpType.Gain);
        }
        // 练习场-场地费扣除
        this.consume(PrizeType.Gold, Math.abs(baseMark), gameId, ItemFlow.SiteFee, ResOpType.Lose);
    }

    /**
     * 获得货币
     *
     * @param type      消耗类型
     * @param value     值
     * @param gameId    游戏ID
     * @param reason    项
     * @param resOpType 资源(货币、物品等)操作类型
     * @return
     */
    public int gain(PrizeType type, int value, int gameId, ItemFlow reason, ResOpType resOpType) {
        if (value <= 0) {
            return 0;
        }
        switch (type) {
            case Gold:
                return gainGold(value, gameId, reason, resOpType);
            case RoomCard:
                return getPlayer().getFeature(PlayerWallet.class).gainRoomCard(value, gameId, reason, resOpType);
            default:
                CommLogD.warn("类型{}未指定货币或商品，无法 获得。", type, new Throwable());
                return 0;
        }
    }


    /**
     * 消耗货币
     *
     * @param type      消耗类型
     * @param value     值
     * @param gameId    游戏ID
     * @param reason    项
     * @param resOpType 资源(货币、物品等)操作类型
     */
    public void consume(PrizeType type, int value, int gameId, ItemFlow reason, ResOpType resOpType) {
        if (value <= 0) {
            return;
        }
        switch (type) {
            case Gold:
                this.consumeGold(value, gameId, reason, resOpType);
                return;
            case RoomCard:
                this.getPlayer().getFeature(PlayerWallet.class).consumeRoomCard(value, gameId, reason, resOpType);
                return;
            default:
                CommLogD.warn("类型{}未指定货币或商品，无法consume。", type, new Throwable());
                return;
        }
    }

    /**
     * 乐豆(练习场)
     *
     * @param value     值
     * @param gameId    游戏ID
     * @param reason    项
     * @param resOpType 资源(货币、物品等)操作类型
     */
    private void consumeGold(int value, int gameId, ItemFlow reason, ResOpType resOpType) {
        if (value <= 0) {
            return;
        }
        this.lock();
        int before = player.getPlayerBO().getGold();
        int finalMoney = Math.max(0, before - value);
        // 消耗金币
        player.getPlayerBO().saveGold(finalMoney);
        this.unlock();
        player.pushProperties("gold", player.getPlayerBO().getGold());
        FlowLogger.goldChargeLog(player.getPid(), reason.value(), value, finalMoney, before, resOpType.ordinal(), 0);
    }

    /**
     * 乐豆
     *
     * @param value     值
     * @param gameId    游戏ID
     * @param reason    项
     * @param resOpType 资源(货币、物品等)操作类型
     * @return
     */
    private int gainGold(int value, int gameId, ItemFlow reason, ResOpType resOpType) {
        this.lock();
        int before = player.getPlayerBO().getGold();
        int finalMoney = Math.min(1999999999, before + value);
        // 获得金币
        player.getPlayerBO().saveGold(finalMoney);
        this.unlock();
        player.pushProperties("gold", player.getPlayerBO().getGold());
        FlowLogger.goldChargeLog(player.getPid(), reason.value(), value, finalMoney, before, resOpType.ordinal(), 0);
        return value;
    }


    /**
     * 房卡兑换乐豆
     */
    public int roomCardLeDou(int productID) {
        RefDiamondStore data = RefDataMgr.get(RefDiamondStore.class, productID);
        if (null == data) {
            // ID错误
            return 0;
        }
        // 判断商品类型是否正确。
        if (data.goodsType != 1) {
            return 0;
        }
        // 先检查用户的房卡是否足够兑换乐豆,满足消耗房卡。
        if (!getPlayer().getFeature(PlayerWallet.class).checkAndConsumeItemFlow(data.AppPrice, ItemFlow.CardRoomExchangeGold)) {
            return 0;
        }
        // 兑换相应的乐豆。
        gainItemFlow(PrizeType.Gold, data.DiamondNum, ItemFlow.CardRoomExchangeGold);
        return data.DiamondNum;
    }


    /**
     * 清空公共房卡值
     */
    public int clearRoomCard() {
        if (player.getPlayerBO().getRoomCard() <= 0) {
            return 0;
        }
        this.lock();
        int before = player.getPlayerBO().getRoomCard();
        int finalValue = 0;
        // 消耗直充房卡
        player.getPlayerBO().saveRoomCard(finalValue);
        this.unlock();
        return before;
    }

}
