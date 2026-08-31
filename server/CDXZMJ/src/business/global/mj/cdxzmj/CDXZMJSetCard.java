package business.global.mj.cdxzmj;

import business.global.mj.RandomCard;
import business.global.mj.template.MJTemplateSetCard;
import cenum.mj.MJCardCfg;

import java.util.ArrayList;
import java.util.List;

/**
 * 保定易县麻将
 * 每一局麻将底牌信息
 * 抓牌人是逆时针出手
 * 牌是顺时针被抓
 *
 * @author Huaxing
 */
public class CDXZMJSetCard extends MJTemplateSetCard {


    public CDXZMJSetCard(CDXZMJRoomSet set) {
        super(set);
        if (set.getGodInfo().isGodCardMode()) {
            liuPai = set.getGodInfo().getJin(0);
        }
    }


    /**
     * 洗牌
     */
    @Override
    public void randomCard() {
        List<MJCardCfg> mCfgs = new ArrayList<MJCardCfg>();
        if (set.getPlayerNum() > 2) {
            mCfgs.add(MJCardCfg.WANG);
        }
        mCfgs.add(MJCardCfg.TIAO);
        mCfgs.add(MJCardCfg.TONG);

        this.setRandomCard(new RandomCard(mCfgs, this.room.getPlayerNum(), this.room.getXiPaiList().size()));

        this.initDPos(this.set);
    }

    /**
     * 最后4张牌必胡：勾选后，最后4张牌摸到时能胡，不能选择过胡；
     *
     * @return
     */
    public boolean isMustHu() {
        return getRoom().RoomCfg(CDXZMJRoomEnum.KeXuanWanFa.ZUI_HOU_SI_ZHANG_BI_HU) ? randomCard.getSize() - liuPai < 4 : false;
    }

    /**
     * T:首次随机庄，F:房主庄
     */
    @Override
    protected boolean firstRandomDPos() {
        return true;
    }

}			
				
