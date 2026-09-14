package business.global.mj.extbussiness.dto.iclass;
			
import jsproto.c2s.iclass.S_GetRoomInfo;			
			
public class SStandardMJ_GetRoomInfo extends S_GetRoomInfo {

    private int quanShu = 0;//圈数 -1 不显示

    public void setQuanShu(int quanShu) {
        this.quanShu = quanShu;
    }
}
