#!/usr/bin/env ruby
# frozen_string_literal: true

# Release-only projection step. Runtime code never executes this file and never
# discovers a checkout path. Every source/value is explicit on the command line.

require "digest"
require "fileutils"
require "json"
require "optparse"
require "tempfile"

EXPECTED_REQUIRED_FIELDS = %w[
  snapshotSchemaVersion
  cardCodecVersion
  eventInterpreterVersion
  protocolVersion
  contractVersion
  sourceHash
  releaseId
  indexGeneration
].freeze
RESOURCE_BASENAME = "runtime-metadata-canonical-manifest.json"
SHA256 = /\A[0-9a-f]{64}\z/.freeze
EDITION = /\A[A-Za-z0-9][A-Za-z0-9._-]{0,63}\z/.freeze
VERSION = EDITION

options = {}
parser = OptionParser.new do |opts|
  opts.banner = "Usage: project-runtime-metadata-classpath-resource.rb --input PATH --output PATH --expected-sha256 HEX --expected-edition EDITION"
  opts.on("--input PATH", "explicit release staging manifest") { |value| options[:input] = value }
  opts.on("--output PATH", "generated target resource") { |value| options[:output] = value }
  opts.on("--expected-sha256 HEX", "expected SHA-256 of exact input bytes") { |value| options[:sha256] = value }
  opts.on("--expected-edition EDITION", "expected release edition") { |value| options[:edition] = value }
end

begin
  parser.parse!(ARGV)
rescue OptionParser::ParseError => error
  abort "RUNTIME_METADATA_PROJECTION_ARGUMENTS_INVALID #{error.message}"
end

missing = %i[input output sha256 edition].reject { |key| options.key?(key) && !options[key].to_s.empty? }
abort "RUNTIME_METADATA_PROJECTION_ARGUMENTS_MISSING #{missing.join(",")}" unless missing.empty?
abort "RUNTIME_METADATA_EXPECTED_SHA_INVALID" unless options[:sha256].match?(SHA256)
abort "RUNTIME_METADATA_EXPECTED_EDITION_INVALID" unless options[:edition].match?(EDITION)

input = File.expand_path(options[:input])
output = File.expand_path(options[:output])
forbidden_fragment = File::SEPARATOR + "Server" + File::SEPARATOR + "tools" + File::SEPARATOR
abort "RUNTIME_METADATA_SERVER_TOOLS_FORBIDDEN" if input.include?(forbidden_fragment)
abort "RUNTIME_METADATA_INPUT_MISSING" unless File.file?(input)
abort "RUNTIME_METADATA_OUTPUT_INVALID" unless File.basename(output) == RESOURCE_BASENAME
target_fragment = File::SEPARATOR + "target" + File::SEPARATOR + "generated-resources" + File::SEPARATOR
abort "RUNTIME_METADATA_OUTPUT_NOT_GENERATED_TARGET" unless output.include?(target_fragment)
abort "RUNTIME_METADATA_OUTPUT_SYMLINK_FORBIDDEN" if File.symlink?(output)

bytes = File.binread(input)
actual_sha = Digest::SHA256.hexdigest(bytes)
abort "RUNTIME_METADATA_SOURCE_SHA_MISMATCH expected=#{options[:sha256]} actual=#{actual_sha}" unless actual_sha == options[:sha256]

begin
  root = JSON.parse(bytes)
rescue JSON::ParserError => error
  abort "RUNTIME_METADATA_MANIFEST_JSON_INVALID #{error.message}"
end
abort "RUNTIME_METADATA_MANIFEST_OBJECT_REQUIRED" unless root.is_a?(Hash)
abort "RUNTIME_METADATA_SCHEMA_INVALID" unless root["schemaVersion"] == 1
abort "RUNTIME_METADATA_KIND_INVALID" unless root["manifestKind"] == "runtime-metadata-canonical"
abort "RUNTIME_METADATA_STATUS_INVALID" unless root["manifestStatus"] == "SIGNED_CANONICAL_INPUT"
abort "RUNTIME_METADATA_NOT_CONSUMABLE" unless root["consumable"] == true
abort "RUNTIME_METADATA_EDITION_MISMATCH expected=#{options[:edition]} actual=#{root["releaseEdition"]}" unless root["releaseEdition"] == options[:edition]
abort "RUNTIME_METADATA_REQUIRED_FIELDS_INVALID" unless root["requiredFields"] == EXPECTED_REQUIRED_FIELDS

rows = root["rows"]
abort "RUNTIME_METADATA_ROWS_INVALID" unless rows.is_a?(Array) && rows.length == 32

seen_ids = {}
seen_codes = {}
seen_versions = {}
seen_releases = {}
seen_generations = {}
seen_identities = {}
rows.each_with_index do |row, index|
  abort "RUNTIME_METADATA_ROW_INVALID index=#{index}" unless row.is_a?(Hash)
  required = %w[gameId gameCode family playVersion regionCode] + EXPECTED_REQUIRED_FIELDS
  missing_fields = required.reject { |field| row.key?(field) }
  abort "RUNTIME_METADATA_ROW_FIELDS_INVALID index=#{index} missing=#{missing_fields.join(",")}" unless missing_fields.empty?

  game_id = row["gameId"]
  game_code = row["gameCode"]
  family = row["family"]
  play_version = row["playVersion"]
  region_code = row["regionCode"]
  abort "RUNTIME_METADATA_GAME_ID_INVALID index=#{index}" unless game_id.is_a?(Integer) && game_id.positive?
  strings = { "gameCode" => game_code, "family" => family, "playVersion" => play_version, "regionCode" => region_code }
  strings.each do |field, value|
    abort "RUNTIME_METADATA_FIELD_INVALID field=#{field} index=#{index}" unless value.is_a?(String) && !value.strip.empty?
  end
  abort "RUNTIME_METADATA_COMPATIBILITY_IDENTITY index=#{index}" if game_id == 516 || play_version.downcase.start_with?("legacy-")
  abort "RUNTIME_METADATA_SOURCE_HASH_INVALID index=#{index}" unless row["sourceHash"].is_a?(String) && row["sourceHash"].match?(SHA256)
  %w[snapshotSchemaVersion cardCodecVersion eventInterpreterVersion protocolVersion contractVersion].each do |field|
    abort "RUNTIME_METADATA_VERSION_INVALID field=#{field} index=#{index}" unless row[field].is_a?(String) && row[field].match?(VERSION)
  end
  %w[releaseId indexGeneration].each do |field|
    abort "RUNTIME_METADATA_NUMBER_INVALID field=#{field} index=#{index}" unless row[field].is_a?(Integer) && row[field].positive?
  end

  abort "RUNTIME_METADATA_DUPLICATE_GAME_ID gameId=#{game_id}" if seen_ids.key?(game_id)
  abort "RUNTIME_METADATA_DUPLICATE_GAME_CODE gameCode=#{game_code}" if seen_codes.key?(game_code)
  abort "RUNTIME_METADATA_DUPLICATE_PLAY_VERSION playVersion=#{play_version}" if seen_versions.key?(play_version)
  release_id = row["releaseId"]
  generation = row["indexGeneration"]
  abort "RUNTIME_METADATA_DUPLICATE_RELEASE_ID releaseId=#{release_id}" if seen_releases.key?(release_id)
  abort "RUNTIME_METADATA_DUPLICATE_INDEX_GENERATION indexGeneration=#{generation}" if seen_generations.key?(generation)
  identity = [game_id, game_code, family, play_version, region_code]
  abort "RUNTIME_METADATA_DUPLICATE_IDENTITY gameId=#{game_id}" if seen_identities.key?(identity)
  seen_ids[game_id] = true
  seen_codes[game_code] = true
  seen_versions[play_version] = true
  seen_releases[release_id] = true
  seen_generations[generation] = true
  seen_identities[identity] = true
end

parent = File.dirname(output)
FileUtils.mkdir_p(parent)
temporary = Tempfile.new(["runtime-metadata-canonical-manifest", ".json"], parent)
begin
  temporary.binmode
  temporary.write(bytes)
  temporary.flush
  temporary.fsync
  temporary.close
  File.rename(temporary.path, output)
ensure
  temporary.close! if temporary
end

puts JSON.generate(
  "output" => output,
  "manifestSha256" => actual_sha,
  "edition" => root["releaseEdition"],
  "rowCount" => rows.length,
  "requiredFields" => EXPECTED_REQUIRED_FIELDS
)
