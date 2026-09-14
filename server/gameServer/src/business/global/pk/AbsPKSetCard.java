package business.global.pk;


import java.util.ArrayList;
import java.util.List;

/**
 * 扑克牌
 *
 * @author Administrator
 */
public abstract class AbsPKSetCard {
    public abstract ArrayList<Integer> popList(int cnt);

    public abstract List<Integer> forcePopList(List<Integer> forcePop);

    public abstract Integer pop();

    public abstract Integer appointPopCard(int popCard);

    public abstract ArrayList<Integer> popList(int cnt, int i);
}
