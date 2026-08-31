package core.db.service.clarkGame;

import com.ddm.server.annotation.Service;
import core.db.dao.clarkGame.BaseClarkGameDao;
import core.db.entity.clarkGame.PromotionShareSectionBO;
import core.db.other.Restrictions;
import core.db.persistence.BaseDao;
import core.db.persistence.CustomerDao;
import core.db.service.BaseService;
import jsproto.c2s.cclass.club.PromotionShareSectionItem;

@Service(source = "clark_game")
public class PromotionShareSectionBOService implements BaseService<PromotionShareSectionBO> {
    private BaseClarkGameDao<PromotionShareSectionBO> promotionShareSectionBOBaseClarkGameDao = new BaseClarkGameDao<>(PromotionShareSectionBO.class);

    @Override
    public CustomerDao getDefaultDao() {
        return promotionShareSectionBOBaseClarkGameDao;
    }

    /**
     * 保存或者更新（同步）
     *
     * @param element 欲操作实体
     * @return 返回操作结果
     */
    public long saveIgnoreOrUpDateInit(PromotionShareSectionBO element) {
        try {
            PromotionShareSectionItem idItem = findOneE(Restrictions.and(Restrictions.eq("clubId", element.getClubId()), Restrictions.eq("pid", element.getPid()), Restrictions.eq("unionSectionId", element.getUnionSectionId())), PromotionShareSectionItem.class, PromotionShareSectionItem.getItemsName());
            if (idItem != null) {
                element.setId(idItem.getId());
                element.setShareToSelfValue(idItem.getShareToSelfValue());
                return update(element);
            }
        } catch (Exception e) {
            BaseDao.stackTrace("saveIgnoreOrUpDate{}", e);
        }
        return saveIgnore(element);
    }

    /**
     * 保存或者更新（同步）
     *
     * @param element 欲操作实体
     * @return 返回操作结果
     */
    @Override
    public long saveIgnoreOrUpDate(PromotionShareSectionBO element) {
        try {
            PromotionShareSectionItem idItem = findOneE(Restrictions.and(Restrictions.eq("clubId", element.getClubId()), Restrictions.eq("pid", element.getPid()), Restrictions.eq("unionSectionId", element.getUnionSectionId())), PromotionShareSectionItem.class, PromotionShareSectionItem.getItemsName());
            if (idItem != null) {
                element.setId(idItem.getId());
                return update(element);
            }
        } catch (Exception e) {
            BaseDao.stackTrace("saveIgnoreOrUpDate{}", e);
        }
        return saveIgnore(element);
    }
}
