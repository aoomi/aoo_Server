#!/usr/bin/env ruby
# frozen_string_literal: true

require "base64"
require "digest"
require "json"

abort "usage: #{$PROGRAM_NAME} active.tsv output.sql" unless ARGV.length == 2
input, output = ARGV

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

def quote(value)
  "'#{value.to_s.gsub("'", "''")}'"
end

rows = File.readlines(input, chomp: true).reject(&:empty?).map do |line|
  fields = line.split("\t", -1)
  abort "invalid ACTIVE projection column count" unless fields.length == 23
  game_id, play_code, region, play, release_id, generation, cache_epoch,
    _snapshot_schema, _card_codec, _event_interpreter, _protocol, source_hash,
    _release_version, _release_scope, _rollout_percent, created_by,
    catalog_b64, rule_b64, ui_b64, component_b64, chain_b64, validator_b64, schema_b64 = fields
  documents = [catalog_b64, rule_b64, ui_b64, component_b64, chain_b64, validator_b64, schema_b64].map { |v| JSON.parse(Base64.strict_decode64(v)) }
  abort "compiled and release UI snapshots disagree for #{game_id}" unless documents[2] == documents[6]
  abort "release rule and compiled validator disagree for #{game_id}" unless documents[1] == documents[5]
  abort "invalid source hash for #{game_id}" unless source_hash.match?(/\A[0-9a-f]{64}\z/)
  abort "invalid play code for #{game_id}" unless play_code.match?(/\A[a-z][a-z0-9]{2,31}\z/)
  bundle = {"catalogSnapshot"=>documents[0], "ruleValidator"=>documents[5], "uiSchema"=>documents[6], "componentSnapshot"=>documents[3], "componentChain"=>documents[4]}
  {game_id: Integer(game_id), play_code: play_code, region: region, play: play, old_release: Integer(release_id),
   old_generation: Integer(generation), cache_epoch: Integer(cache_epoch), created_by: Integer(created_by),
   ui_hash: sha(canonical_json(documents[2])), bundle_hash: sha(canonical_json(bundle))}
end

abort "expected exactly 32 unique ACTIVE games" unless rows.length == 32 && rows.map { |r| r[:game_id] }.uniq.length == 32
rows.sort_by! { |r| r[:game_id] }
base_generation = rows.map { |r| r[:old_generation] }.max + 1
rows.each_with_index do |row, index|
  row[:new_release] = row[:game_id] * 1_000_000_000 + 20_261_126_06
  row[:new_generation] = base_generation + index
end
abort "generated identifiers are not unique" unless rows.map { |r| r[:new_release] }.uniq.length == 32 && rows.map { |r| r[:new_generation] }.uniq.length == 32

sql = ["-- Generated forward-only immutable publication.", "SET NAMES utf8mb4 COLLATE utf8mb4_0900_ai_ci;", "CREATE TEMPORARY TABLE ui_hash_forward_guard(ok INT NOT NULL CHECK(ok=1));", "START TRANSACTION;"]
rows.each do |r|
  sql << "INSERT INTO ui_hash_forward_guard SELECT IF(COUNT(*)=1,1,0) FROM aoo_compiled_index_active WHERE game_id=#{r[:game_id]} AND region_code=#{quote(r[:region])} AND play_version=#{quote(r[:play])} AND release_id=#{r[:old_release]} AND index_generation=#{r[:old_generation]} AND cache_epoch=#{r[:cache_epoch]} FOR UPDATE;"
  sql << "INSERT INTO ui_hash_forward_guard SELECT IF(COUNT(*)=0,1,0) FROM aoo_game_release WHERE release_id=#{r[:new_release]};"
  sql << "INSERT INTO ui_hash_forward_guard SELECT IF(COUNT(*)=0,1,0) FROM aoo_compiled_room_create_index WHERE index_generation=#{r[:new_generation]};"
  sql << "INSERT INTO aoo_game_release(release_id,game_id,play_version,snapshot_schema_version,card_codec_version,event_interpreter_version,protocol_version,source_hash,release_version,release_scope,catalog_snapshot,rule_snapshot,ui_snapshot,component_snapshot,catalog_hash,rule_hash,ui_hash,component_hash,bundle_hash,status,rollout_percent,created_by,reason,validated_at,activated_at) SELECT #{r[:new_release]},game_id,play_version,snapshot_schema_version,card_codec_version,event_interpreter_version,protocol_version,source_hash,release_version+1,release_scope,catalog_snapshot,rule_snapshot,ui_snapshot,component_snapshot,catalog_hash,rule_hash,#{quote(r[:ui_hash])},component_hash,#{quote(r[:bundle_hash])},'ACTIVE',rollout_percent,created_by,'Repair canonical UI commitment by forward publication',CURRENT_TIMESTAMP(3),CURRENT_TIMESTAMP(3) FROM aoo_game_release WHERE release_id=#{r[:old_release]} AND game_id=#{r[:game_id]} AND play_version=#{quote(r[:play])} AND status='ACTIVE';"
  sql << "INSERT INTO ui_hash_forward_guard VALUES(ROW_COUNT());"
  sql << "INSERT INTO aoo_game_release_region(release_id,region_code,rollout_percent,status) SELECT #{r[:new_release]},region_code,rollout_percent,'ACTIVE' FROM aoo_game_release_region WHERE release_id=#{r[:old_release]} AND region_code=#{quote(r[:region])} AND status='ACTIVE';"
  sql << "INSERT INTO ui_hash_forward_guard VALUES(ROW_COUNT());"
  sql << "INSERT INTO aoo_compiled_room_create_index(game_id,region_code,play_version,index_generation,release_id,component_chain,rule_validator,ui_schema,lookup_hash,bundle_hash,lifecycle_state,compiled_at,validated_at,activated_at) SELECT game_id,region_code,play_version,#{r[:new_generation]},#{r[:new_release]},component_chain,rule_validator,ui_schema,lookup_hash,#{quote(r[:bundle_hash])},'ACTIVE',CURRENT_TIMESTAMP(3),CURRENT_TIMESTAMP(3),CURRENT_TIMESTAMP(3) FROM aoo_compiled_room_create_index WHERE game_id=#{r[:game_id]} AND region_code=#{quote(r[:region])} AND play_version=#{quote(r[:play])} AND release_id=#{r[:old_release]} AND index_generation=#{r[:old_generation]} AND lifecycle_state='ACTIVE';"
  sql << "INSERT INTO ui_hash_forward_guard VALUES(ROW_COUNT());"
  sql << "UPDATE aoo_game_release SET status='RETIRED',retired_at=CURRENT_TIMESTAMP(3) WHERE release_id=#{r[:old_release]} AND status='ACTIVE';"
  sql << "INSERT INTO ui_hash_forward_guard VALUES(ROW_COUNT());"
  sql << "UPDATE aoo_game_release_region SET status='RETIRED' WHERE release_id=#{r[:old_release]} AND region_code=#{quote(r[:region])} AND status='ACTIVE';"
  sql << "INSERT INTO ui_hash_forward_guard VALUES(ROW_COUNT());"
  sql << "UPDATE aoo_compiled_room_create_index SET lifecycle_state='RETIRED',retired_at=CURRENT_TIMESTAMP(3) WHERE game_id=#{r[:game_id]} AND region_code=#{quote(r[:region])} AND play_version=#{quote(r[:play])} AND release_id=#{r[:old_release]} AND index_generation=#{r[:old_generation]} AND lifecycle_state='ACTIVE';"
  sql << "INSERT INTO ui_hash_forward_guard VALUES(ROW_COUNT());"
  sql << "UPDATE aoo_compiled_index_active SET release_id=#{r[:new_release]},index_generation=#{r[:new_generation]},cache_epoch=cache_epoch+1,activated_by=#{r[:created_by]},activation_reason='Repair canonical UI commitment by forward publication',activated_at=CURRENT_TIMESTAMP(3) WHERE game_id=#{r[:game_id]} AND region_code=#{quote(r[:region])} AND play_version=#{quote(r[:play])} AND release_id=#{r[:old_release]} AND index_generation=#{r[:old_generation]} AND cache_epoch=#{r[:cache_epoch]};"
  sql << "INSERT INTO ui_hash_forward_guard VALUES(ROW_COUNT());"
  sql << "UPDATE aoo_room_card_play_registry SET release_id=#{r[:new_release]},index_generation=#{r[:new_generation]},runtime_source_hash=(SELECT source_hash FROM aoo_game_release WHERE release_id=#{r[:new_release]}) WHERE play_code=#{quote(r[:play_code])} AND game_id=#{r[:game_id]} AND play_version=#{quote(r[:play])} AND release_id=#{r[:old_release]} AND index_generation=#{r[:old_generation]} AND status='ACTIVE';"
  sql << "INSERT INTO ui_hash_forward_guard VALUES(ROW_COUNT());"
  sql << "UPDATE `aoo_room_card_price_#{r[:play_code]}` SET release_id=#{r[:new_release]},index_generation=#{r[:new_generation]},runtime_source_hash=(SELECT source_hash FROM aoo_game_release WHERE release_id=#{r[:new_release]}) WHERE play_code=#{quote(r[:play_code])} AND play_version=#{quote(r[:play])} AND release_id=#{r[:old_release]} AND index_generation=#{r[:old_generation]} AND status='ACTIVE' AND confirmation_status='CONFIRMED' AND amount_minor=0;"
  sql << "INSERT INTO ui_hash_forward_guard SELECT IF(ROW_COUNT()>0,1,0);"
end
sql << "COMMIT;"
File.write(output, sql.join("\n") + "\n", mode: "wx")
