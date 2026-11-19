package com.aoo.bcg.poker;

import java.util.Objects;
import java.util.Set;

/**
 * playVersion 绑定的不可变策略集合。policyId 必须等于规则版本，恢复时才能 fail-closed，
 * 防止旧快照被错误装配到另一玩法规则上产生静默错算。
 */
public record PdkVariantPolicy(String policyId, PaoDeKuaiFamily family,
        PdkCardPatternPolicy cardPatternPolicy, PdkScoringPolicy scoringPolicy,
        Set<String> enabledOptionalPatterns) {
    private static final Set<String> OPTIONAL_PATTERNS = Set.of("FOUR_WITH_THREE");

    public PdkVariantPolicy(String policyId, PaoDeKuaiFamily family,
            PdkCardPatternPolicy cardPatternPolicy, PdkScoringPolicy scoringPolicy) {
        this(policyId, family, cardPatternPolicy, scoringPolicy, Set.of());
    }

    public PdkVariantPolicy {
        if (policyId == null || policyId.isBlank()) throw new IllegalArgumentException("policyId required");
        Objects.requireNonNull(family);
        Objects.requireNonNull(cardPatternPolicy);
        Objects.requireNonNull(scoringPolicy);
        enabledOptionalPatterns = Set.copyOf(Objects.requireNonNull(enabledOptionalPatterns));
        if (!OPTIONAL_PATTERNS.containsAll(enabledOptionalPatterns))
            throw new IllegalArgumentException("unknown optional PDK pattern");
        if (!policyId.equals(family.profile().version()))
            throw new IllegalArgumentException("policyId must equal playVersion");
    }

    public static PdkVariantPolicy standard(PaoDeKuaiFamily family) {
        return new PdkVariantPolicy(family.profile().version(), family,
                PdkCardPatternPolicy.standard(), PdkScoringPolicy.standard());
    }

    public static PdkVariantPolicy withExplicitOptionalPatterns(PaoDeKuaiFamily family,
            Set<String> enabledOptionalPatterns) {
        return new PdkVariantPolicy(family.profile().version(), family,
                PdkCardPatternPolicy.standard(), PdkScoringPolicy.standard(), enabledOptionalPatterns);
    }

    /**
     * 公共规则解析器能识别某些可选牌型，不代表某个玩法已经发布该能力。
     * 在进入状态机前再次检查不可变策略，避免客户端构造规则字段开启未发布能力。
     */
    public void validatePattern(com.aoo.bcg.gamespi.GameCommandRequest request,
            CardCombination combination, PaoDeKuaiContext context) {
        if (OPTIONAL_PATTERNS.contains(combination.type())
                && !enabledOptionalPatterns.contains(combination.type()))
            throw new IllegalArgumentException("optional PDK pattern is not enabled by variant policy");
        cardPatternPolicy.validate(request, combination, context);
    }
}
