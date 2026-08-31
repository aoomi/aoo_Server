# R63-001 CI gate

Run `ruby tools/legacy-protocol-ledger/verify_ledger.rb`. The command exits non-zero for a changed
283-item baseline, an unmapped protocol, duplicate legacy/new mapping, missing evidence, or a route
whose production Bootstrap assembly marker disappeared. `build_ledger.rb` is the only writer and
reads the legacy tree without modifying it.
