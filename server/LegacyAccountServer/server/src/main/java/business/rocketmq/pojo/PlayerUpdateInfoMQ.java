package business.rocketmq.pojo;

import lombok.Data;
import lombok.EqualsAndHashCode;
import server.aoo.dao.entity.game.DbPlayer;

@Data
@EqualsAndHashCode(callSuper = true)
public class PlayerUpdateInfoMQ extends BaseConsumerMQ {

    /**
     * 玩家信息
     */
    private DbPlayer dbPlayer;
    public static PlayerUpdateInfoMQ make(DbPlayer dbPlayer) {
        PlayerUpdateInfoMQ ret = new PlayerUpdateInfoMQ();
        ret.setExistTest(false);
        ret.setDbPlayer(dbPlayer);
        return ret;
    }
}
