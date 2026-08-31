package core.db.service.clarkGame;

import com.ddm.server.annotation.Service;
import core.db.dao.clarkGame.BaseClarkGameDao;
import core.db.entity.clarkGame.PlayerWalletBO;
import core.db.other.Restrictions;
import core.db.persistence.CustomerDao;
import core.db.service.BaseService;

import java.util.Objects;

@Service(source = "clark_game")
public class PlayerWalletBOService implements BaseService<PlayerWalletBO> {
    private final BaseClarkGameDao<PlayerWalletBO> dao = new BaseClarkGameDao<>(PlayerWalletBO.class);

    @Override public CustomerDao getDefaultDao() { return dao; }

    public PlayerWalletBO findByPid(long pid) { return findOne(Restrictions.eq("pid", pid), null); }

    @Override public long saveIgnoreOrUpDate(PlayerWalletBO candidate) {
        PlayerWalletBO existing = findByPid(candidate.getPid());
        if (Objects.nonNull(existing)) {
            candidate.setId(existing.getId());
            candidate.setValue(existing.getValue());
            return existing.getId();
        }
        return saveIgnore(candidate);
    }
}
