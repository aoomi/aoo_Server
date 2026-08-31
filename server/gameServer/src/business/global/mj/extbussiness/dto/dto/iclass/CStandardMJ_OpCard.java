package business.global.mj.extbussiness.dto.iclass;
						
import jsproto.c2s.iclass.mj.CMJ_OpCard;						
						
import java.util.List;						
						
public class CStandardMJ_OpCard extends CMJ_OpCard {
						
    private List<Integer> gangCardList;						
						
    public List<Integer> getGangCardList() {						
        return gangCardList;						
    }						
						
    public void setGangCardList(List<Integer> gangCardList) {						
        this.gangCardList = gangCardList;						
    }						
}						
