package business.global.mj.cdxzmj;

import business.global.mj.AbsMJSetRoom;
import business.global.mj.template.xueZhan.MJTemplateXueZhanSetPosMgr;

public class CDXZMJSetPosMgr extends MJTemplateXueZhanSetPosMgr {
    public CDXZMJSetPosMgr(AbsMJSetRoom set) {
        super(set);
    }

    @Override
    protected boolean checkExistYPDX() {
        return true;
    }

    @Override
    protected boolean checkExistPingHu() {
        return true;
    }

    @Override
    protected boolean checkExistJGBG() {
        return false;
    }

    @Override
    protected boolean checkExistChi() {
        return false;

    }

    /**
     * 检查出牌后是否有人可以接手。
     *
     * @param curOpPos  当前操作位置ID
     * @param curCardID 当前操作牌ID
     */
    @Override
    protected void checkOutOpType(int curOpPos, int curCardID) {
        //王牌可打出，但是不可以吃、碰、杠、胡别人打出的王牌；	
        check_otherPingHu(curOpPos, curCardID);
        if (((CDXZMJSetCard) this.set.getSetCard()).isPopCardNull()) {
            return;
        }
        check_otherJieGang(curOpPos, curCardID);
        check_otherPeng(curOpPos, curCardID);

    }

}		
