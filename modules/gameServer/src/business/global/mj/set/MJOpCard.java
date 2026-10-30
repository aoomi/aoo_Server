package business.global.mj.set;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 麻将打牌操作
 */
@Data
@NoArgsConstructor
public class MJOpCard {
    /**
     * 操作的牌
     */
    private int opCard;

    public MJOpCard(int opCard) {
        this.opCard = opCard;
    }

    public static MJOpCard OpCard(int opCard) {
        return new MJOpCard(opCard);
    }

}
