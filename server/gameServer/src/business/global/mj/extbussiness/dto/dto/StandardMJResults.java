package business.global.mj.extbussiness.dto;
						
import cenum.mj.HuType;						
import jsproto.c2s.cclass.room.AbsBaseResults;

/**
 * 红中麻将总结算信息						
 *						
 * @author Huaxing						
 */						
public class StandardMJResults extends AbsBaseResults {

    public void addJiePaoPoint(HuType hType) {
        if (HuType.JiePao.equals(hType)||HuType.QGH.equals(hType)) {		
            this.setJiePaoPoint(this.getJiePaoPoint()+1);		
        }		
    }
}
