# 2.22 isolation gate

Read-only, deterministic audit of Client, Server, Admin and CI configuration. It checks path/version/package/brand/entry references, dynamic loaders, runtime fallbacks, copy bridges, build dependencies, direct old-table SQL, legacy interfaces, duplicate literal route registrations, byte-identical Native prefab aliases, symlinks, and (when the immutable `Test` tree is present) cross-tree Cocos UUID reuse.

```sh
python3 Server/tools/legacy-isolation/gate.py --output Server/tools/legacy-isolation/audit.json
python3 Server/tools/legacy-isolation/test_gate.py
```

Exit `2` means a production/configuration/build reachable finding. Exit `0` means none. Documentation and migration evidence is reported but does not block unless it is in a production/config/build path. An allowlist entry requires the exact stable finding ID and a non-empty reason; the scanner refuses to allowlist production/config/build findings. A file merely containing an identifier named `Legacy` is not a match.

CI without the immutable old tree can pass `--no-legacy-uuid`; release audit should mount it read-only and omit that option. A report is `signable=true` only in full mode when the immutable tree exists, at least one legacy asset root and UUID were loaded, and `blocking=0`. Text-only CI may be `passed=true` but can never be signed as CUR-05 completion.
