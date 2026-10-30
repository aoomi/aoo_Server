#!/usr/bin/env ruby
# frozen_string_literal: true

require "json"
require "digest"

path = ARGV.fetch(0, File.expand_path("../../../server/Bootstrap/src/main/resources/canonical-pricing-policy.json", __dir__))
source = JSON.parse(File.read(path))
abort "INVALID_PRICING_SOURCE: root" unless source.is_a?(Hash)
abort "INVALID_PRICING_SOURCE: schema" unless source["schemaVersion"] == 1
abort "INVALID_PRICING_SOURCE: hashAlgorithm" unless source["hashAlgorithm"] == "sha256-canonical-v1"
policies = source["policies"]
abort "INVALID_PRICING_SOURCE: policies" unless policies.is_a?(Array) && !policies.empty?

keys = {}
policies.each do |policy|
  required = %w[gameId gameCode playVersion regionCode releaseId indexGeneration runtimeSourceHash
                policyVersion pricingSourceHash playerCount payerMode currencyCode measures]
  abort "INVALID_PRICING_SOURCE: missing fields" unless required.all? { |key| policy.key?(key) }
  key = [policy["gameId"], policy["playVersion"], policy["regionCode"]]
  abort "INVALID_PRICING_SOURCE: duplicate identity #{key.join("/")}" if keys[key]
  keys[key] = true
  %w[runtimeSourceHash pricingSourceHash].each do |field|
    abort "INVALID_PRICING_SOURCE: #{field}" unless policy[field].is_a?(String) && policy[field].match?(/\A[0-9a-f]{64}\z/)
  end
  %w[gameId releaseId indexGeneration policyVersion playerCount].each do |field|
    abort "INVALID_PRICING_SOURCE: #{field}" unless policy[field].is_a?(Integer) && policy[field].positive?
  end
  abort "INVALID_PRICING_SOURCE: measures" unless policy["measures"].is_a?(Array) && !policy["measures"].empty?
  seen = {}
  policy["measures"].each do |measure|
    abort "INVALID_PRICING_SOURCE: measure" unless measure.is_a?(Hash)
    kind = measure["measureKind"]
    value = measure["measureValue"]
    cost = measure["costMinor"]
    abort "INVALID_PRICING_SOURCE: measure kind" unless %w[ROUND_COUNT DURATION_MINUTES].include?(kind)
    abort "INVALID_PRICING_SOURCE: measure value" unless value.is_a?(Integer) && value.positive?
    abort "INVALID_PRICING_SOURCE: measure cost" unless cost.is_a?(Integer) && cost >= 0
    tuple = [kind, value]
    abort "INVALID_PRICING_SOURCE: duplicate measure" if seen[tuple]
    seen[tuple] = true
  end
  lines = [
    "schemaVersion=#{source["schemaVersion"]}",
    "gameId=#{policy["gameId"]}",
    "gameCode=#{policy["gameCode"]}",
    "playVersion=#{policy["playVersion"]}",
    "regionCode=#{policy["regionCode"]}",
    "releaseId=#{policy["releaseId"]}",
    "indexGeneration=#{policy["indexGeneration"]}",
    "runtimeSourceHash=#{policy["runtimeSourceHash"]}",
    "policyVersion=#{policy["policyVersion"]}",
    "playerCount=#{policy["playerCount"]}",
    "payerMode=#{policy["payerMode"].upcase}",
    "currencyCode=#{policy["currencyCode"].upcase}"
  ]
  policy["measures"].sort_by { |measure| [measure["measureKind"], measure["measureValue"]] }.each do |measure|
    lines << "measure=#{measure["measureKind"]}:#{measure["measureValue"]}:#{measure["costMinor"]}"
  end
  actual = Digest::SHA256.hexdigest(lines.join("\n"))
  abort "INVALID_PRICING_SOURCE: pricingSourceHash mismatch" unless actual == policy["pricingSourceHash"]
end

if source.key?("clubCreate")
  club = source["clubCreate"]
  abort "INVALID_CLUB_CREATE: object" unless club.is_a?(Hash)
  abort "INVALID_CLUB_CREATE: currency" unless club["currency"] == "ROOM_CARD"
  %w[eligibilityEnabled chargeEnabled].each do |field|
    abort "INVALID_CLUB_CREATE: boolean" unless club[field] == true || club[field] == false
  end
  %w[minimumRoomCardBalance chargeRoomCardAmount].each do |field|
    abort "INVALID_CLUB_CREATE: amount" unless club[field].is_a?(Integer) && club[field].between?(0, 9_223_372_036_854_775_807)
  end
  abort "INVALID_CLUB_CREATE: version" unless club["policyVersion"].is_a?(Integer) && club["policyVersion"].between?(1, 2_147_483_647)
  lines = ["schemaVersion=1", "action=CLUB_CREATE", "currency=ROOM_CARD"]
  %w[eligibilityEnabled minimumRoomCardBalance chargeEnabled chargeRoomCardAmount policyVersion].each do |field|
    lines << "#{field}=#{club[field]}"
  end
  hash = club["contentHash"]
  abort "INVALID_CLUB_CREATE: contentHash" unless hash.is_a?(String) && hash.match?(/\A[0-9a-f]{64}\z/) && Digest::SHA256.hexdigest(lines.join("\n")) == hash
end

puts "OK canonical pricing policies=#{policies.length} measures=#{policies.sum { |policy| policy["measures"].length }}"
