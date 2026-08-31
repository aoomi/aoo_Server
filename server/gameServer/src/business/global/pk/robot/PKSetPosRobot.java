package business.global.pk.robot;

import business.global.pk.AbsPKSetPos;
import business.global.pk.PKOpCard;
import lombok.Data;

/**
 * 机器人位置操作
 *
 * @author Administrator
 */
@Data
public class PKSetPosRobot {
    protected AbsPKSetPos mSetPos;


    public PKSetPosRobot(AbsPKSetPos mSetPos) {
        this.mSetPos = mSetPos;
    }


    public PKOpCard getAutoCard() {
        return PKOpCard.OpCard(0);
    }


}






