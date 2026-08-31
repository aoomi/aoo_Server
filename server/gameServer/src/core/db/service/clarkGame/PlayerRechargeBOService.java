package core.db.service.clarkGame;

import cenum.redis.RedisBydrKeyEnum;
import com.ddm.server.annotation.Service;
import com.ddm.server.common.redis.RedisMgr;
import core.db.dao.clarkGame.BaseClarkGameDao;
import core.db.entity.clarkGame.PlayerRechargeBO;
import core.db.other.Restrictions;
import core.db.persistence.CustomerDao;
import core.db.service.BaseService;
import core.ioc.ContainerMgr;
import jsproto.c2s.cclass.QueryIdItem;

import java.util.Objects;

@Service(source = "clark_game")
public class PlayerRechargeBOService implements BaseService<PlayerRechargeBO> {
    private BaseClarkGameDao<PlayerRechargeBO> playerRechargeBODao = new BaseClarkGameDao<>(PlayerRechargeBO.class);

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Override
    public CustomerDao getDefaultDao() {
        return playerRechargeBODao;
    }

    /**
     * 是否存在
     *
     * @param orderID 订单号
     * @return T:存在，F:不存在
     */
    public boolean existsOrderId(String orderID) {
        if (RedisMgr.get().isOpenRedis()) {
            return ContainerMgr.get().getRedis().exists(RedisBydrKeyEnum.RECHARGE_REPEAT_ORDER_ID.getKey(orderID));
        } else {
            QueryIdItem idItem = findOneE(Restrictions.eq("orderId", orderID), QueryIdItem.class, QueryIdItem.getItemsName());
            return Objects.nonNull(idItem) && idItem.getId() > 0L;
        }
    }

    @Override
    public long saveIgnore(PlayerRechargeBO element) {
        long id = getDefaultDao().add(element, true);
        if (id > 0L && RedisMgr.get().isOpenRedis()) {
            ContainerMgr.get().getRedis().putWithTime(RedisBydrKeyEnum.RECHARGE_REPEAT_ORDER_ID.getKey(element.getOrderId()), 20, element.getOrderId());
        }
        return id;
    }
}
