package jsproto.c2s.cclass.club;

import lombok.Data;

import java.util.List;

/**
 * 联赛区间id
 */
@Data
public class SharePromotionSection {
    private List<PromotionShareSectionItem> promotionShareSectionItems;
    private boolean isShowSelf;
    private double minAllowShareToValue;

    public SharePromotionSection(List<PromotionShareSectionItem> promotionShareSectionItems) {
        this.promotionShareSectionItems = promotionShareSectionItems;
    }

}
