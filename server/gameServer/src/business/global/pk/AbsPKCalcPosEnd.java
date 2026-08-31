package business.global.pk;

import com.ddm.server.common.utils.CommMath;
import lombok.Data;

/**
 * 公共的普通算分方式
 *
 * @author Administrator
 */
@Data
public abstract class AbsPKCalcPosEnd {
    public void calcPosEndYiKao(AbsPKSetPos mSetPos) {
        mSetPos.setEndPoint(mSetPos.getEndPoint() + mSetPos.getDeductPoint());
        mSetPos.setDeductEndPoint(CommMath.addDouble(mSetPos.getDeductEndPoint(), mSetPos.getDeductPointYiKao()));
    }
}
