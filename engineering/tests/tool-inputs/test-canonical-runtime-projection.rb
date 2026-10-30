#!/usr/bin/env ruby
# frozen_string_literal: true

require 'json'
require 'open3'
require 'tempfile'

ROOT = File.expand_path('../../../..', __dir__)
VALIDATOR = File.join(ROOT, 'server/engineering/release/tool-inputs/validate-canonical-runtime-projection.rb')
MANIFEST = File.join(ROOT, 'Server/tools/runtime-metadata-canonical-manifest.json')
REGISTRY = File.join(ROOT, 'server/release-input/identity/canonical-identity-registry.json')

manifest = JSON.parse(File.binread(MANIFEST))
provider_rows = {
  5 => ['cn298-niuniu', 'com.aoo.bcg.poker.nn.CN298PokerFamilyProviderFactory', 'com.aoo.bcg.poker.nn.CN298GameProvider', [
    ['Server/server/Games/Poker/NN/src/main/java/com/aoo/bcg/poker/nn/CN298PokerFamilyProviderFactory.java', 'ba2735ecfa9e8289c429fa3670fd10fd153223e1fcc65f895ce48a843549031d'],
    ['Server/server/Games/Poker/NN/src/main/java/com/aoo/bcg/poker/nn/CN298GameProvider.java', '69f35e4c48ff6554aa9bcb0e256c0317eca2baae04a04efeb67fd6d36bdfcf47']
  ]],
  9 => ['cn297-zjh', 'business.global.pk.zjh.ZJHPokerFamilyProviderFactory', 'business.global.pk.zjh.ZJHGameProvider', [
    ['Server/server/Games/Poker/ZJH/src/business/global/pk/zjh/ZJHPokerFamilyProviderFactory.java', 'd4da358fc01e5a9ad142b1966d8745dc78a1ae47887d28488a310df009277bfd'],
    ['Server/server/Games/Poker/ZJH/src/business/global/pk/zjh/ZJHGameProvider.java', '74d7bccf2bbd2705735e87c953d18fa88ea9afb095756467b99673930e0a9e55']
  ]],
  2999 => ['cd299-native', 'com.aoo.bcg.poker.cd299.Cd299ProviderFactory', 'com.aoo.bcg.poker.cd299.Cd299GameProvider', [
    ['Server/server/Games/Poker/CX/src/main/java/com/aoo/bcg/poker/cd299/Cd299ProviderFactory.java', '026abfd0f72328c40d5f284c9325f1dbb61038a55161afec1d09aea96cd6a55d'],
    ['Server/server/Games/Poker/CX/src/main/java/com/aoo/bcg/poker/cd299/Cd299GameProvider.java', 'b274e0ae872aaff7a9571baf64d5f81f460d5b71e1a9343ccfc3cf64c5c30d22']
  ]],
  1001 => ['native-mahjong-family', 'catalog.chengdu1001', 'com.aoo.bcg.mahjong.CatalogMahjongFamilyProvider', [
    ['Server/database/migrations/V20261006_05__repair_chengdu1001_publication.sql', 'a082ca58cc4d0f7a787ff5de9f660dbf5224761f426f9d9fd306c327ebca8f78'],
    ['Server/server/Games/Mahjong/src/main/java/com/aoo/bcg/mahjong/CatalogMahjongFamilyProvider.java', '909584116a0265d51066be24b347f0399331facab57b02a5644c50eef779d411']
  ]]
}

Tempfile.create(['projection', '.tsv']) do |projection|
  projection.puts %w[gameId gameCode playVersion regionCode releaseId indexGeneration activeReleaseCount activeIndexCount].join("\t")
  manifest['rows'].each do |row|
    projection.puts [row['gameId'], row['gameCode'], row['playVersion'], row['regionCode'], row['releaseId'], row['indexGeneration'], 1, 1].join("\t")
  end
  projection.flush
  original_projection = File.read(projection.path)
  Tempfile.create(['providers', '.json']) do |providers|
    providers.write(JSON.pretty_generate(schemaVersion: 1, status: 'SIGNED_FORMAL_MAPPING', rows: provider_rows.map { |id, values|
      key, factory, implementation, refs = values
      { gameId: id, registryProviderKey: key, providerFactoryKey: factory,
        providerImplementationClass: implementation,
        sourceRefs: refs.map { |path, sha| { path: File.join(ROOT, path), sha256: sha } } }
    }))
    providers.flush
    base = ['ruby', VALIDATOR, '--manifest', MANIFEST, '--registry', REGISTRY,
            '--projection', projection.path, '--provider-contract', providers.path]
    out, err, status = Open3.capture3(*base)
    abort "positive projection validation failed: #{out} #{err}" unless status.success? && JSON.parse(out)['ok'] == true

    broken = original_projection.sub("1107349\t80417\t1\t1", "9999999\t80417\t1\t1")
    projection.rewind
    projection.truncate(0)
    projection.write(broken)
    projection.flush
    _out, broken_err, broken_status = Open3.capture3(*base)
    abort 'pointer mismatch negative case was accepted' if broken_status.success? || !broken_err.include?('PROJECTION_POINTER_MISMATCH')

    projection.rewind
    projection.truncate(0)
    projection.write(original_projection.sub("1107349\t80417\t1\t1", "1107349\t80417\t2\t1"))
    projection.flush
    _out, duplicate_err, duplicate_status = Open3.capture3(*base)
    abort 'active-release uniqueness negative case was accepted' if duplicate_status.success? || !duplicate_err.include?('PROJECTION_ACTIVE_NOT_UNIQUE')

    projection.rewind
    projection.truncate(0)
    projection.write(original_projection)
    projection.flush
    providers_json = JSON.parse(File.read(providers.path))
    providers_json['rows'].find { |r| r['gameId'] == 2999 }['providerImplementationClass'] = 'com.example.WrongProvider'
    providers.rewind
    providers.truncate(0)
    providers.write(JSON.pretty_generate(providers_json))
    providers.flush
    _out, provider_err, provider_status = Open3.capture3(*base)
    abort 'provider drift negative case was accepted' if provider_status.success? || !provider_err.include?('PROVIDER_IMPLEMENTATION_MISMATCH')
  end
end

puts JSON.generate(ok: true, positive: '32/32', negative: %w[pointer active_uniqueness provider_contract])
