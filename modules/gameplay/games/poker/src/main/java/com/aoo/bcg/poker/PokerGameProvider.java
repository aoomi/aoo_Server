package com.aoo.bcg.poker;

import com.aoo.bcg.gamespi.GameCategory;
import com.aoo.bcg.gamespi.GameProvider;
import com.aoo.bcg.gamespi.GameCommandRequest;
import com.aoo.bcg.gamespi.RuleComponent;
import com.aoo.bcg.gamespi.RuleResult;
import com.aoo.bcg.gamespi.RuleStage;
import java.util.List;

public interface PokerGameProvider extends GameProvider {
    PokerRuleFamily pokerFamily();
    @Override default java.util.Optional<com.aoo.bcg.gamespi.GameCommandHandler> commandHandler(){return java.util.Optional.of(new PokerDispatchCommandHandler());}
    @Override default List<RuleComponent<GameCommandRequest>> ruleComponents(){return List.of(new RuleComponent<>(){public String ruleId(){return descriptor().family()+".provider-boundary";}public String componentVersion(){return descriptor().version();}public RuleStage stage(){return RuleStage.PLAY;}public int priority(){return 100;}public RuleResult execute(GameCommandRequest request){validatePokerCategory();return request.msgId().startsWith("poker.")||request.msgId().startsWith("common.room.")?RuleResult.accept():RuleResult.reject("POKER_MESSAGE_REQUIRED","message does not belong to Poker");}});}
    default void validatePokerCategory() {
        if (descriptor().category() != GameCategory.POKER) {
            throw new IllegalStateException("poker provider must declare POKER category");
        }
        if (!descriptor().family().equals(pokerFamily().familyCode())) {
            throw new IllegalStateException("poker descriptor and runtime family must match");
        }
    }
}
