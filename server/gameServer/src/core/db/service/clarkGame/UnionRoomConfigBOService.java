package core.db.service.clarkGame;

import BaseCommon.CommLog;
import com.ddm.server.annotation.Service;
import com.google.gson.Gson;
import core.db.dao.clarkGame.BaseClarkGameDao;
import core.db.entity.clarkGame.UnionRoomConfigBO;
import core.db.other.Restrictions;
import core.db.persistence.CustomerDao;
import core.db.service.BaseService;
import jsproto.c2s.cclass.QueryIdItem;
import jsproto.c2s.cclass.union.UnionCreateGameSet;

import java.util.Objects;

import static core.db.persistence.BaseDao.stackTrace;

@Service(source = "clark_game")
public class UnionRoomConfigBOService implements BaseService<UnionRoomConfigBO> {
    private BaseClarkGameDao<UnionRoomConfigBO> unionRoomConfigBODao = new BaseClarkGameDao<>(UnionRoomConfigBO.class);

    @Override
    public CustomerDao getDefaultDao() {
        return unionRoomConfigBODao;
    }

    /**
     * 保存或更新配置
     *
     * @param gameSet 游戏配置
     * @return
     */
    public boolean saveOrUpDate(UnionCreateGameSet gameSet) {
        UnionRoomConfigBO unionRoomConfigBO = new UnionRoomConfigBO();
        if (gameSet.getbRoomConfigure().getGameType().getId() != gameSet.getGameType().getId()) {
            CommLog.error("UnionRoomConfigBOService saveOrUpDate");
            unionRoomConfigBO.setGameId(gameSet.getbRoomConfigure().getGameType().getId());
        } else {
            unionRoomConfigBO.setGameId(gameSet.getGameType().getId());
        }
        unionRoomConfigBO.setUnionId(gameSet.getbRoomConfigure().getBaseCreateRoom().getUnionId());
        unionRoomConfigBO.setStatus(gameSet.getStatus());
        unionRoomConfigBO.setGameConfig(new Gson().toJson(gameSet.getbRoomConfigure()));
        // 是否可以创建
        boolean isCreate = gameSet.getGameIndex() <= 0L;
        // 返回标识
        boolean isSuccess = false;
        try {
            // 查询是否存在
            QueryIdItem queryIdItem = isCreate ? null : findOneE(Restrictions.eq("id", gameSet.getGameIndex()), QueryIdItem.class, QueryIdItem.getItemsName());
            if (Objects.isNull(queryIdItem)) {
                unionRoomConfigBO.setCreateTime(gameSet.getCreateTime());
                long saveOrUpDateId = unionRoomConfigBO.getBaseService().save(unionRoomConfigBO);
                isSuccess = saveOrUpDateId > 0L;
                if (isSuccess) {
                    gameSet.setGameIndex(unionRoomConfigBO.getId());
                }
            } else {
                unionRoomConfigBO.setId(queryIdItem.getId());
                isSuccess = unionRoomConfigBO.savaConfig();
            }
        } catch (Exception e) {
            stackTrace("saveOrUpDate{}", e);
            isSuccess = false;
        }
        return isSuccess;
    }

    /**
     * 更新状态
     *
     * @param id
     * @param status
     */
    public void updateStatus(Long id, Integer status) {
        UnionRoomConfigBO unionRoomConfigBO = unionRoomConfigBODao.findOne(id);
        if (unionRoomConfigBO != null) {
            unionRoomConfigBO.saveStatus(status);
        }
    }


}
