# R04 Mahjong family de-duplication audit

## Boundary and result

The module currently contains 54 `GameProvider` classes and 54 session classes (including the generic Mahjong provider/session). A provider name or an identical SPI method body is not sufficient evidence that two catalog codes are one game family. The required fingerprint is:

`source lineage + create-contract mapping + state-machine phases + physical tile model + settlement lifecycle`.

The rejected JJMJ188/JLMJ193 pilot proved this boundary: JJMJ has 72/108 suit removal, tail oil cards and oil settlement; JLMJ has a 144-card flower wall, opening replacement, dynamic treasure, egg parity reserves and dealer transitions. Their temporary provider-parent extraction was reverted.

## State-machine fingerprints

These groups are mechanical candidates, not permission to merge rules:

- Core turn loop only: AH HBMJ, AHHNMJ, BDJHMJ, CCMJ, CHMJ, DXBJMJ, FJYXMJ, FXMJ, FZJXMJ, GDMJ, HFBZMJ, HFMJ, JCAHMJ, JCHHMJ, JJMJ, JMHHMJ, JSYCMJ and the generic provider.
- Pre-game/round declaration (`WAITING_EX` or equivalent): BZMJ, BZQZMJ, CXMJ, CXYXMJ, CZMJ, DNMJ, DTLGFMJ, GDCZMJ, GDJYMJ, GFT258MJ, GSLZMJ, GSMJ, HAMJ, HBMJ, HBWHMJ, HBYXMJ, HNCSMJ, HNHBMJ, HNJYMJ, HNXCMJ, HNXYMJ, HTMJ and JSSQMJ.
- Multi-response discard window: DZSJZMJ, GAMJ and HBHBMJ. These cannot share the single-response loop without preserving pending decisions, timeout resolution, seat priority and multi-winner settlement.
- Non-standard initial hand: DZSJZMJ (19 cards) and FDMJ (16 cards). These cannot use the generic 13-card deal lifecycle.

## Tile-model fingerprints

- Dynamic indicator/wildcard: AFMJ, AHMJ, CCMJ, CZMJ, DXBJMJ, FDMJ, FJYXMJ, FZJXMJ, GDCZMJ, GDJYMJ, GFT258MJ, GNMJ, GSLZMJ, GYZJMJ, GZMJ, HAMJ, HBYXMJ, HNMJ, HNXCMJ, HYHSMJ, JCHHMJ, JLMJ, JMHHMJ and RJMJ.
- Tail award cards: DNMJ, FZJXMJ, GDCZMJ, GDJYMJ, GDMJ, GFT258MJ, GSMJ, HBMJ, HBWHMJ, HNCSMJ, HNHBMJ, HTMJ, HYHSMJ and JJMJ.
- Flower replacement: AFMJ, AHHBMJ, AHMJ, DTLGFMJ, FDMJ, FZMJ, GNMJ, GYZJMJ, GZMJ, HNMJ, HNXCMJ, JCAHMJ, JLMJ, JSSQMJ and RJMJ.

Dynamic indicators, tail awards and flowers are independent axes. Combining sessions across an axis is forbidden unless physical removal/replacement and restore/replay representations are identical.

## First valid source-derived family

BZMJ189 and BZQZMJ195 are the first viable pilot:

- Both derive from the Bozhou ZhangZhuang lineage.
- Both use fixed old tile `6001` mapped to white-board `47` as wildcard.
- Both reserve 14 wall tiles and have the dealer-only ZhangZhuang phase.
- Both share join/ready/deal/turn/operation/replay/restore/reconnect lifecycle.
- BZQZ is a regional profile extension: BaoTing, LiuZui and its scoring pattern table must remain rule components, not branches copied into another provider.

## Target architecture

1. `BozhouFamilyProvider` is the sole runtime implementation for the source family.
2. `BozhouRegionProfile` owns descriptor-independent config decoding, rules, family construction and settlement policy for `bzmj` and `bzqzmj`.
3. `BozhouFamilySession` owns the single state machine. Its snapshot contains a stable profile code and profile-owned state; restore resolves the same profile before reconstructing the core.
4. A public, Mahjong-module-owned `MahjongCatalogRuntimeRegistry` maps catalog code to a runtime binding factory.
5. Bootstrap's catalog loader asks that registry for a native binding before falling back to `CatalogGameProvider`. This is an internal Bootstrap-to-Mahjong dependency; `GameSPI`, Gateway and wire messages remain unchanged.
6. The ServiceLoader contains one Bozhou family entry, not one entry per catalog code. Bootstrap expands its catalog bindings into normal per-descriptor `GameProvider` views for `GameRegistry`.

## Acceptance gates

- Exactly one Bozhou ServiceLoader entry and no BZMJ/BZQZMJ entries.
- Catalog registry exposes both IDs/codes with their original descriptors and versions.
- One family nine-stage test runs lifecycle/replay/restore/reconnect once.
- A two-row profile matrix proves config, legal-Hu and settlement differences.
- Snapshot restoration rejects a mismatched profile code.
- Existing BZMJ/BZQZ behavioral assertions remain green.
- Full Mahjong and Bootstrap catalog/ServiceLoader tests pass.

No bulk provider deletion is authorized until this pilot satisfies all gates.
