package com.aoo.bcg.poker;

import com.aoo.bcg.gamespi.GameCommandRequest;

/**
 * 牌型差异扩展边界。公共房间只传入服务端已经识别的牌型和权威手牌上下文，
 * 因而无需也禁止根据地区名、gameId 或客户端上报的牌型做分支。
 */
@FunctionalInterface
public interface PdkCardPatternPolicy {
    void validate(GameCommandRequest request, CardCombination combination, PaoDeKuaiContext context);

    static PdkCardPatternPolicy standard() {
        return (request, combination, context) -> { };
    }
}
