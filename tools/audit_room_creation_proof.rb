#!/usr/bin/env ruby
# frozen_string_literal: true

require "json"
require "fileutils"
require "time"

ROOT = File.expand_path("..", __dir__)
OUTPUT = File.join(ROOT, "work/audit/room-creation-proof-audit.json")
registry_path = File.join(ROOT, "server/Gateway/src/main/java/com/aoo/bcg/gateway/RuntimeGameRoomRegistry.java")
runtime_path = File.join(ROOT, "server/Gateway/src/main/java/com/aoo/bcg/gateway/UnifiedGameRuntime.java")
tests = Dir.glob(File.join(ROOT, "server/**/*Test.java"))
registry = File.read(registry_path)
runtime = File.read(runtime_path)
test_text = tests.map { |path| File.read(path, encoding: "UTF-8", invalid: :replace, undef: :replace) }.join("\n")

checks = {
  "registryIndexHit" => registry.match?(/registryIndexHit/),
  "componentChain" => registry.match?(/componentChain/),
  "playVersion" => registry.match?(/playVersion/),
  "immutableRuleHash" => registry.match?(/immutableRuleHash/),
  "elapsedTime" => registry.match?(/elapsedNanos/),
  "noHalfRoomRegistry" => registry.match?(/catch\s*\(RuntimeException[\s\S]{0,700}rooms\.remove/),
  "providerCompensation" => registry.match?(/compensat(?:e|ion)\s*\(/i),
  "durableAtomicTransaction" => runtime.match?(/transaction|unitOfWork|outbox/i),
  "atomicFailureTest" => test_text.match?(/half.?room|atomicallyRegistered|compensat|factory.*throw/i),
  "evidenceTest" => test_text.match?(/lastCreationEvidence|CreationEvidence/)
}
result = {
  "schemaVersion" => 1,
  "generatedAt" => Time.now.utc.iso8601,
  "gate" => { "passed" => checks.values.all?, "runtimeVerified" => false },
  "summary" => checks.merge("passedChecks" => checks.values.count(true), "totalChecks" => checks.length),
  "checks" => checks,
  "evidenceContract" => %w[roomId gameId gameCode registryIndexHit componentChain playVersion immutableRuleHash elapsedNanos atomicallyRegistered failureType],
  "limitations" => [
    "Registry rollback prevents an addressable half-room but cannot compensate provider-owned external resources without a lifecycle SPI.",
    "Durable room row, billing reservation, outbox and runtime binding still require one transaction/saga and fault-injection tests.",
    "Rule hash canonicalization must move to the published configuration codec before cross-language equivalence is claimed."
  ]
}
FileUtils.mkdir_p(File.dirname(OUTPUT))
File.write(OUTPUT, JSON.pretty_generate(result) + "\n")
puts JSON.generate(result["summary"].merge("passed" => result["gate"]["passed"]))
