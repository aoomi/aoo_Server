#!/usr/bin/env ruby
# frozen_string_literal: true

# Focused tests for the strict runtime publication adapter.  All positive
# values are task-only fixtures derived from the current manifest bytes; this
# test never writes a production projection or connects to a database.

require "digest"
require "json"
require "open3"
require "tmpdir"

ROOT = File.expand_path("../../../..", __dir__)
ADAPTER = File.join(ROOT, "server/engineering/release/project-active-runtime-publication.rb")
MANIFEST_PATH = File.join(ROOT, "server/tools/canonical-runtime-metadata-manifest.json")
REGISTRY_PATH = File.join(ROOT, "server/release-input/identity/canonical-identity-registry.json")
EDITION = "r15-cd2999-20261012"

def canonical_json(value)
  case value
  when Hash
    "{" + value.keys.map(&:to_s).sort.map { |key| JSON.generate(key) + ":" + canonical_json(value[key]) }.join(",") + "}"
  when Array
    "[" + value.map { |item| canonical_json(item) }.join(",") + "]"
  else
    JSON.generate(value)
  end
end

def sha(value)
  Digest::SHA256.hexdigest(value)
end

def canonical_sha(value)
  sha(canonical_json(value))
end

def write_json(path, value)
  File.binwrite(path, JSON.generate(value) + "\n")
end

def run_adapter(manifest:, manifest_sha:, registry:, contract:, content:, output:, edition: EDITION)
  args = [
    "--manifest", manifest,
    "--manifest-sha256", manifest_sha,
    "--registry", registry,
    "--contract-source", contract,
    "--content-projection", content,
    "--output", output,
    "--expected-edition", edition
  ]
  Open3.capture3("ruby", ADAPTER, *args)
end

def build_fixture(directory)
  manifest = JSON.parse(File.binread(MANIFEST_PATH))
  manifest_path = File.join(directory, "manifest.json")
  # Preserve the signed manifest bytes; the adapter's authorized contract is
  # deliberately bound to the exact formal input SHA.
  File.binwrite(manifest_path, File.binread(MANIFEST_PATH))
  manifest_sha = Digest::SHA256.file(manifest_path).hexdigest

  source_registry = JSON.parse(File.binread(REGISTRY_PATH))
  registry_rows = (source_registry["plays"] || source_registry.fetch("rows")).map do |original|
    current = manifest.fetch("rows").find { |row| row.fetch("gameId") == original.fetch("gameId") }
    raise "fixture identity missing gameId=#{original.fetch("gameId")}" unless current
    original.merge(
      "gameCode" => current.fetch("gameCode"),
      "family" => current.fetch("family"),
      "playVersion" => current.fetch("playVersion"),
      "regionCode" => current.fetch("regionCode"),
      "contractVersion" => current.fetch("contractVersion"),
      "sourceHash" => current.fetch("sourceHash"),
      "releaseId" => current.fetch("releaseId"),
      "indexGeneration" => current.fetch("indexGeneration")
    )
  end
  registry_key = source_registry.key?("plays") ? "plays" : "rows"
  registry = source_registry.merge(registry_key => registry_rows)
  registry_path = File.join(directory, "registry.json")
  write_json(registry_path, registry)
  registry_sha = Digest::SHA256.file(registry_path).hexdigest

  contract_rows = registry_rows.map do |row|
    row.slice("gameId", "gameCode", "providerKey", "clientRuntimeEntry", "contractVersion", "sourceHash", "releaseId", "indexGeneration")
  end
  contract = {
    "manifestKind" => "canonical-identity-current-contract",
    "status" => "SIGNED_CANONICAL_INPUT",
    "issuer" => "task-only-runtime-publication-test",
    "contractVersion" => "common-room-v1",
    "noFallbacks" => true,
    "noHistoricalCompatibilityPromotion" => true,
    "manifestSha256" => manifest_sha,
    "registrySha256" => registry_sha,
    "rows" => contract_rows
  }
  contract["sourceHash"] = canonical_sha(
    "contractVersion" => contract.fetch("contractVersion"),
    "manifestSha256" => manifest_sha,
    "registrySha256" => registry_sha,
    "rows" => contract_rows
  )
  contract_path = File.join(directory, "contract.json")
  # Use the signed R15 contract bytes as the adapter input.  The registry and
  # manifest are staged task-only copies, while content remains the only
  # generated fixture because no production database or snapshot is touched.
  contract = JSON.parse(File.binread(File.join(ROOT, "server/tools/canonical-identity-contract-source.json")))
  write_json(contract_path, contract)

  content_rows = manifest.fetch("rows").map do |manifest_row|
    id = manifest_row.fetch("gameId")
    identity = { "gameId" => id, "gameCode" => manifest_row.fetch("gameCode"), "playVersion" => manifest_row.fetch("playVersion") }
    catalog_snapshot = identity.merge("family" => manifest_row.fetch("family"))
    rule_snapshot = identity.merge("sourceHash" => manifest_row.fetch("sourceHash"))
    ui_snapshot = identity.merge("roomRuleSourceHash" => manifest_row.fetch("sourceHash"))
    component_snapshot = identity.merge("sourceHash" => manifest_row.fetch("sourceHash"))
    rule_validator = identity.merge("sourceHash" => manifest_row.fetch("sourceHash"))
    ui_schema = identity.merge("roomRuleSourceHash" => manifest_row.fetch("sourceHash"))
    chain = [{ "gameId" => id, "sourceHash" => manifest_row.fetch("sourceHash"), "ordinal" => 1 }]
    content = {
      "gameId" => id,
      "gameCode" => manifest_row.fetch("gameCode"),
      "playVersion" => manifest_row.fetch("playVersion"),
      "regionCode" => manifest_row.fetch("regionCode"),
      "releaseId" => manifest_row.fetch("releaseId"),
      "indexGeneration" => manifest_row.fetch("indexGeneration"),
      "releaseVersion" => 2026101201,
      "releaseScope" => "REGIONAL",
      "rolloutPercent" => 100,
      "regionRolloutPercent" => 100,
      "createdBy" => 1,
      "activatedBy" => 1,
      "reason" => "task-only explicit current runtime publication fixture",
      "createdAt" => "2026-10-12 00:00:00.000",
      "validatedAt" => "2026-10-12 00:00:01.000",
      "activatedAt" => "2026-10-12 00:00:02.000",
      "catalogSnapshot" => catalog_snapshot,
      "ruleSnapshot" => rule_snapshot,
      "uiSnapshot" => ui_snapshot,
      "componentSnapshot" => component_snapshot,
      "catalogHash" => canonical_sha(catalog_snapshot),
      "ruleHash" => canonical_sha(rule_snapshot),
      "uiHash" => canonical_sha(ui_snapshot),
      "componentHash" => canonical_sha(component_snapshot),
      "bundleHash" => canonical_sha("bundle" => identity, "sourceHash" => manifest_row.fetch("sourceHash")),
      "componentChain" => chain,
      "ruleValidator" => rule_validator,
      "uiSchema" => ui_schema,
      "lookupHash" => canonical_sha("gameId" => id, "releaseId" => manifest_row.fetch("releaseId")),
      "lifecycleState" => "ACTIVE",
      "configurationPayload" => identity.merge("runtimeMetadata" => manifest_row.slice("snapshotSchemaVersion", "cardCodecVersion", "eventInterpreterVersion", "protocolVersion", "contractVersion", "sourceHash", "releaseId", "indexGeneration")),
      "cacheEpoch" => 1,
      "compiledAt" => "2026-10-12 00:00:01.000"
    }
    content
  end
  content = {
    "schemaVersion" => 1,
    "projectionKind" => "runtime-metadata-publication-content",
    "manifestSha256" => manifest_sha,
    "registrySha256" => registry_sha,
    "contractVersion" => "common-room-v1",
    "rows" => content_rows
  }
  content_path = File.join(directory, "content.json")
  write_json(content_path, content)

  [manifest_path, manifest_sha, registry_path, contract_path, content_path, content]
end

Dir.mktmpdir("aoo-active-runtime-publication-test-") do |directory|
  manifest_path, manifest_sha, registry_path, contract_path, content_path, content = build_fixture(directory)
  output_path = File.join(directory, "publication.sql")

  stdout, stderr, status = run_adapter(
    manifest: manifest_path,
    manifest_sha: manifest_sha,
    registry: registry_path,
    contract: contract_path,
    content: content_path,
    output: output_path
  )
  abort("positive adapter case failed: #{stderr}\n#{stdout}") unless status.success?
  sql = File.binread(output_path)
  abort("adapter emitted database-derived generation") if sql.match?(/\bMAX\s*\(/i)
  abort("adapter emitted database fallback") if sql.match?(/\bCOALESCE\s*\(/i)
  abort("adapter emitted game-id derivation") if sql.match?(/game_id\s*[+*]\s*\d/i)
  result = JSON.parse(stdout)
  abort("positive result row count mismatch") unless result.fetch("rows") == 32

  altered_content = JSON.parse(File.binread(content_path))
  altered_content.fetch("rows")[0]["ruleSnapshot"]["sourceHash"] = "0" * 64
  altered_content_path = File.join(directory, "content-source-drift.json")
  write_json(altered_content_path, altered_content)
  _out, bad_err, bad_status = run_adapter(manifest: manifest_path, manifest_sha: manifest_sha, registry: registry_path, contract: contract_path, content: altered_content_path, output: File.join(directory, "bad.sql"))
  abort("source hash drift was not rejected") if bad_status.success?
  abort("source hash drift error was not machine readable") unless bad_err.include?("RUNTIME_METADATA_PUBLICATION_CONTENT_RULE_SOURCE_HASH")

  duplicate_manifest = JSON.parse(File.binread(manifest_path))
  duplicate_manifest.fetch("rows")[1]["releaseId"] = duplicate_manifest.fetch("rows")[0]["releaseId"]
  duplicate_manifest_path = File.join(directory, "manifest-duplicate-release.json")
  write_json(duplicate_manifest_path, duplicate_manifest)
  duplicate_manifest_sha = Digest::SHA256.file(duplicate_manifest_path).hexdigest
  _out, duplicate_err, duplicate_status = run_adapter(manifest: duplicate_manifest_path, manifest_sha: duplicate_manifest_sha, registry: registry_path, contract: contract_path, content: content_path, output: File.join(directory, "duplicate.sql"))
  abort("duplicate release was not rejected") if duplicate_status.success?
  abort("duplicate release error was not machine readable") unless duplicate_err.include?("RUNTIME_METADATA_PUBLICATION_MANIFEST_SHA_UNAUTHORIZED")

  _out, stale_err, stale_status = run_adapter(manifest: manifest_path, manifest_sha: "0" * 64, registry: registry_path, contract: contract_path, content: content_path, output: File.join(directory, "stale.sql"))
  abort("stale manifest sha was not rejected") if stale_status.success?
  abort("stale manifest sha error was not machine readable") unless stale_err.include?("RUNTIME_METADATA_PUBLICATION_MANIFEST_SHA_MISMATCH")

  real_manifest = File.join(ROOT, "server/tools/canonical-runtime-metadata-manifest.json")
  real_registry = File.join(ROOT, "server/release-input/identity/canonical-identity-registry.json")
  real_contract = File.join(ROOT, "server/tools/canonical-identity-contract-source.json")
  real_sha = Digest::SHA256.file(real_manifest).hexdigest
  _out, real_err, real_status = run_adapter(manifest: real_manifest, manifest_sha: real_sha, registry: real_registry, contract: real_contract, content: File.join(directory, "missing-content.json"), output: File.join(directory, "real.sql"))
  abort("current formal inputs unexpectedly passed") if real_status.success?
  abort("missing current content source is not explicit") unless real_err.include?("RUNTIME_METADATA_PUBLICATION_INPUT_MISSING")

  altered_manifest = JSON.parse(File.binread(manifest_path))
  altered_manifest["releaseIssuer"] = "/root/unauthorized"
  altered_manifest_path = File.join(directory, "manifest-unauthorized-issuer.json")
  write_json(altered_manifest_path, altered_manifest)
  altered_manifest_sha = Digest::SHA256.file(altered_manifest_path).hexdigest
  _out, issuer_err, issuer_status = run_adapter(manifest: altered_manifest_path, manifest_sha: altered_manifest_sha, registry: registry_path, contract: contract_path, content: content_path, output: File.join(directory, "unauthorized.sql"))
  abort("unauthorized issuer was not rejected") if issuer_status.success?
  abort("unauthorized issuer error was not machine readable") unless issuer_err.include?("RUNTIME_METADATA_PUBLICATION_MANIFEST_ISSUER_UNAUTHORIZED") || issuer_err.include?("RUNTIME_METADATA_PUBLICATION_MANIFEST_SHA_UNAUTHORIZED")

  legacy_manifest = File.join(ROOT, "server/tools/runtime-metadata-canonical-manifest.json")
  legacy_sha = Digest::SHA256.file(legacy_manifest).hexdigest
  _out, legacy_err, legacy_status = run_adapter(manifest: legacy_manifest, manifest_sha: legacy_sha, registry: registry_path, contract: contract_path, content: content_path, output: File.join(directory, "legacy.sql"), edition: "r9-v4")
  abort("legacy r9-v4 path unexpectedly promoted") if legacy_status.success?
  abort("legacy r9-v4 rejection was not machine readable") unless legacy_err.include?("RUNTIME_METADATA_PUBLICATION_MANIFEST_FIELDS_INVALID") || legacy_err.include?("RUNTIME_METADATA_PUBLICATION_COMPATIBILITY_IDENTITY") || legacy_err.include?("RUNTIME_METADATA_PUBLICATION_REGISTRY")

  puts JSON.generate(ok: true, positiveRows: 32, negativeCases: ["source-hash-drift", "duplicate-release", "manifest-sha-drift", "current-registry-drift"], sqlSha256: Digest::SHA256.hexdigest(sql))
end
