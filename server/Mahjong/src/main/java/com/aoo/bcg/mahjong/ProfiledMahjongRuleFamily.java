package com.aoo.bcg.mahjong;

/** Five family engines with the inventory selected by the source-derived archetype. */
final class ProfiledMahjongRuleFamily implements MahjongRuleFamily {
    private final MahjongRegionRuntimeProfile profile;
    private final MahjongRuleSet<MahjongState> rules;
    ProfiledMahjongRuleFamily(MahjongRegionRuntimeProfile profile) {
        this.profile=profile;
        this.rules=switch(profile.family()) {
            case "mahjong:lai-zi" -> new LaiZiMahjongRuleSet(new LaiZiMahjongConfig(45,StandardMahjongConfig.defaults()));
            case "mahjong:tui-dao-hu" -> new StandardMahjongRuleSet(new StandardMahjongConfig(false,true,true));
            case "mahjong:xue-zhan", "mahjong:xue-liu" -> new StandardMahjongRuleSet(new StandardMahjongConfig(false,true,true));
            case "mahjong:standard" -> new StandardMahjongRuleSet(StandardMahjongConfig.defaults());
            default -> throw new IllegalArgumentException("unsupported Mahjong family " + profile.family());
        };
    }
    public String familyCode(){return profile.family().replace(':','-');}
    public MahjongRuleSet<MahjongState> ruleSet(){return rules;}
    public MahjongTileSet tileSet(String version){
        if(profile.reducedSuits())return MahjongTileSet.sichuan(version);
        if(profile.flowerExtended())return MahjongTileSet.standardWithFlowers(version);
        return MahjongTileSet.standard(version);
    }
}
