package com.aoo.bcg.mahjong;

import com.aoo.bcg.gamespi.GameCategory;
import com.aoo.bcg.gamespi.GameProvider;
import com.aoo.bcg.gamespi.GameCommandRequest;
import com.aoo.bcg.gamespi.RuleComponent;
import com.aoo.bcg.gamespi.RuleResult;
import com.aoo.bcg.gamespi.RuleStage;
import java.util.List;

public interface MahjongGameProvider extends GameProvider {
    MahjongRuleFamily mahjongFamily();
    @Override default List<RuleComponent<GameCommandRequest>> ruleComponents(){return List.of(new RuleComponent<>(){public String ruleId(){return descriptor().family()+".provider-boundary";}public String componentVersion(){return descriptor().version();}public RuleStage stage(){return RuleStage.PLAY;}public int priority(){return 100;}public RuleResult execute(GameCommandRequest request){validateMahjongCategory();return request.msgId().startsWith("mahjong.")||request.msgId().startsWith("common.room.")?RuleResult.accept():RuleResult.reject("MAHJONG_MESSAGE_REQUIRED","message does not belong to Mahjong");}});}
    default void validateMahjongCategory() {
        if (descriptor().category() != GameCategory.MAHJONG) {
            throw new IllegalStateException("mahjong provider must declare MAHJONG category");
        }
        if (!descriptor().family().equals(mahjongFamily().familyCode())) {
            throw new IllegalStateException("mahjong descriptor and runtime family must match");
        }
    }
}
