# LegacyCommon compatibility boundary

This tree contains the source-only compatibility implementation embedded privately by `gameServer` while migration is in progress.

- The sole public production common framework artifact is `server/GameCommon` (`com.aoo.bcg:game-common`).
- `LegacyCommon` has no POM, no standalone Jar, no local `lib`, and no production launcher.
- New framework, category and gameplay modules must not depend on this tree.
- Only `gameServer` may add its sources through the explicitly named `shared-sources` build-helper execution.
- Binary 2.22 references live under `reference/legacy-2.22` and are never loaded by production.
