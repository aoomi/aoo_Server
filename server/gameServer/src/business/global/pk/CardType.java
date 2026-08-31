package business.global.pk;

import java.util.List;

public interface CardType<T> {

    public boolean resultType(AbsPKSetPos mSetPos, List<Integer> privateCardList);

    public boolean resultType(AbsPKSetPos mSetPos, List<Integer> privateCardList, PKOpCard opCard);

    public PKOpCard robotResultType(AbsPKSetPos mSetPos, PKCurOutCardInfo curOutCardInfo, T item);

}
