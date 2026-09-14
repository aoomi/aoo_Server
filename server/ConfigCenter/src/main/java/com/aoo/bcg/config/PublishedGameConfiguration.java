package com.aoo.bcg.config;
import com.aoo.bcg.common.config.RoomRuleSnapshot;
import com.aoo.bcg.families.CompiledRuleChain;
public record PublishedGameConfiguration<C>(RoomRuleSnapshot snapshot, ReleaseManifest manifest, CompiledRuleChain<C> rules) {}
