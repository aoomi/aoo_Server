package jsproto.c2s.cclass;

import java.util.List;

/**
 * 推广领取列表
 *
 * @author Huaxing
 */
public class RefererReceiveList {
    private List<RefererReceiveItem> refererReceiveItems;
    private int totalPrice;
    private int totalNumber;

    public RefererReceiveList(List<RefererReceiveItem> refererReceiveItems, int totalPrice, int totalNumber) {
        super();
        this.refererReceiveItems = refererReceiveItems;
        this.totalPrice = totalPrice;
        this.totalNumber = totalNumber;
    }

    public List<RefererReceiveItem> getRefererReceiveItems() {
        return refererReceiveItems;
    }

    public void setRefererReceiveItems(List<RefererReceiveItem> refererReceiveItems) {
        this.refererReceiveItems = refererReceiveItems;
    }

    public int getTotalPrice() {
        return totalPrice;
    }

    public void setTotalPrice(int totalPrice) {
        this.totalPrice = totalPrice;
    }

    public int getTotalNumber() {
        return totalNumber;
    }

    public void setTotalNumber(int totalNumber) {
        this.totalNumber = totalNumber;
    }


}
