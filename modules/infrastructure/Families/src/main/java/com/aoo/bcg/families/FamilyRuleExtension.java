package com.aoo.bcg.families;

import com.aoo.bcg.gamespi.RuleComponent;
import java.util.List;

public interface FamilyRuleExtension<C> {
    String familyCode();
    List<RuleComponent<C>> orderedComponents();
}
