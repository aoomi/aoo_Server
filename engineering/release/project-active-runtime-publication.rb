#!/usr/bin/env ruby
# frozen_string_literal: true

# Strict, forward-only runtime publication adapter.
#
# This tool consumes three signed/current projections and one explicit content
# projection.  It never reads a database and never derives a publish value from
# a database row, game id, environment variable, default, or historical
# snapshot.  It can emit SQL for a disposable database only after every value
# needed by the immutable release/index/configuration tables has been supplied
# and cross-checked.

require "digest"
require "json"
require "optparse"

HASH = /\A[0-9a-f]{64}\z/.freeze
VERSION = /\A[A-Za-z0-9][A-Za-z0-9._-]{0,127}\z/.freeze
EDITION = /\A[A-Za-z0-9][A-Za-z0-9._-]{0,127}\z/.freeze
AUTHORIZED_MANIFEST_CONTRACTS = {
  # Historical contract is retained as an explicit compatibility input.  It
  # is never selected implicitly for the current projection and its old 630
  # row remains rejected by validate_manifest!.
  "r9-v4" => {
    manifest_version: "r9-v4",
    release_issuer: "/root/map_runtime_release_values",
    release_edition: "r9-v4",
    manifest_sha256: "7f26b45d6a3dec5290004367f5db1f83c223b08cbf883f2e0356810531add613"
  },
  # R15 is an authorized current edition: the manifestVersion is the current
  # R50 schema edition while releaseEdition/issuer are bound to the R15
  # identity issuance receipt.  No arbitrary issuer or edition is accepted.
  "r15-cd2999-20261012" => {
    manifest_version: "r50-current-20261011",
    release_issuer: "/root/lobby_runtime_from_staged_source",
    release_edition: "r15-cd2999-20261012",
    manifest_sha256: "a453cb986a674f0f6044a581f8e69927a4cfbf94f531567ef021dfaee07a8282",
    contract_manifest_version: "batch2-r2",
    contract_source_hash: "12f7ac19c77d0bf39cab2f78eb2f7c6e8eefbc0dc4e578be9442ff84a0a7b075"
  }
}.freeze
REQUIRED_MANIFEST_FIELDS = %w[
  snapshotSchemaVersion cardCodecVersion eventInterpreterVersion protocolVersion
  contractVersion sourceHash releaseId indexGeneration
].freeze
REGISTRY_FIELDS = %w[
  gameId gameCode family playVersion regionCode providerKey clientRuntimeEntry
  contractVersion sourceHash releaseId indexGeneration
].freeze
CONTENT_FIELDS = %w[
  gameId gameCode playVersion regionCode releaseId indexGeneration releaseVersion
  releaseScope rolloutPercent regionRolloutPercent createdBy activatedBy reason
  createdAt validatedAt activatedAt catalogSnapshot ruleSnapshot uiSnapshot
  componentSnapshot catalogHash ruleHash uiHash componentHash bundleHash
  componentChain ruleValidator uiSchema lookupHash lifecycleState
  configurationPayload cacheEpoch compiledAt
].freeze

class PublicationError < StandardError
  attr_reader :code

  def initialize(code, detail = nil)
    @code = code
    super([code, detail].compact.join("|"))
  end
end

def fail!(code, detail = nil)
  raise PublicationError.new(code, detail)
end

def read_json(path, role)
  fail!("RUNTIME_METADATA_PUBLICATION_INPUT_MISSING", "role=#{role}|path=#{path}") unless File.file?(path)
  JSON.parse(File.binread(path))
rescue JSON::ParserError => error
  fail!("RUNTIME_METADATA_PUBLICATION_INPUT_INVALID", "role=#{role}|#{error.message}")
end

def file_sha(path, role)
  fail!("RUNTIME_METADATA_PUBLICATION_INPUT_MISSING", "role=#{role}|path=#{path}") unless File.file?(path)
  Digest::SHA256.file(path).hexdigest
end

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

def canonical_sha(value)
  Digest::SHA256.hexdigest(canonical_json(value))
end

def require_hash(value, code, detail)
  fail!(code, detail) unless value.is_a?(String) && value.match?(HASH)
  value
end

def require_version(value, code, detail)
  fail!(code, detail) unless value.is_a?(String) && value.match?(VERSION)
  value
end

def require_positive_integer(value, code, detail)
  fail!(code, detail) unless value.is_a?(Integer) && value.positive?
  value
end

def require_text(value, code, detail)
  fail!(code, detail) unless value.is_a?(String) && !value.strip.empty?
  value
end

def sql_string(value)
  "'#{value.to_s.gsub("'", "''")}'"
end

def sql_json(value)
  sql_string(canonical_json(value))
end

def sql_number(value)
  fail!("RUNTIME_METADATA_PUBLICATION_INTERNAL_NUMBER", value.inspect) unless value.is_a?(Integer) && value >= 0
  value.to_s
end

def sql_time(value)
  require_text(value, "RUNTIME_METADATA_PUBLICATION_TIMESTAMP_INVALID", value.inspect)
  sql_string(value)
end

def load_registry_rows(document)
  rows = document["plays"] || document["rows"]
  fail!("RUNTIME_METADATA_PUBLICATION_REGISTRY_ROWS_MISSING") unless rows.is_a?(Array)
  rows
end

def authorized_manifest_contract!(expected_edition, contract_path = nil, contract_sha = nil)
  if contract_path || contract_sha
    fail!("RUNTIME_METADATA_PUBLICATION_AUTHORITY_PAIR_REQUIRED") unless
      contract_path && contract_sha && contract_sha.match?(HASH)
    fail!("RUNTIME_METADATA_PUBLICATION_AUTHORITY_PATH_INVALID") unless
      contract_path.start_with?("/") && File.file?(contract_path)
    bytes = File.binread(contract_path)
    fail!("RUNTIME_METADATA_PUBLICATION_AUTHORITY_SHA_MISMATCH") unless Digest::SHA256.hexdigest(bytes) == contract_sha
    explicit = JSON.parse(bytes)
    keys = %w[contractKind schemaVersion manifest_version release_issuer release_edition manifest_sha256 contract_manifest_version contract_source_hash contract_manifest_path]
    fail!("RUNTIME_METADATA_PUBLICATION_AUTHORITY_SCHEMA_INVALID") unless
      explicit.is_a?(Hash) && explicit.keys.sort == keys.sort &&
      explicit["contractKind"] == "approved-active-runtime-projection-contract" && explicit["schemaVersion"] == 1
    keys.drop(2).each do |key|
      require_text(explicit[key], "RUNTIME_METADATA_PUBLICATION_AUTHORITY_FIELD_INVALID", key)
    end
    fail!("RUNTIME_METADATA_PUBLICATION_AUTHORITY_EDITION_MISMATCH") unless explicit["release_edition"] == expected_edition
    %w[manifest_sha256 contract_source_hash].each do |key|
      fail!("RUNTIME_METADATA_PUBLICATION_AUTHORITY_HASH_INVALID", key) unless explicit[key].match?(HASH)
    end
    path = explicit["contract_manifest_path"]
    fail!("RUNTIME_METADATA_PUBLICATION_AUTHORITY_EVIDENCE_PATH_INVALID") if
      path.start_with?("/") || path.split("/").include?("..") || path.include?("\0")
    return explicit.reject { |key, _| %w[contractKind schemaVersion].include?(key) }.transform_keys(&:to_sym)
  end
  contract = AUTHORIZED_MANIFEST_CONTRACTS[expected_edition]
  fail!("RUNTIME_METADATA_PUBLICATION_EDITION_UNAUTHORIZED", "edition=#{expected_edition}") unless contract
  contract
end

def validate_manifest!(manifest, manifest_sha, expected_edition, authorized = nil)
  authorized ||= authorized_manifest_contract!(expected_edition)
  fail!("RUNTIME_METADATA_PUBLICATION_MANIFEST_KIND_INVALID") unless manifest["manifestKind"] == "runtime-metadata-canonical"
  fail!("RUNTIME_METADATA_PUBLICATION_MANIFEST_STATUS_INVALID") unless manifest["manifestStatus"] == "SIGNED_CANONICAL_INPUT"
  fail!("RUNTIME_METADATA_PUBLICATION_MANIFEST_NOT_CONSUMABLE") unless manifest["consumable"] == true
  fail!("RUNTIME_METADATA_PUBLICATION_MANIFEST_VERSION_UNAUTHORIZED") unless manifest["manifestVersion"] == authorized[:manifest_version]
  fail!("RUNTIME_METADATA_PUBLICATION_MANIFEST_ISSUER_UNAUTHORIZED") unless manifest["releaseIssuer"] == authorized[:release_issuer]
  fail!("RUNTIME_METADATA_PUBLICATION_MANIFEST_EDITION_INVALID") unless manifest["releaseEdition"] == authorized[:release_edition]
  fail!("RUNTIME_METADATA_PUBLICATION_MANIFEST_SHA_UNAUTHORIZED") unless manifest_sha == authorized[:manifest_sha256]
  fail!("RUNTIME_METADATA_PUBLICATION_MANIFEST_FIELDS_INVALID") unless manifest["requiredFields"] == REQUIRED_MANIFEST_FIELDS
  rows = manifest["rows"]
  fail!("RUNTIME_METADATA_PUBLICATION_MANIFEST_ROW_COUNT", rows.inspect) unless rows.is_a?(Array) && rows.length == 32

  seen = {}
  seen_releases = {}
  seen_generations = {}
  rows.each_with_index do |row, index|
    fail!("RUNTIME_METADATA_PUBLICATION_MANIFEST_ROW_INVALID", "index=#{index}") unless row.is_a?(Hash)
    missing = %w[gameId gameCode family playVersion regionCode] + REQUIRED_MANIFEST_FIELDS
    missing = missing.reject { |field| row.key?(field) }
    fail!("RUNTIME_METADATA_PUBLICATION_MANIFEST_ROW_FIELDS", "index=#{index}|missing=#{missing.join(",")}") unless missing.empty?
    id = require_positive_integer(row["gameId"], "RUNTIME_METADATA_PUBLICATION_MANIFEST_GAME_ID", "index=#{index}")
    fail!("RUNTIME_METADATA_PUBLICATION_COMPATIBILITY_IDENTITY", "gameId=#{id}") if id == 516 || row["playVersion"].to_s.downcase.start_with?("legacy-")
    %w[gameCode family playVersion regionCode].each { |field| require_text(row[field], "RUNTIME_METADATA_PUBLICATION_MANIFEST_FIELD", "field=#{field}|gameId=#{id}") }
    %w[snapshotSchemaVersion cardCodecVersion eventInterpreterVersion protocolVersion contractVersion].each do |field|
      require_version(row[field], "RUNTIME_METADATA_PUBLICATION_MANIFEST_VERSION", "field=#{field}|gameId=#{id}")
    end
    require_hash(row["sourceHash"], "RUNTIME_METADATA_PUBLICATION_MANIFEST_SOURCE_HASH", "gameId=#{id}")
    %w[releaseId indexGeneration].each { |field| require_positive_integer(row[field], "RUNTIME_METADATA_PUBLICATION_MANIFEST_LITERAL", "field=#{field}|gameId=#{id}") }
    fail!("RUNTIME_METADATA_PUBLICATION_MANIFEST_DUPLICATE_ID", "gameId=#{id}") if seen.key?(id)
    fail!("RUNTIME_METADATA_PUBLICATION_MANIFEST_DUPLICATE_RELEASE", "releaseId=#{row["releaseId"]}") if seen_releases.key?(row["releaseId"])
    fail!("RUNTIME_METADATA_PUBLICATION_MANIFEST_DUPLICATE_GENERATION", "indexGeneration=#{row["indexGeneration"]}") if seen_generations.key?(row["indexGeneration"])
    seen[id] = true
    seen_releases[row["releaseId"]] = true
    seen_generations[row["indexGeneration"]] = true
  end
  fail!("RUNTIME_METADATA_PUBLICATION_MANIFEST_SHA_MISMATCH", "expected=#{manifest_sha}") unless manifest_sha.match?(HASH)
  rows
end

def validate_registry!(registry, manifest_rows)
  rows = load_registry_rows(registry)
  fail!("RUNTIME_METADATA_PUBLICATION_REGISTRY_ROW_COUNT", rows.length) unless rows.length == 32
  by_id = rows.to_h { |row| [row["gameId"], row] }
  fail!("RUNTIME_METADATA_PUBLICATION_REGISTRY_DUPLICATE_ID") unless by_id.length == rows.length
  manifest_by_id = manifest_rows.to_h { |row| [row["gameId"], row] }
  fail!("RUNTIME_METADATA_PUBLICATION_REGISTRY_ID_SET_DRIFT") unless by_id.keys.sort == manifest_by_id.keys.sort
  rows.each do |row|
    id = row["gameId"]
    fail!("RUNTIME_METADATA_PUBLICATION_REGISTRY_ROW_INVALID", "gameId=#{id}") unless row.is_a?(Hash)
    REGISTRY_FIELDS.each { |field| fail!("RUNTIME_METADATA_PUBLICATION_REGISTRY_FIELD_MISSING", "field=#{field}|gameId=#{id}") unless row.key?(field) }
    %w[providerKey clientRuntimeEntry].each { |field| require_text(row[field], "RUNTIME_METADATA_PUBLICATION_REGISTRY_FIELD_INVALID", "field=#{field}|gameId=#{id}") }
    expected = manifest_by_id.fetch(id)
    %w[gameId gameCode family playVersion regionCode contractVersion sourceHash releaseId indexGeneration].each do |field|
      fail!("RUNTIME_METADATA_PUBLICATION_REGISTRY_MISMATCH", "field=#{field}|gameId=#{id}") unless row[field] == expected[field]
    end
  end
  rows
end

def validate_contract!(contract, manifest_sha, registry_sha, registry_rows, authorized)
  fail!("RUNTIME_METADATA_PUBLICATION_CONTRACT_KIND_INVALID") unless contract["manifestKind"] == "canonical-identity-current-contract"
  fail!("RUNTIME_METADATA_PUBLICATION_CONTRACT_STATUS_INVALID") unless contract["status"] == "SIGNED_CANONICAL_INPUT"
  contract_version = require_version(contract["contractVersion"], "RUNTIME_METADATA_PUBLICATION_CONTRACT_VERSION_INVALID", "contractVersion")
  fail!("RUNTIME_METADATA_PUBLICATION_CONTRACT_NO_FALLBACK") unless contract["noFallbacks"] == true && contract["noHistoricalCompatibilityPromotion"] == true
  if authorized[:contract_manifest_version]
    fail!("RUNTIME_METADATA_PUBLICATION_CONTRACT_VERSION_BINDING_MISSING") unless contract["manifestVersion"] == authorized[:contract_manifest_version]
    fail!("RUNTIME_METADATA_PUBLICATION_CONTRACT_ISSUER_BINDING_MISSING") unless contract["issuer"] == authorized[:release_issuer]
    fail!("RUNTIME_METADATA_PUBLICATION_CONTRACT_EDITION_BINDING_MISSING") unless contract["releaseEdition"] == authorized[:release_edition]
    fail!("RUNTIME_METADATA_PUBLICATION_CONTRACT_SOURCE_HASH_MISMATCH") unless contract["sourceHash"] == authorized[:contract_source_hash]
    evidence = contract["evidence"]
    manifest_evidence = evidence.is_a?(Array) && evidence.find { |item| item.is_a?(Hash) && item["path"] == (authorized[:contract_manifest_path] || "Server/tools/canonical-runtime-metadata-manifest.json") }
    fail!("RUNTIME_METADATA_PUBLICATION_CONTRACT_MANIFEST_BINDING_MISSING") unless manifest_evidence && manifest_evidence["sha256"] == manifest_sha
  else
    fail!("RUNTIME_METADATA_PUBLICATION_CONTRACT_MANIFEST_BINDING_MISSING") unless contract["manifestSha256"] == manifest_sha
    fail!("RUNTIME_METADATA_PUBLICATION_CONTRACT_REGISTRY_BINDING_MISSING") unless contract["registrySha256"] == registry_sha
  end
  contract_rows = contract["rows"]
  fail!("RUNTIME_METADATA_PUBLICATION_CONTRACT_ROW_COUNT", contract_rows.inspect) unless contract_rows.is_a?(Array) && contract_rows.length == 32
  by_id = contract_rows.to_h { |row| [row["gameId"], row] }
  fail!("RUNTIME_METADATA_PUBLICATION_CONTRACT_ID_SET_DRIFT") unless by_id.keys.sort == registry_rows.map { |row| row["gameId"] }.sort
  registry_rows.each do |registry_row|
    row = by_id.fetch(registry_row["gameId"], nil)
    fail!("RUNTIME_METADATA_PUBLICATION_CONTRACT_ROW_MISSING", "gameId=#{registry_row["gameId"]}") unless row
    contract_fields = authorized[:contract_manifest_version] ?
      %w[gameId gameCode contractVersion sourceHash releaseId indexGeneration] :
      %w[gameId gameCode providerKey clientRuntimeEntry contractVersion sourceHash releaseId indexGeneration]
    contract_fields.each do |field|
      fail!("RUNTIME_METADATA_PUBLICATION_CONTRACT_ROW_DRIFT", "field=#{field}|gameId=#{registry_row["gameId"]}") unless row[field] == registry_row[field]
    end
  end
  unless authorized[:contract_manifest_version]
    expected_source_hash = canonical_sha(
      "contractVersion" => contract_version,
      "manifestSha256" => manifest_sha,
      "registrySha256" => registry_sha,
      "rows" => contract_rows
    )
    fail!("RUNTIME_METADATA_PUBLICATION_CONTRACT_SOURCE_HASH_MISMATCH") unless contract["sourceHash"] == expected_source_hash
  end
  contract_version
end

def validate_snapshot!(snapshot, row, field, source_hash)
  fail!("RUNTIME_METADATA_PUBLICATION_CONTENT_SNAPSHOT_INVALID", "field=#{field}|gameId=#{row["gameId"]}") unless snapshot.is_a?(Hash)
  %w[gameId gameCode playVersion].each do |identity_field|
    fail!("RUNTIME_METADATA_PUBLICATION_CONTENT_IDENTITY_DRIFT", "field=#{field}.#{identity_field}|gameId=#{row["gameId"]}") unless snapshot[identity_field] == row[identity_field]
  end
  if field == "ruleSnapshot"
    fail!("RUNTIME_METADATA_PUBLICATION_CONTENT_RULE_SOURCE_HASH", "gameId=#{row["gameId"]}") unless snapshot["sourceHash"] == source_hash
  end
  if field == "uiSnapshot"
    fail!("RUNTIME_METADATA_PUBLICATION_CONTENT_UI_SOURCE_HASH", "gameId=#{row["gameId"]}") unless snapshot["roomRuleSourceHash"] == source_hash
  end
end

def validate_content!(content, manifest_rows, manifest_sha, registry_sha, contract_version)
  fail!("RUNTIME_METADATA_PUBLICATION_CONTENT_KIND_INVALID") unless content["projectionKind"] == "runtime-metadata-publication-content"
  fail!("RUNTIME_METADATA_PUBLICATION_CONTENT_SCHEMA_INVALID") unless content["schemaVersion"] == 1
  fail!("RUNTIME_METADATA_PUBLICATION_CONTENT_MANIFEST_BINDING_MISSING") unless content["manifestSha256"] == manifest_sha
  fail!("RUNTIME_METADATA_PUBLICATION_CONTENT_REGISTRY_BINDING_MISSING") unless content["registrySha256"] == registry_sha
  fail!("RUNTIME_METADATA_PUBLICATION_CONTENT_CONTRACT_BINDING_MISSING") unless content["contractVersion"] == contract_version
  rows = content["rows"]
  fail!("RUNTIME_METADATA_PUBLICATION_CONTENT_ROW_COUNT", rows.inspect) unless rows.is_a?(Array) && rows.length == 32
  manifest_by_id = manifest_rows.to_h { |row| [row["gameId"], row] }
  by_id = rows.to_h { |row| [row["gameId"], row] }
  fail!("RUNTIME_METADATA_PUBLICATION_CONTENT_DUPLICATE_ID") unless by_id.length == rows.length
  fail!("RUNTIME_METADATA_PUBLICATION_CONTENT_ID_SET_DRIFT") unless by_id.keys.sort == manifest_by_id.keys.sort
  seen_releases = {}
  seen_generations = {}
  rows.each do |row|
    id = row["gameId"]
    missing = CONTENT_FIELDS.reject { |field| row.key?(field) }
    fail!("RUNTIME_METADATA_PUBLICATION_CONTENT_FIELDS_MISSING", "gameId=#{id}|missing=#{missing.join(",")}") unless missing.empty?
    expected = manifest_by_id.fetch(id)
    %w[gameId gameCode playVersion regionCode releaseId indexGeneration].each do |field|
      fail!("RUNTIME_METADATA_PUBLICATION_CONTENT_IDENTITY_DRIFT", "field=#{field}|gameId=#{id}") unless row[field] == expected[field]
    end
    fail!("RUNTIME_METADATA_PUBLICATION_CONTENT_DUPLICATE_RELEASE", "releaseId=#{row["releaseId"]}") if seen_releases.key?(row["releaseId"])
    fail!("RUNTIME_METADATA_PUBLICATION_CONTENT_DUPLICATE_GENERATION", "indexGeneration=#{row["indexGeneration"]}") if seen_generations.key?(row["indexGeneration"])
    seen_releases[row["releaseId"]] = true
    seen_generations[row["indexGeneration"]] = true
    require_positive_integer(row["releaseVersion"], "RUNTIME_METADATA_PUBLICATION_CONTENT_RELEASE_VERSION", "gameId=#{id}")
    fail!("RUNTIME_METADATA_PUBLICATION_CONTENT_RELEASE_SCOPE", "gameId=#{id}") unless %w[GLOBAL REGIONAL CANARY].include?(row["releaseScope"])
    %w[rolloutPercent regionRolloutPercent].each do |field|
      fail!("RUNTIME_METADATA_PUBLICATION_CONTENT_ROLLOUT", "field=#{field}|gameId=#{id}") unless row[field].is_a?(Numeric) && row[field] >= 0 && row[field] <= 100
    end
    %w[createdBy activatedBy cacheEpoch].each { |field| require_positive_integer(row[field], "RUNTIME_METADATA_PUBLICATION_CONTENT_NUMBER", "field=#{field}|gameId=#{id}") }
    %w[reason createdAt validatedAt activatedAt compiledAt].each { |field| require_text(row[field], "RUNTIME_METADATA_PUBLICATION_CONTENT_TEXT", "field=#{field}|gameId=#{id}") }
    %w[catalogSnapshot ruleSnapshot uiSnapshot componentSnapshot configurationPayload ruleValidator uiSchema].each do |field|
      fail!("RUNTIME_METADATA_PUBLICATION_CONTENT_OBJECT", "field=#{field}|gameId=#{id}") unless row[field].is_a?(Hash)
    end
    validate_snapshot!(row["catalogSnapshot"], row, "catalogSnapshot", expected["sourceHash"])
    validate_snapshot!(row["ruleSnapshot"], row, "ruleSnapshot", expected["sourceHash"])
    validate_snapshot!(row["uiSnapshot"], row, "uiSnapshot", expected["sourceHash"])
    validate_snapshot!(row["componentSnapshot"], row, "componentSnapshot", expected["sourceHash"])
    fail!("RUNTIME_METADATA_PUBLICATION_CONTENT_RULE_VALIDATOR", "gameId=#{id}") unless row["ruleValidator"].values_at("gameId", "gameCode", "playVersion", "sourceHash") == [id, row["gameCode"], row["playVersion"], expected["sourceHash"]]
    fail!("RUNTIME_METADATA_PUBLICATION_CONTENT_UI_SCHEMA", "gameId=#{id}") unless row["uiSchema"]["roomRuleSourceHash"] == expected["sourceHash"]
    runtime_metadata = row["configurationPayload"]["runtimeMetadata"]
    fail!("RUNTIME_METADATA_PUBLICATION_CONTENT_CONFIGURATION_METADATA", "gameId=#{id}") unless runtime_metadata.is_a?(Hash) && REQUIRED_MANIFEST_FIELDS.all? { |field| runtime_metadata[field] == expected[field] }
    fail!("RUNTIME_METADATA_PUBLICATION_CONTENT_CHAIN", "gameId=#{id}") unless row["componentChain"].is_a?(Array) && !row["componentChain"].empty?
    %w[catalogHash ruleHash uiHash componentHash bundleHash lookupHash].each do |field|
      require_hash(row[field], "RUNTIME_METADATA_PUBLICATION_CONTENT_HASH", "field=#{field}|gameId=#{id}")
    end
    fail!("RUNTIME_METADATA_PUBLICATION_CONTENT_CATALOG_HASH", "gameId=#{id}") unless row["catalogHash"] == canonical_sha(row["catalogSnapshot"])
    fail!("RUNTIME_METADATA_PUBLICATION_CONTENT_RULE_HASH", "gameId=#{id}") unless row["ruleHash"] == canonical_sha(row["ruleSnapshot"])
    fail!("RUNTIME_METADATA_PUBLICATION_CONTENT_UI_HASH", "gameId=#{id}") unless row["uiHash"] == canonical_sha(row["uiSnapshot"])
    fail!("RUNTIME_METADATA_PUBLICATION_CONTENT_COMPONENT_HASH", "gameId=#{id}") unless row["componentHash"] == canonical_sha(row["componentSnapshot"])
    fail!("RUNTIME_METADATA_PUBLICATION_CONTENT_LIFECYCLE", "gameId=#{id}") unless row["lifecycleState"] == "ACTIVE"
  end
  rows
end

def emit_sql(manifest_rows, content_rows, edition)
  lines = []
  lines << "SET NAMES utf8mb4 COLLATE utf8mb4_0900_ai_ci;"
  lines << "START TRANSACTION;"
  content_rows.each do |row|
    lines << "INSERT INTO aoo_game_release (release_id,game_id,play_version,snapshot_schema_version,card_codec_version,event_interpreter_version,protocol_version,source_hash,release_version,release_scope,catalog_snapshot,rule_snapshot,ui_snapshot,component_snapshot,catalog_hash,rule_hash,ui_hash,component_hash,bundle_hash,status,rollout_percent,created_by,reason,created_at,validated_at,activated_at) VALUES (#{sql_number(row["releaseId"])},#{sql_number(row["gameId"])},#{sql_string(row["playVersion"])},#{sql_string(manifest_rows.find { |m| m["gameId"] == row["gameId"] }["snapshotSchemaVersion"])},#{sql_string(manifest_rows.find { |m| m["gameId"] == row["gameId"] }["cardCodecVersion"])},#{sql_string(manifest_rows.find { |m| m["gameId"] == row["gameId"] }["eventInterpreterVersion"])},#{sql_string(manifest_rows.find { |m| m["gameId"] == row["gameId"] }["protocolVersion"])},#{sql_string(manifest_rows.find { |m| m["gameId"] == row["gameId"] }["sourceHash"])},#{sql_number(row["releaseVersion"])},#{sql_string(row["releaseScope"])},#{sql_json(row["catalogSnapshot"])},#{sql_json(row["ruleSnapshot"])},#{sql_json(row["uiSnapshot"])},#{sql_json(row["componentSnapshot"])},#{sql_string(row["catalogHash"])},#{sql_string(row["ruleHash"])},#{sql_string(row["uiHash"])},#{sql_string(row["componentHash"])},#{sql_string(row["bundleHash"])},'ACTIVE',#{row["rolloutPercent"]},#{sql_number(row["createdBy"])},#{sql_string(row["reason"])},#{sql_time(row["createdAt"])},#{sql_time(row["validatedAt"])},#{sql_time(row["activatedAt"])});"
    lines << "INSERT INTO aoo_game_release_region (release_id,region_code,rollout_percent,status) VALUES (#{sql_number(row["releaseId"])},#{sql_string(row["regionCode"])},#{row["regionRolloutPercent"]},'ACTIVE');"
    lines << "INSERT INTO aoo_compiled_room_create_index (game_id,region_code,play_version,index_generation,release_id,component_chain,rule_validator,ui_schema,lookup_hash,bundle_hash,lifecycle_state,compiled_at,validated_at,activated_at) VALUES (#{sql_number(row["gameId"])},#{sql_string(row["regionCode"])},#{sql_string(row["playVersion"])},#{sql_number(row["indexGeneration"])},#{sql_number(row["releaseId"])},#{sql_json(row["componentChain"])},#{sql_json(row["ruleValidator"])},#{sql_json(row["uiSchema"])},#{sql_string(row["lookupHash"])},#{sql_string(row["bundleHash"])},'ACTIVE',#{sql_time(row["compiledAt"])},#{sql_time(row["validatedAt"])},#{sql_time(row["activatedAt"])});"
    lines << "INSERT INTO aoo_published_game_configuration (game_id,play_version,configuration_payload,created_by,reason,created_at) VALUES (#{sql_number(row["gameId"])},#{sql_string(row["playVersion"])},#{sql_json(row["configurationPayload"])},#{sql_number(row["createdBy"])},#{sql_string(row["reason"])},#{sql_time(row["createdAt"])}) ON DUPLICATE KEY UPDATE configuration_payload=VALUES(configuration_payload),created_by=VALUES(created_by),reason=VALUES(reason),created_at=VALUES(created_at);"
    lines << "UPDATE aoo_game_release SET status='RETIRED',retired_at=#{sql_time(row["activatedAt"])} WHERE game_id=#{sql_number(row["gameId"])} AND play_version=#{sql_string(row["playVersion"])} AND release_id<>#{sql_number(row["releaseId"])} AND status='ACTIVE';"
    lines << "UPDATE aoo_game_release_region rr JOIN aoo_game_release r ON r.release_id=rr.release_id SET rr.status='RETIRED' WHERE r.game_id=#{sql_number(row["gameId"])} AND r.play_version=#{sql_string(row["playVersion"])} AND rr.region_code=#{sql_string(row["regionCode"])} AND rr.release_id<>#{sql_number(row["releaseId"])} AND rr.status='ACTIVE';"
    lines << "UPDATE aoo_compiled_room_create_index SET lifecycle_state='RETIRED',retired_at=#{sql_time(row["activatedAt"])} WHERE game_id=#{sql_number(row["gameId"])} AND region_code=#{sql_string(row["regionCode"])} AND play_version=#{sql_string(row["playVersion"])} AND release_id<>#{sql_number(row["releaseId"])} AND lifecycle_state='ACTIVE';"
    lines << "INSERT INTO aoo_compiled_index_active (game_id,region_code,play_version,index_generation,release_id,cache_epoch,activated_by,activation_reason,activated_at) VALUES (#{sql_number(row["gameId"])},#{sql_string(row["regionCode"])},#{sql_string(row["playVersion"])},#{sql_number(row["indexGeneration"])},#{sql_number(row["releaseId"])},#{sql_number(row["cacheEpoch"])},#{sql_number(row["activatedBy"])},#{sql_string(row["reason"])},#{sql_time(row["activatedAt"])}) ON DUPLICATE KEY UPDATE index_generation=VALUES(index_generation),release_id=VALUES(release_id),cache_epoch=VALUES(cache_epoch),activated_by=VALUES(activated_by),activation_reason=VALUES(activation_reason),activated_at=VALUES(activated_at);"
  end
  lines << "COMMIT;"
  lines.join("\n") + "\n"
end

# Current publication migration is deliberately separate from the historical
# insert/upsert adapter. Current content is an explicit, hash-pinned projection;
# no bootstrap JSON, SQL text, endpoint or guessed parent is accepted as a fallback.
module CurrentVersionMigration
  NATIVE = {
    5 => ['cn298-v1.0.0', 'nn-v1.0.0', 'cn298-niuniu', 'nn-native', 'poker:betting', 'poker:betting'],
    9 => ['cn297-v1.0.0', 'zjh-v1.0.0', 'cn297-zjh', 'zjh-native', 'poker:compare-hand', 'poker:compare-hand'],
    2999 => ['cd299-v1.0.0', 'cx-v1.0.0', 'cd299-native', 'cx-native', 'poker:cd299', 'poker:cx']
  }.freeze
  CONTENT = %w[catalogSnapshot ruleValidator uiSchema componentSnapshot componentChain].freeze
  EXECUTABLE_BINDINGS = {
    5 => [
      [%w[catalogSnapshot providerClass], 'com.aoo.bcg.poker.nn.CN298GameProvider', 'com.aoo.bcg.poker.nn.NnGameProvider'],
      [%w[componentSnapshot providerClass], 'com.aoo.bcg.poker.nn.CN298GameProvider', 'com.aoo.bcg.poker.nn.NnGameProvider'],
      [%w[uiSchema clientRuntimeEntry], 'cn298', 'nn'],
      [%w[componentSnapshot providerVersion], 'cn298-v1.0.0', 'nn-v1.0.0'],
      [['componentChain', 0], 'com.aoo.bcg.poker.nn.CN298PokerFamilyProviderFactory@cn298-v1.0.0', 'com.aoo.bcg.poker.nn.NnPokerFamilyProviderFactory@nn-v1.0.0']
    ],
    9 => [
      [%w[catalogSnapshot providerClass], 'business.global.pk.zjh.ZJHGameProvider', 'business.global.pk.zjh.ZjhGameProvider'],
      [%w[componentSnapshot providerClass], 'business.global.pk.zjh.ZJHGameProvider', 'business.global.pk.zjh.ZjhGameProvider'],
      [%w[uiSchema clientRuntimeEntry], 'cn297-zjh', 'zjh'],
      [%w[componentSnapshot providerVersion], 'cn297-v1.0.0', 'zjh-v1.0.0'],
      [['componentChain', 0], 'business.global.pk.zjh.ZJHPokerFamilyProviderFactory@cn297-v1.0.0', 'business.global.pk.zjh.ZjhPokerFamilyProviderFactory@zjh-v1.0.0']
    ],
    2999 => [
      [%w[catalogSnapshot providerClass], 'com.aoo.bcg.poker.cd299.Cd299GameProvider', 'com.aoo.bcg.poker.cx.CxGameProvider'],
      [%w[componentSnapshot providerClass], 'com.aoo.bcg.poker.cd299.Cd299GameProvider', 'com.aoo.bcg.poker.cx.CxGameProvider'],
      [%w[uiSchema clientRuntimeEntry], 'cd299', 'cx'],
      [%w[uiSchema playFamily], 'poker:cd299', 'poker:cx'],
      [%w[componentSnapshot providerVersion], 'cd299-v1.0.0', 'cx-v1.0.0'],
      [['componentChain', 0], 'com.aoo.bcg.poker.cd299.Cd299ProviderFactory@cd299-v1.0.0', 'com.aoo.bcg.poker.cx.CxProviderFactory@cx-v1.0.0'],
      [['componentChain', 1], 'com.aoo.bcg.poker.cd299.Cd299GameProvider', 'com.aoo.bcg.poker.cx.CxGameProvider']
    ]
  }.freeze
  def self.execution_projection(content, game_id)
    projected = JSON.parse(JSON.generate(content))
    EXECUTABLE_BINDINGS.fetch(game_id, []).each do |path, before, after|
      parent = path[0...-1].reduce(projected) { |value, key| value.fetch(key) }
      check(parent.fetch(path.last) == before, 'EXECUTABLE_BINDING_PREIMAGE')
      parent[path.last] = after
    end
    projected
  rescue KeyError, IndexError, TypeError
    fail!('CURRENT_VERSION_CAS_EXECUTABLE_BINDING_SHAPE')
  end
  def self.check(value, label)
    fail!("CURRENT_VERSION_CAS_#{label}") unless value
  end
  def self.identity_projection(value, changes)
    case value
    when Hash
      value.to_h do |key, item|
        replacement = changes[key]
        [key, replacement && item == replacement[0] ? replacement[1] : identity_projection(item, changes)]
      end
    when Array then value.map { |item| identity_projection(item, changes) }
    else value
    end
  end
  def self.validate!(contract, manifest)
    check(contract['kind'] == 'CURRENT32_VERSION_CAS_V1', 'KIND')
    check(contract['edition'] == 'current32-active-naming-r12', 'EDITION')
    rows = contract.fetch('targets')
    check(rows.is_a?(Array) && rows.length == 32, 'COUNT')
    check(rows.map { |t| t.fetch('old').values_at('gameId', 'regionCode') }.uniq.length == 32, 'DUPLICATE')
    check(rows.map { |t| t.fetch('new').fetch('releaseId') }.uniq.length == 32, 'RELEASE_DUPLICATE')
    check(rows.map { |t| t.fetch('new').fetch('indexGeneration') }.uniq.length == 32, 'INDEX_DUPLICATE')
    check(manifest.map { |m| m.values_at('gameId', 'regionCode') }.sort == rows.map { |t| t.fetch('old').values_at('gameId', 'regionCode') }.sort, 'IDENTITY_SET')
    rows.each do |t|
      old = t.fetch('old'); new = t.fetch('new')
      %w[gameId releaseId indexGeneration cacheEpoch].each { |k| check(old[k].is_a?(Integer) && old[k] > 0, 'OLD_NUMBER') }
      %w[releaseId indexGeneration releaseVersion createdBy activatedBy].each { |k| check(new[k].is_a?(Integer) && new[k] > 0, 'NEW_NUMBER') }
      check(new['gameId'] == old['gameId'] && new['regionCode'] == old['regionCode'] && new['gameCode'] == old['gameCode'], 'BUSINESS_IDENTITY')
      check(new['cacheEpoch'] == old['cacheEpoch'] + 1, 'EPOCH')
      check(new['releaseId'] != old['releaseId'] && new['indexGeneration'] != old['indexGeneration'], 'LITERAL_REUSE')
      check(%w[rolloutPercent regionRolloutPercent].all? { |k| new[k].is_a?(Numeric) && new[k].finite? && new[k] >= 0 && new[k] <= 100 }, 'ROLLOUT_TYPE')
      check(%w[GLOBAL REGIONAL CANARY].include?(new['releaseScope']), 'RELEASE_SCOPE')
      check(old['sourceHash'].is_a?(String) && old['sourceHash'].match?(HASH) && new['sourceHash'].is_a?(String) && new['sourceHash'].match?(HASH), 'SOURCE_HASH')
      check(new['sourceHash'] != old['sourceHash'], 'SOURCE_HASH_REUSE')
      expected = manifest.find { |m| m['gameId'] == new['gameId'] && m['regionCode'] == new['regionCode'] }
      check(%w[gameId gameCode playVersion regionCode releaseId indexGeneration sourceHash].all? { |k| expected[k] == new[k] }, 'SIGNED_BINDING')
      check(%w[snapshotSchemaVersion cardCodecVersion eventInterpreterVersion protocolVersion].all? { |k| expected[k] == old[k] }, 'SCHEMA_PRESERVATION')
      native = NATIVE[old['gameId']]
      check(old['playVersion'] == (native ? native[0] : new['playVersion']) && (!native || new['playVersion'] == native[1]), 'VERSION')
      changes = {'playVersion' => [old['playVersion'],new['playVersion']], 'sourceHash' => [old['sourceHash'],new['sourceHash']], 'roomRuleSourceHash' => [old['sourceHash'],new['sourceHash']]}
      if native
        check(t['catalog'] == {'oldProvider'=>native[2], 'newProvider'=>native[3], 'oldFamily'=>native[4], 'newFamily'=>native[5], 'rowVersion'=>1}, 'CATALOG')
        changes['providerKey'] = native.values_at(2,3)
        changes['family'] = native.values_at(4,5)
        changes['familyCode'] = native.values_at(4,5)
        changes['componentVersion'] = ['CN297-table-v1','zjh-table-v1'] if old['gameId'] == 9
        check(old['parentContentHash'] == old['sourceHash'] && old['parentRowVersion'] == 1, 'PARENT_PROOF')
        check(t['routeCount'].is_a?(Integer) && t['routeCount'] > 0 && t['routeFingerprint'].is_a?(String) && t['routeFingerprint'].match?(HASH), 'ROUTE_PROOF')
      else
        check(!t.key?('catalog') && !t.key?('routeCount'), 'NON_NATIVE_PARENT_WRITE')
      end
      expected_content = identity_projection(execution_projection(t.fetch('currentContent'), old['gameId']), changes)
      CONTENT.each do |key|
        before = t.fetch('currentContent').fetch(key); after = t.fetch('publication').fetch(key)
        check(after == expected_content.fetch(key), "CONTENT_PRESERVATION_#{key}")
        check(t.fetch('currentCanonicalHashes')[key] == canonical_sha(before), 'CURRENT_CONTENT_DIGEST')
      end
      check(%w[catalogHash ruleHash oldStoredUiHash componentHash bundleHash lookupHash].all? { |k| old[k].is_a?(String) && old[k].match?(HASH) }, 'OLD_STORAGE_COMMITMENTS')
      # Existing stored UI commitments may be legacy aliases or use a different
      # serialization. They are immutable CAS preimages, never recomputed and
      # substituted to make a comparison pass. The current canonical digest is
      # a separate content proof; only the newly inserted release uses it.
      check(t['currentCanonicalUiHash'] == canonical_sha(t['currentContent']['uiSchema']), 'CURRENT_UI_CANONICAL_COMMITMENT')
      check(old['lookupHash'] == Digest::SHA256.hexdigest(old.values_at('gameId','regionCode','playVersion','sourceHash').join('|')), 'CURRENT_LOOKUP_COMMITMENT')
      check(t['publication']['uiSchema']['roomRuleSourceHash'] == new['sourceHash'], 'UI_SOURCE')
      check(t['publication']['ruleValidator'].values_at('gameId','gameCode','playVersion','sourceHash') == new.values_at('gameId','gameCode','playVersion','sourceHash'), 'VALIDATOR_IDENTITY')
      check(new['releaseScope'] == old['releaseScope'] && new['rolloutPercent'] == old['rolloutPercent'] && new['regionRolloutPercent'] == old['regionRolloutPercent'], 'ROLLOUT_PRESERVATION')
    end
    rows
  rescue KeyError
    fail!('CURRENT_VERSION_CAS_MISSING_FIELD')
  end

  def self.sql(contract, manifest, reverse: false)
    rows = validate!(contract, manifest)
    q = ->(v) { sql_string(v) }
    j = ->(v) { "CAST(#{sql_json(v)} AS JSON)" }
    h = ->(v) { "SHA2(CAST(#{v} AS CHAR CHARACTER SET utf8mb4),256)" }
    guard = 'aoo_current_version_cas_guard'
    lines = ["-- Exact current32 CAS; execute without --force; abort/rollback on any error.", "SET NAMES utf8mb4 COLLATE utf8mb4_0900_ai_ci;", "CREATE TEMPORARY TABLE #{guard}(ok INT NOT NULL CHECK(ok=1));", 'START TRANSACTION;']
    rows.each do |t|
      old=t['old']; new=t['new']; from=reverse ? new : old; to=reverse ? old : new
      id=old['gameId']; region=q.call(old['regionCode']); native=NATIVE[id]
      epoch=old['cacheEpoch']+(reverse ? 1 : 0)
      where="game_id=#{id} AND region_code=#{region} AND play_version=#{q.call(from['playVersion'])} AND release_id=#{from['releaseId']} AND index_generation=#{from['indexGeneration']} AND cache_epoch=#{epoch}"
      lines << "INSERT INTO #{guard} SELECT IF(COUNT(*)=1,1,0) FROM aoo_compiled_index_active WHERE #{where} FOR UPDATE;"
      unless reverse
        # The input includes full current public content, but SQL guards bind its
        # storage representation to the exact currently selected release/index.
        s=t['currentContent']; conditions=["r.release_id=#{old['releaseId']}", "r.game_id=#{id}", "r.play_version=#{q.call(old['playVersion'])}", "r.status='ACTIVE'", "r.source_hash=#{q.call(old['sourceHash'])}", "r.release_scope=#{q.call(old['releaseScope'])}", "r.rollout_percent=#{old['rolloutPercent']}", "i.game_id=#{id}", "i.region_code=#{region}", "i.play_version=#{q.call(old['playVersion'])}", "i.release_id=#{old['releaseId']}", "i.index_generation=#{old['indexGeneration']}", "i.lifecycle_state='ACTIVE'"]
        lines << "INSERT INTO #{guard} SELECT IF(COUNT(*)=1,1,0) FROM aoo_game_release_region WHERE release_id=#{old['releaseId']} AND region_code=#{region} AND rollout_percent=#{old['regionRolloutPercent']} AND status='ACTIVE' FOR UPDATE;"
        {'catalog_snapshot'=>'catalogSnapshot','rule_snapshot'=>'ruleValidator','ui_snapshot'=>'uiSchema','component_snapshot'=>'componentSnapshot'}.each { |column,key| conditions << "#{h.call("r.#{column}")}=#{h.call(j.call(s[key]))}" }
        conditions << "#{h.call('i.component_chain')}=#{h.call(j.call(s['componentChain']))}"
        conditions << "#{h.call('i.rule_validator')}=#{h.call(j.call(s['ruleValidator']))}"
        conditions << "#{h.call('i.ui_schema')}=#{h.call(j.call(s['uiSchema']))}"
        {'catalog_hash'=>'catalogHash','rule_hash'=>'ruleHash','ui_hash'=>'oldStoredUiHash','component_hash'=>'componentHash','bundle_hash'=>'bundleHash'}.each { |column,key| conditions << "r.#{column}=#{q.call(old[key])}" }
        conditions << "r.catalog_hash=#{h.call('r.catalog_snapshot')}"
        conditions << "r.rule_hash=#{h.call('i.rule_validator')}"
        conditions << "r.component_hash=#{h.call('i.component_chain')}"
        conditions << "i.bundle_hash=#{q.call(old['bundleHash'])}"
        conditions << "i.bundle_hash=r.bundle_hash"
        stored_bundle="JSON_OBJECT('catalogSnapshot',r.catalog_snapshot,'ruleValidator',i.rule_validator,'uiSchema',i.ui_schema,'componentSnapshot',r.component_snapshot,'componentChain',i.component_chain)"
        conditions << "i.bundle_hash=#{h.call(stored_bundle)}"
        conditions << "i.lookup_hash=#{q.call(old['lookupHash'])}"
        conditions << "i.lookup_hash=SHA2(CONCAT(#{id},'|',#{region},'|',#{q.call(old['playVersion'])},'|',#{q.call(old['sourceHash'])}),256)"
        lines << "INSERT INTO #{guard} SELECT IF(COUNT(*)=1,1,0) FROM aoo_game_release r JOIN aoo_compiled_room_create_index i ON i.release_id=r.release_id AND i.game_id=r.game_id WHERE #{conditions.join(' AND ')} FOR UPDATE;"
      end
      if native
        c=t['catalog']; rv=c['rowVersion']+(reverse ? 1 : 0)
        provider=reverse ? c['newProvider'] : c['oldProvider']; family=reverse ? c['newFamily'] : c['oldFamily']
        lines << "INSERT INTO #{guard} SELECT IF(COUNT(*)=1,1,0) FROM aoo_game_catalog WHERE game_id=#{id} AND provider_key=#{q.call(provider)} AND family_code=#{q.call(family)} AND row_version=#{rv} AND status='ACTIVE' FOR UPDATE;"
        if !reverse && id == 2999
          lines << "INSERT INTO #{guard} SELECT IF(COUNT(*)=0,1,0) FROM aoo_game_family WHERE family_code='poker:cx';"
          lines << "INSERT INTO aoo_game_family(family_code,category_code,display_name,status,created_at) SELECT 'poker:cx',category_code,display_name,status,CURRENT_TIMESTAMP(3) FROM aoo_game_family WHERE family_code='poker:cd299' AND status='ACTIVE';"
          lines << "INSERT INTO #{guard} VALUES(ROW_COUNT());"
        end
        unless reverse
          lines << "INSERT INTO #{guard} SELECT IF(COUNT(*)=0,1,0) FROM aoo_play_version WHERE game_id=#{id} AND play_version=#{q.call(new['playVersion'])};"
          lines << "INSERT INTO aoo_play_version(game_id,play_version,default_region_code,rule_schema_version,ui_schema_version,component_schema_version,content_hash,status,compatibility_floor,activated_at,row_version,created_by,created_at,updated_at) SELECT game_id,#{q.call(new['playVersion'])},default_region_code,rule_schema_version,ui_schema_version,component_schema_version,#{q.call(new['sourceHash'])},'ACTIVE',compatibility_floor,CURRENT_TIMESTAMP(3),1,created_by,CURRENT_TIMESTAMP(3),CURRENT_TIMESTAMP(3) FROM aoo_play_version WHERE game_id=#{id} AND play_version=#{q.call(old['playVersion'])} AND content_hash=#{q.call(old['parentContentHash'])} AND row_version=1 AND status='ACTIVE';"
          lines << "INSERT INTO #{guard} VALUES(ROW_COUNT());"
        end
        lines << "UPDATE aoo_game_catalog SET provider_key=#{q.call(reverse ? c['oldProvider'] : c['newProvider'])},family_code=#{q.call(reverse ? c['oldFamily'] : c['newFamily'])},row_version=row_version+1,updated_at=CURRENT_TIMESTAMP(3) WHERE game_id=#{id} AND row_version=#{rv};"
        lines << "INSERT INTO #{guard} VALUES(ROW_COUNT());"
        # Endpoint/route ID/priority/status are never rewritten. A deterministic
        # fingerprint binds all rows before changing only their version key.
        lines << "SET @route_fp=SHA2('',256),@route_n=0;"
        lines << "SELECT @route_fp:=SHA2(CONCAT(@route_fp,':',SHA2(CAST(JSON_ARRAY(game_id,#{q.call(old['playVersion'])},route_id,SHA2(endpoint,256),priority,status,created_at) AS CHAR CHARACTER SET utf8mb4),256)),256),@route_n:=@route_n+1 INTO @route_fp,@route_n FROM aoo_game_service_route WHERE game_id=#{id} AND play_version=#{q.call(from['playVersion'])} ORDER BY route_id FOR UPDATE;"
        # This edition's observed route has exactly one row; do not silently
        # generalise a single-row SELECT INTO to multiple rows.
        check(t['routeCount'] == 1, 'ROUTE_SINGLE_ROW_REQUIRED')
        lines << "INSERT INTO #{guard} VALUES(IF(@route_n=1 AND @route_fp=#{q.call(t['routeFingerprint'])},1,0));"
        lines << "UPDATE aoo_game_service_route SET play_version=#{q.call(to['playVersion'])} WHERE game_id=#{id} AND play_version=#{q.call(from['playVersion'])};"
        lines << "INSERT INTO #{guard} VALUES(ROW_COUNT());"
      end
      unless reverse
        p=t['publication']; m=manifest.find { |x| x['gameId']==id && x['regionCode']==old['regionCode'] }
        snapshots=CONTENT.to_h { |key| [key,j.call(p[key])] }
        combined="JSON_OBJECT("+snapshots.flat_map { |key,value| [q.call(key),value] }.join(',')+")"
        bh=h.call(combined); lookup=Digest::SHA256.hexdigest(new.values_at('gameId','regionCode','playVersion','sourceHash').join('|'))
        columns=%w[release_id game_id play_version snapshot_schema_version card_codec_version event_interpreter_version protocol_version source_hash release_version release_scope catalog_snapshot rule_snapshot ui_snapshot component_snapshot catalog_hash rule_hash ui_hash component_hash bundle_hash status rollout_percent created_by reason validated_at activated_at]
        values=[new['releaseId'],id,q.call(new['playVersion']),*%w[snapshotSchemaVersion cardCodecVersion eventInterpreterVersion protocolVersion].map { |k| q.call(m.fetch(k)) },q.call(new['sourceHash']),new['releaseVersion'],q.call(new['releaseScope']),snapshots['catalogSnapshot'],snapshots['ruleValidator'],snapshots['uiSchema'],snapshots['componentSnapshot'],h.call(snapshots['catalogSnapshot']),h.call(snapshots['ruleValidator']),q.call(canonical_sha(p['uiSchema'])),h.call(snapshots['componentChain']),bh,"'ACTIVE'",new['rolloutPercent'],new['createdBy'],q.call(contract['edition']),'CURRENT_TIMESTAMP(3)','CURRENT_TIMESTAMP(3)']
        lines << "INSERT INTO aoo_game_release(#{columns.join(',')}) VALUES(#{values.join(',')});"
        lines << "INSERT INTO aoo_game_release_region(release_id,region_code,rollout_percent,status) VALUES(#{new['releaseId']},#{region},#{new['regionRolloutPercent']},'ACTIVE');"
        lines << "INSERT INTO aoo_compiled_room_create_index(game_id,region_code,play_version,index_generation,release_id,component_chain,rule_validator,ui_schema,lookup_hash,bundle_hash,lifecycle_state,validated_at,activated_at) VALUES(#{id},#{region},#{q.call(new['playVersion'])},#{new['indexGeneration']},#{new['releaseId']},#{snapshots['componentChain']},#{snapshots['ruleValidator']},#{snapshots['uiSchema']},#{q.call(lookup)},#{bh},'ACTIVE',CURRENT_TIMESTAMP(3),CURRENT_TIMESTAMP(3));"
      end
      lines << "UPDATE aoo_game_release SET status='RETIRED',retired_at=CURRENT_TIMESTAMP(3) WHERE release_id=#{from['releaseId']} AND game_id=#{id} AND play_version=#{q.call(from['playVersion'])} AND status='ACTIVE';"
      lines << "INSERT INTO #{guard} VALUES(ROW_COUNT());"
      lines << "UPDATE aoo_game_release_region SET status='RETIRED' WHERE release_id=#{from['releaseId']} AND region_code=#{region} AND status='ACTIVE';"
      lines << "INSERT INTO #{guard} VALUES(ROW_COUNT());"
      lines << "UPDATE aoo_compiled_room_create_index SET lifecycle_state='RETIRED',retired_at=CURRENT_TIMESTAMP(3) WHERE game_id=#{id} AND region_code=#{region} AND play_version=#{q.call(from['playVersion'])} AND index_generation=#{from['indexGeneration']} AND release_id=#{from['releaseId']} AND lifecycle_state='ACTIVE';"
      lines << "INSERT INTO #{guard} VALUES(ROW_COUNT());"
      if reverse
        lines << "UPDATE aoo_game_release SET status='ACTIVE',retired_at=NULL WHERE release_id=#{old['releaseId']} AND status='RETIRED';"
        lines << "INSERT INTO #{guard} VALUES(ROW_COUNT());"
        lines << "UPDATE aoo_game_release_region SET status='ACTIVE' WHERE release_id=#{old['releaseId']} AND region_code=#{region} AND status='RETIRED';"
        lines << "INSERT INTO #{guard} VALUES(ROW_COUNT());"
        lines << "UPDATE aoo_compiled_room_create_index SET lifecycle_state='ACTIVE',retired_at=NULL WHERE game_id=#{id} AND region_code=#{region} AND index_generation=#{old['indexGeneration']} AND release_id=#{old['releaseId']} AND lifecycle_state='RETIRED';"
        lines << "INSERT INTO #{guard} VALUES(ROW_COUNT());"
      end
      lines << "UPDATE aoo_compiled_index_active SET play_version=#{q.call(to['playVersion'])},index_generation=#{to['indexGeneration']},release_id=#{to['releaseId']},cache_epoch=cache_epoch+1,activated_by=#{new['activatedBy']},activation_reason=#{q.call((reverse ? 'rollback-' : '')+contract['edition'])},activated_at=CURRENT_TIMESTAMP(3) WHERE #{where};"
      lines << "INSERT INTO #{guard} VALUES(ROW_COUNT());"
      if native
        lines << "INSERT INTO #{guard} SELECT IF(COUNT(*)=0,1,0) FROM aoo_compiled_index_active WHERE game_id=#{id} AND play_version=#{q.call(from['playVersion'])};"
        lines << "UPDATE aoo_play_version SET status='RETIRED',retired_at=CURRENT_TIMESTAMP(3),row_version=row_version+1,updated_at=CURRENT_TIMESTAMP(3) WHERE game_id=#{id} AND play_version=#{q.call(from['playVersion'])} AND status='ACTIVE';"
        lines << "INSERT INTO #{guard} VALUES(ROW_COUNT());"
        if reverse
          lines << "UPDATE aoo_play_version SET status='ACTIVE',retired_at=NULL,row_version=row_version+1,updated_at=CURRENT_TIMESTAMP(3) WHERE game_id=#{id} AND play_version=#{q.call(to['playVersion'])} AND status='RETIRED';"
          lines << "INSERT INTO #{guard} VALUES(ROW_COUNT());"
        end
      end
    end
    # Published configuration is immutable and not an identity alias table.
    # No INSERT/UPDATE/DELETE against it is emitted in this migration.
    lines << 'COMMIT;'
    lines.join("\n")+"\n"
  end
end

# The local PDK edition is a two-target immutable delta, not the legacy full publication.
if ARGV.include?('--pdk-forward-input')
  require_relative 'issue-pdk-room-resource-metadata'
  forward = {}
  OptionParser.new do |parser|
    %w[pdk-forward-input baseline publications literal-input collision-manifest version-collision-manifest lease manifest manifest-sha256 expected-edition output].each do |key|
      parser.on("--#{key} VALUE") { |value| forward[key] = value }
    end
  end.parse!(ARGV)
  PdkRoomResourceIssuance.require!(forward.keys.sort == %w[pdk-forward-input baseline publications literal-input collision-manifest version-collision-manifest lease manifest manifest-sha256 expected-edition output].sort, 'PDK_PUBLICATION_ARGUMENTS')
  PdkRoomResourceIssuance.require!(forward['expected-edition'] == PdkRoomResourceIssuance::EDITION, 'PDK_PUBLICATION_EDITION')
  PdkRoomResourceIssuance.gate!(forward['collision-manifest'], forward['lease'], forward['version-collision-manifest'])
  expected = PdkRoomResourceIssuance.project(
    PdkRoomResourceIssuance.read(forward['baseline'], PdkRoomResourceIssuance::BASELINE_SHA),
    PdkRoomResourceIssuance.read(forward['publications'], PdkRoomResourceIssuance::BOOTSTRAP_SHA),
    PdkRoomResourceIssuance.read(forward['literal-input'], PdkRoomResourceIssuance::INPUT_SHA))
  manifest_bytes = File.binread(forward['manifest'])
  manifest_sha = Digest::SHA256.hexdigest(manifest_bytes)
  PdkRoomResourceIssuance.require!(manifest_sha == forward['manifest-sha256'] && JSON.parse(manifest_bytes) == expected['manifest'], 'PDK_PUBLICATION_MANIFEST_DRIFT')
  expected['manifestSHA256'] = manifest_sha
  PdkRoomResourceIssuance.require!(JSON.parse(File.read(forward['pdk-forward-input'])) == expected, 'PDK_PUBLICATION_CONTENT_DRIFT')
  File.write(forward['output'], PdkRoomResourceIssuance.sql(expected), mode: 'wx')
  File.write(forward['output'] + '.rollback.sql', PdkRoomResourceIssuance.sql(expected, reverse: true), mode: 'wx')
  puts JSON.generate(ok: true, edition: PdkRoomResourceIssuance::EDITION, targets: 2, otherRowsWritten: 0, manifestSHA256: manifest_sha)
  exit 0
end

options = {}
OptionParser.new do |parser|
  parser.banner = "Usage: project-active-runtime-publication.rb --manifest PATH --manifest-sha256 HEX --registry PATH --contract-source PATH --content-projection PATH --output PATH --expected-edition EDITION"
  parser.on("--manifest PATH") { |value| options[:manifest] = value }
  parser.on("--manifest-sha256 HEX") { |value| options[:manifest_sha256] = value }
  parser.on("--registry PATH") { |value| options[:registry] = value }
  parser.on("--contract-source PATH") { |value| options[:contract_source] = value }
  parser.on("--content-projection PATH") { |value| options[:content_projection] = value }
  parser.on("--output PATH") { |value| options[:output] = value }
  parser.on("--expected-edition EDITION") { |value| options[:expected_edition] = value }
  parser.on("--authority-contract PATH") { |value| options[:authority_contract] = value }
  parser.on("--authority-contract-sha256 HEX") { |value| options[:authority_contract_sha256] = value }
  parser.on("--version-cas-contract PATH") { |value| options[:version_cas_contract] = value }
  parser.on("--version-cas-contract-sha256 HEX") { |value| options[:version_cas_contract_sha256] = value }
end.parse!(ARGV)

begin
  required = %i[manifest manifest_sha256 registry contract_source content_projection output expected_edition]
  missing = required.reject { |key| options[key].is_a?(String) && !options[key].empty? }
  fail!("RUNTIME_METADATA_PUBLICATION_ARGUMENTS_MISSING", missing.join(",")) unless missing.empty?
  fail!("RUNTIME_METADATA_PUBLICATION_MANIFEST_SHA_INVALID") unless options[:manifest_sha256].match?(HASH)
  fail!("RUNTIME_METADATA_PUBLICATION_EDITION_INVALID") unless options[:expected_edition].match?(EDITION)
  actual_manifest_sha = file_sha(options[:manifest], "manifest")
  fail!("RUNTIME_METADATA_PUBLICATION_MANIFEST_SHA_MISMATCH", "expected=#{options[:manifest_sha256]}|actual=#{actual_manifest_sha}") unless actual_manifest_sha == options[:manifest_sha256]
  manifest = read_json(options[:manifest], "manifest")
  authorized = authorized_manifest_contract!(options[:expected_edition], options[:authority_contract], options[:authority_contract_sha256])
  manifest_rows = validate_manifest!(manifest, actual_manifest_sha, options[:expected_edition], authorized)
  registry = read_json(options[:registry], "registry")
  registry_rows = validate_registry!(registry, manifest_rows)
  registry_sha = file_sha(options[:registry], "registry")
  contract = read_json(options[:contract_source], "contract-source")
  contract_version = validate_contract!(contract, actual_manifest_sha, registry_sha, registry_rows, authorized)
  if options[:version_cas_contract]
    fail!('CURRENT_VERSION_CAS_CONTRACT_SHA') unless file_sha(options[:version_cas_contract], 'version-cas-contract') == options[:version_cas_contract_sha256]
    migration = read_json(options[:version_cas_contract], 'version-cas-contract')
    fail!('CURRENT_VERSION_CAS_INPUT_BINDING') unless migration.values_at('manifestSHA256','registrySHA256','contractSourceSHA256','contentProjectionSHA256') == [actual_manifest_sha,registry_sha,file_sha(options[:contract_source],'contract-source'),file_sha(options[:content_projection],'content-projection')]
    fail!('CURRENT_VERSION_CAS_PROJECTION_BINDING') unless read_json(options[:content_projection], 'content-projection') == migration['targets'].map { |t| t['publication'] }
    content_rows = CurrentVersionMigration.validate!(migration, manifest_rows)
    sql = CurrentVersionMigration.sql(migration, manifest_rows)
    rollback = CurrentVersionMigration.sql(migration, manifest_rows, reverse: true)
    fail!('CURRENT_VERSION_CAS_OUTPUT_EXISTS') if File.exist?(options[:output]) || File.exist?(options[:output]+'.rollback.sql')
    File.write(options[:output]+'.rollback.sql',rollback,mode:'wx')
  else
    content = read_json(options[:content_projection], "content-projection")
    content_rows = validate_content!(content, manifest_rows, actual_manifest_sha, registry_sha, contract_version)
    sql = emit_sql(manifest_rows, content_rows, options[:expected_edition])
  end
  if options[:version_cas_contract]
    File.write(options[:output],sql,mode:'wx')
  else
    File.binwrite(options[:output], sql)
  end
  puts JSON.generate(ok: true, edition: options[:expected_edition], rows: content_rows.length, manifestSha256: actual_manifest_sha, registrySha256: registry_sha, output: File.expand_path(options[:output]))
rescue PublicationError => error
  warn error.message
  exit 1
end
