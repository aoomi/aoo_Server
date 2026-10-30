package business.global.mj.set;

import java.util.List;


public class MJTemplate_OpCard extends MJOpCard {

    private List<Integer> cardList;

    public MJTemplate_OpCard(int opCard, List<Integer> cardList) {
        super(opCard);
        this.cardList = cardList;
    }

    public List<Integer> getCardList() {
        return cardList;
    }

    public void setCardList(List<Integer> cardList) {
        this.cardList = cardList;
    }

    public static MJOpCard OpCard(int opCard, List<Integer> cardList) {
        return new MJTemplate_OpCard(opCard, cardList);
    }
}
