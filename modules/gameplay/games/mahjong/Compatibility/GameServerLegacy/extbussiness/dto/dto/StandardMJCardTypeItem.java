package business.global.mj.extbussiness.dto;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author zhujianming
 * @date 2021-03-15 14:42
 */
public class StandardMJCardTypeItem<T> implements Serializable {
    public List<List<Integer>> cardList = new ArrayList<>();
    public Map<T, Integer> pointMap = new HashMap<>();

    public StandardMJCardTypeItem(List<List<Integer>> cardList) {
        this.cardList = cardList;
    }

    public StandardMJCardTypeItem() {
    }

    public void addCardTypeToMap(T cardType, Integer point) {
        pointMap.put(cardType, point);
    }
}
