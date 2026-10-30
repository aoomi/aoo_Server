package business.global.mj.extbussiness.dto;				
				
import business.global.mj.set.MJOpCard;				
				
import java.util.List;				
				
/**				
 * 麻将打牌操作				
 */				
public class StandardMJOpCard extends MJOpCard {
    private List<Integer> gangCardList;				
				
    public StandardMJOpCard(int opCard, List<Integer> gangCardList) {
        this.setOpCard(opCard);				
        this.gangCardList = gangCardList;				
				
    }				
				
    public List<Integer> getGangCardList() {				
        return gangCardList;				
    }				
				
				
    public StandardMJOpCard(int opCard) {
        super(opCard);				
    }				
				
				
    public final static StandardMJOpCard OpCard(int opCard, List<Integer> gangCardList) {
        return new StandardMJOpCard(opCard, gangCardList);
    }				
				
    public static StandardMJOpCard OpCard(int opCard) {
        return new StandardMJOpCard(opCard);
    }				
}						
