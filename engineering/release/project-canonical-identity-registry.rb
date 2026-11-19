#!/usr/bin/env ruby
# frozen_string_literal: true

# Generates the Batch 2 identity-contract registry from explicit, immutable
# inputs.  It intentionally does not read a database, environment variable,
# checkout snapshot, or client-generated bundle.  A missing current contract
# field is retained as nil and makes the affected row non-consumable. The
# current contract is accepted only from the separately signed contract source;
# historical CompatibilityOrigin metadata is never promoted.

require 'json'
require 'optparse'
require 'digest'
require 'fileutils'

class RegistryError < StandardError; end

begin

options = {
  identity_source: nil,
  runtime_manifest: nil,
  catalog: nil,
  server_root: nil,
  client_root: nil,
  contract_source: nil,
  output: nil
}
OptionParser.new do |parser|
  parser.banner = 'Usage: project-canonical-identity-registry.rb [options]'
  parser.on('--identity-source PATH') { |v| options[:identity_source] = v }
  parser.on('--runtime-manifest PATH') { |v| options[:runtime_manifest] = v }
  parser.on('--catalog PATH') { |v| options[:catalog] = v }
  parser.on('--server-root PATH') { |v| options[:server_root] = v }
  parser.on('--client-root PATH') { |v| options[:client_root] = v }
  parser.on('--contract-source PATH') { |v| options[:contract_source] = v }
  parser.on('--output PATH') { |v| options[:output] = v }
end.parse!

options.each do |name, value|
  raise RegistryError, "#{name}=REQUIRED" if value.nil? || value.empty?
end

def read_file(path)
  raise RegistryError, "SOURCE_MISSING path=#{path}" unless File.file?(path)
  File.binread(path)
end

def sha(path)
  Digest::SHA256.hexdigest(read_file(path))
end

def require_text(value, field)
  text = value.to_s.strip
  raise RegistryError, "#{field}=REQUIRED" if text.empty?
  text
end

def parse_tsv(path)
  lines = read_file(path).force_encoding(Encoding::UTF_8).split("\n", -1)
  lines.pop if lines.last == ''
  raise RegistryError, "CATALOG_HEADER_MISSING path=#{path}" if lines.empty?
  header = lines.shift.split("\t", -1)
  lines.map.with_index(2) do |line, number|
    values = line.split("\t", -1)
    raise RegistryError, "CATALOG_COLUMN_COUNT line=#{number}" unless values.length == header.length
    header.zip(values).to_h
  end
end

def source(path, assertion, role)
  { 'path' => path, 'sha256' => sha(path), 'role' => role, 'assertion' => assertion }
end

identity_doc = JSON.parse(read_file(options[:identity_source]))
runtime_doc = JSON.parse(read_file(options[:runtime_manifest]))
catalog_rows = parse_tsv(options[:catalog])
server_root = options[:server_root]
client_root = options[:client_root]

plays = identity_doc.fetch('plays')
runtime_rows = runtime_doc.fetch('rows')
raise RegistryError, "CURRENT_IDENTITY_COUNT_INVALID count=#{plays.length}" unless plays.length == 32
raise RegistryError, "RUNTIME_MANIFEST_COUNT_INVALID count=#{runtime_rows.length}" unless runtime_rows.length == 32

catalog_by_code = catalog_rows.to_h { |row| [row.fetch('code'), row] }
runtime_by_code = runtime_rows.to_h { |row| [row.fetch('gameCode'), row] }

poker_registry_path = File.join(server_root, 'modules/gameplay/games/poker/src/main/java/com/aoo/bcg/poker/PokerCatalogRuntimeRegistry.java')
pdk_codes_path = File.join(server_root, 'modules/gameplay/games/poker/src/main/java/com/aoo/bcg/poker/PdkBusinessCodes.java')
poker_registry = read_file(poker_registry_path)
pdk_codes_source = read_file(pdk_codes_path)
pdk_codes = pdk_codes_source.scan(/static final String\s+([A-Z0-9_]+)\s*=\s*"([a-z0-9-]+)";/).to_h
pdk_code_by_name = pdk_codes.invert
raise RegistryError, 'PDK_CODE_SOURCE_EMPTY' if pdk_codes.empty?

client_entries_path = File.join(client_root, 'assets/Games/Common/Code/Runtime/GameRuntimeEntries.ts')
pdk_client_codes_path = File.join(client_root, 'assets/Games/Poker/PDK/Common/Code/Regional/PdkBusinessCodes.ts')
pdk_runtime_path = File.join(client_root, 'assets/Games/Poker/PDK/Common/Code/Runtime/PdkRuntimeEntry.ts')
client_entries = read_file(client_entries_path)
pdk_client_codes = read_file(pdk_client_codes_path)
pdk_runtime = read_file(pdk_runtime_path)
contract_source_path = File.join(server_root, 'modules/infrastructure/GameSPI/src/main/java/com/aoo/bcg/gamespi/RoomCreationContext.java')
contract_source = read_file(contract_source_path)
unless contract_source.include?('CONTRACT_VERSION = "compatibility-v1"')
  raise RegistryError, 'CONTRACT_SOURCE_EXPECTED_COMPATIBILITY_MARKER_MISSING'
end

current_contract_doc = JSON.parse(read_file(options[:contract_source]))
raise RegistryError, 'CURRENT_CONTRACT_KIND_INVALID' unless current_contract_doc.fetch('manifestKind') == 'canonical-identity-current-contract'
raise RegistryError, 'CURRENT_CONTRACT_STATUS_INVALID' unless current_contract_doc.fetch('status') == 'SIGNED_CANONICAL_INPUT'
current_contract_version = current_contract_doc.fetch('contractVersion').to_s.strip
raise RegistryError, 'CURRENT_CONTRACT_VERSION_MISSING' if current_contract_version.empty?
raise RegistryError, 'CURRENT_CONTRACT_COMPATIBILITY_VERSION_FORBIDDEN' if current_contract_version == 'compatibility-v1'
raise RegistryError, 'CURRENT_CONTRACT_ISSUER_MISSING' if current_contract_doc.fetch('issuer').to_s.strip.empty?
raise RegistryError, 'CURRENT_CONTRACT_SOURCE_HASH_INVALID' unless current_contract_doc.fetch('sourceHash').match?(/\A[0-9a-f]{64}\z/)
contract_rows = current_contract_doc.fetch('rows')
raise RegistryError, "CURRENT_CONTRACT_COUNT_INVALID count=#{contract_rows.length}" unless contract_rows.length == 32
raise RegistryError, 'CURRENT_CONTRACT_DUPLICATE_GAME_ID' unless contract_rows.map { |row| row.fetch('gameId') }.uniq.length == contract_rows.length
raise RegistryError, 'CURRENT_CONTRACT_DUPLICATE_GAME_CODE' unless contract_rows.map { |row| row.fetch('gameCode') }.uniq.length == contract_rows.length
contract_by_code = contract_rows.to_h { |row| [row.fetch('gameCode'), row] }
contract_evidence = current_contract_doc.fetch('evidence')
project_root = File.expand_path('..', server_root)
contract_evidence.each do |item|
  relative_path = item.fetch('path')
  absolute_path = File.join(project_root, relative_path)
  actual_sha = sha(absolute_path)
  raise RegistryError, "CURRENT_CONTRACT_EVIDENCE_HASH_MISMATCH path=#{relative_path}" unless actual_sha == item.fetch('sha256')
end
digest_input = [
  "contractVersion=#{current_contract_version}",
  "runtimeManifestSha=#{sha(options[:runtime_manifest])}",
  *contract_evidence.map { |item| "#{item.fetch('path')}\0#{item.fetch('sha256')}" }
].join("\n")
raise RegistryError, 'CURRENT_CONTRACT_SOURCE_HASH_MISMATCH' unless Digest::SHA256.hexdigest(digest_input) == current_contract_doc.fetch('sourceHash')
unless current_contract_version == 'common-room-v1'
  raise RegistryError, 'CURRENT_CONTRACT_VERSION_NOT_PROVEN_BY_CURRENT_RUNTIME'
end
game_runtime_contract_path = File.join(project_root, 'Client/assets/Games/Common/Code/Runtime/GameRuntimeEntries.ts')
room_orchestrator_path = File.join(project_root, 'Client/assets/Games/Common/Code/Runtime/RoomOrchestrator.ts')
snapshot_gate_path = File.join(server_root, 'modules/common/CommonRoom/src/main/java/com/aoo/bcg/common/recovery/RoomSnapshotReadGate.java')
unless read_file(game_runtime_contract_path).include?("commonContractVersion: 'common-room-v1'")
  raise RegistryError, 'CURRENT_CONTRACT_CLIENT_LITERAL_MISSING'
end
unless read_file(room_orchestrator_path).include?('commonContractVersion: string')
  raise RegistryError, 'CURRENT_CONTRACT_CLIENT_CONTRACT_SHAPE_MISSING'
end
unless read_file(snapshot_gate_path).include?('SUPPORTED_SNAPSHOT_SCHEMA_VERSION = "room-snapshot-v1"')
  raise RegistryError, 'CURRENT_CONTRACT_SERVER_SNAPSHOT_GATE_MISSING'
end

def resolve_provider(play, server_root, poker_registry, pdk_codes, pdk_code_by_name)
  code = play.fetch('gameCode')
  family = play.fetch('family')
  if family == 'poker:pao-de-kuai'
    const_name = pdk_code_by_name[code]
    raise RegistryError, "PDK_CODE_NOT_PUBLISHED gameCode=#{code}" if const_name.nil?
    if code == 'chengdu2001'
      assertion = 'PdkGameProvider.defaultConfiguration uses regionalRules==null ? native-pdk : region provider; descriptor is current chengdu2001'
      unless poker_registry.include?('regionalRules==null?"native-pdk"') && poker_registry.include?("PdkBusinessCodes.#{const_name}")
        raise RegistryError, "PDK_CHENGDU_PROVIDER_EVIDENCE_MISSING gameCode=#{code}"
      end
      return ['native-pdk', [
        source(File.join(server_root, 'modules/gameplay/games/poker/src/main/java/com/aoo/bcg/poker/PokerCatalogRuntimeRegistry.java'), assertion, 'provider')
      ]]
    end

    special = {
      'neijiang2002' => 'NeijiangPdkRules.java',
      'liangshan2003' => 'LiangshanPdkRules.java',
      'mianning2004' => 'MianningPdkRegionRules.java',
      'anyue2005' => 'AnyuePdkRegionRules.java',
      'panzhihua2006' => 'PanzhihuaPdkRegionRules.java'
    }
    if (file_name = special[code])
      path = File.join(server_root, 'modules/gameplay/games/poker/src/main/java/com/aoo/bcg/poker', file_name)
      text = read_file(path)
      unless text.include?("providerKey()") && text.include?("PdkBusinessCodes.#{const_name}")
        raise RegistryError, "PDK_PROVIDER_EXPRESSION_MISSING gameCode=#{code}"
      end
      return ["native-pdk-#{code}", [source(path, "providerKey returns native-pdk-#{const_name} and #{const_name} is #{code}", 'provider')]]
    end

    legacy_path = File.join(server_root, 'modules/gameplay/games/poker/src/main/java/com/aoo/bcg/poker/LegacyPdkRegionRules.java')
    legacy = read_file(legacy_path)
    unless legacy.include?('providerKey()') && legacy.include?('"native-pdk-" + gameCode()')
      raise RegistryError, "PDK_LEGACY_PROVIDER_EXPRESSION_MISSING gameCode=#{code}"
    end
    unless poker_registry.include?("PdkBusinessCodes.#{const_name}")
      raise RegistryError, "PDK_LEGACY_DISPATCH_MISSING gameCode=#{code}"
    end
    return ["native-pdk-#{code}", [
      source(legacy_path, 'LegacyPdkRegionRules.providerKey returns native-pdk- plus its dispatched gameCode', 'provider'),
      source(poker_registry_path = File.join(server_root, 'modules/gameplay/games/poker/src/main/java/com/aoo/bcg/poker/PokerCatalogRuntimeRegistry.java'), "PdkBusinessCodes::#{const_name} is dispatched to LegacyPdkRegionRules", 'provider')
    ]]
  end

  cases = {
    'quanguo2998' => [
      File.join(server_root, 'modules/gameplay/games/poker/nn/src/main/java/com/aoo/bcg/poker/nn/NnGameProvider.java'),
      'Map.entry("provider", "nn-native")', 'nn-native'
    ],
    'chengdu2999' => [
      File.join(server_root, 'modules/gameplay/games/poker/cx/src/main/java/com/aoo/bcg/poker/cx/CxGameProvider.java'),
      'config.put("provider","cx-native")', 'cx-native'
    ],
    'chengdu1001' => [
      File.join(server_root, 'modules/gameplay/games/mahjong/src/main/java/com/aoo/bcg/mahjong/CatalogMahjongFamilyProvider.java'),
      'out.put("provider","native-mahjong-family")', 'native-mahjong-family'
    ]
  }
  item = cases[code]
  if code == 'quanguo2997'
    zjh_provider_path = File.join(server_root, 'modules/gameplay/games/poker/zjh/src/business/global/pk/zjh/ZjhGameProvider.java')
    zjh_test_path = File.join(server_root, 'modules/gameplay/games/poker/zjh/test/business/global/pk/zjh/ZjhGameProviderTest.java')
    zjh_factory_path = File.join(server_root, 'modules/gameplay/games/poker/zjh/src/business/global/pk/zjh/ZjhPokerFamilyProviderFactory.java')
    spi_provider_path = File.join(server_root, 'modules/infrastructure/GameSPI/src/main/java/com/aoo/bcg/gamespi/GameProvider.java')
    zjh_provider = read_file(zjh_provider_path)
    zjh_test = read_file(zjh_test_path)
    zjh_factory = read_file(zjh_factory_path)
    spi_provider = read_file(spi_provider_path)
    unless zjh_provider.include?('public static final String PROVIDER_KEY = "zjh-native";') &&
           zjh_provider.include?('defaultConfiguration()') &&
           zjh_provider.include?('"provider", PROVIDER_KEY') &&
           zjh_test.include?('assertEquals("zjh-native", ZjhGameProvider.PROVIDER_KEY)') &&
           zjh_test.include?('defaultConfiguration().get("provider")') &&
           !zjh_factory.include?('providerKey') && !zjh_factory.include?('defaultConfiguration') &&
           spi_provider.include?('default Map<String, Object> defaultConfiguration()') &&
           spi_provider.include?('return Map.of("enabled", false, "playVersion", descriptor().version())') &&
           !spi_provider.include?('"provider"')
      raise RegistryError, 'ZJH_PROVIDER_KEY_EVIDENCE_MISSING_OR_CONFLICTING'
    end
    return ['zjh-native', [
      source(zjh_provider_path, 'explicit production ZJH PROVIDER_KEY and defaultConfiguration provider literal', 'provider'),
      source(zjh_test_path, 'focused provider test asserts the exact ZJH provider key/default configuration', 'provider'),
      source(zjh_factory_path, 'ZjhPokerFamilyProviderFactory binds descriptor only and declares no providerKey', 'provider'),
      source(spi_provider_path, 'GameProvider defaultConfiguration has no provider field and is not a ZJH provider key', 'provider')
    ]]
  end
  raise RegistryError, "PROVIDER_SOURCE_NOT_MAPPED gameCode=#{code}" if item.nil?
  path, assertion, value = item
  text = read_file(path)
  unless text.include?(assertion)
    raise RegistryError, "PROVIDER_LITERAL_MISSING gameCode=#{code}"
  end
  [value, [source(path, assertion, 'provider')]]
end

def resolve_client(play, client_root, client_entries, pdk_client_codes, pdk_runtime)
  code = play.fetch('gameCode')
  family = play.fetch('family')
  entries = []
  if family == 'poker:pao-de-kuai'
    unless pdk_client_codes.include?("'#{code}'")
      raise RegistryError, "CLIENT_PDK_CODE_NOT_REGISTERED gameCode=#{code}"
    end
    unless pdk_runtime.include?("id = 'common-pdk'") && pdk_runtime.include?('PDK_BUSINESS_CODE_LIST')
      raise RegistryError, 'CLIENT_PDK_RUNTIME_ENTRY_EVIDENCE_MISSING'
    end
    entries << source(File.join(client_root, 'assets/Games/Poker/PDK/Common/Code/Regional/PdkBusinessCodes.ts'), "PDK_BUSINESS_CODE_LIST contains #{code}", 'clientRuntimeEntry')
    entries << source(File.join(client_root, 'assets/Games/Poker/PDK/Common/Code/Runtime/PdkRuntimeEntry.ts'), "id common-pdk owns PDK_BUSINESS_CODE_LIST", 'clientRuntimeEntry')
    return ['common-pdk', entries]
  end
  mapping = {
    'quanguo2998' => ['nn', 'NnRuntimeEntry.ts', "id = 'nn'"],
    'quanguo2997' => ['zjh', 'ZjhGameRuntimeEntry.ts', "id = 'zjh'"],
    'chengdu2999' => ['cx', 'CxGameRuntimeEntry.ts', "id = 'cx'"],
    'chengdu1001' => ['chengdu1001', 'Chengdu1001RuntimeEntry.ts', "id = 'chengdu1001'"]
  }
  value, file_name, assertion = mapping.fetch(code) { raise RegistryError, "CLIENT_RUNTIME_NOT_MAPPED gameCode=#{code}" }
  path = case code
         when 'quanguo2998' then File.join(client_root, 'assets/Games/Poker/NN/Common/Code', file_name)
         when 'quanguo2997' then File.join(client_root, 'assets/Games/Poker/ZJH/Common/Code', file_name)
         when 'chengdu2999' then File.join(client_root, 'assets/Games/Poker/CX/Code', file_name)
         else File.join(client_root, 'assets/Games/Mahjong/Chengdu1001', file_name)
         end
  text = read_file(path)
  raise RegistryError, "CLIENT_RUNTIME_ID_MISSING gameCode=#{code}" unless text.include?(assertion)
  [value, [source(path, assertion, 'clientRuntimeEntry')]]
end

rows = plays.sort_by { |row| row.fetch('gameId') }.map do |play|
  code = play.fetch('gameCode')
  catalog = catalog_by_code.fetch(code) { raise RegistryError, "CATALOG_IDENTITY_MISSING gameCode=#{code}" }
  runtime = runtime_by_code.fetch(code) { raise RegistryError, "RUNTIME_IDENTITY_MISSING gameCode=#{code}" }
  %w[gameId family playVersion regionCode].each do |field|
    expected = play.fetch(field).to_s
    actual = runtime.fetch(field).to_s
    raise RegistryError, "RUNTIME_IDENTITY_CONFLICT gameCode=#{code} field=#{field}" unless expected == actual
  end
  raise RegistryError, "CATALOG_IDENTITY_CONFLICT gameCode=#{code}" unless catalog.fetch('code') == code && catalog.fetch('version') == play.fetch('playVersion')
  contract = contract_by_code.fetch(code) { raise RegistryError, "CURRENT_CONTRACT_IDENTITY_MISSING gameCode=#{code}" }
  %w[gameId releaseId indexGeneration sourceHash].each do |field|
    expected = runtime.fetch(field).to_s
    actual = contract.fetch(field).to_s
    raise RegistryError, "CURRENT_CONTRACT_RELEASE_CONFLICT gameCode=#{code} field=#{field}" unless expected == actual
  end
  raise RegistryError, "CURRENT_CONTRACT_VERSION_CONFLICT gameCode=#{code}" unless contract.fetch('contractVersion') == current_contract_version

  provider_key, provider_evidence = resolve_provider(play, server_root, poker_registry, pdk_codes, pdk_code_by_name)
  client_entry, client_evidence = resolve_client(play, client_root, client_entries, pdk_client_codes, pdk_runtime)
  failures = []
  failures << 'PROVIDER_KEY_MISSING_FROM_CURRENT_PRODUCTION_CONTRACT' if provider_key.nil?
  contract_version_evidence = [
    source(options[:contract_source], 'signed current release contract source row', 'contractVersion'),
    *contract_evidence.map do |item|
      item.merge('role' => 'contractVersion')
    end
  ]
  {
    'gameId' => play.fetch('gameId'),
    'gameCode' => code,
    'displayName' => catalog.fetch('displayName'),
    'category' => catalog.fetch('category'),
    'family' => play.fetch('family'),
    'regionCode' => play.fetch('regionCode'),
    'provinceCode' => catalog.fetch('provinceCode'),
    'cityCode' => catalog.fetch('cityCode'),
    'playVersion' => play.fetch('playVersion'),
    'snapshotSchemaVersion' => runtime.fetch('snapshotSchemaVersion'),
    'cardCodecVersion' => runtime.fetch('cardCodecVersion'),
    'eventInterpreterVersion' => runtime.fetch('eventInterpreterVersion'),
    'protocolVersion' => runtime.fetch('protocolVersion'),
    'sourceHash' => runtime.fetch('sourceHash'),
    'releaseId' => runtime.fetch('releaseId'),
    'indexGeneration' => runtime.fetch('indexGeneration'),
    'providerKey' => provider_key,
    'clientRuntimeEntry' => client_entry,
    'contractVersion' => contract.fetch('contractVersion'),
    'status' => failures.empty? ? 'CONSUMABLE' : 'FAIL_CLOSED',
    'failureCodes' => failures,
    'evidence' => {
      'providerKey' => provider_evidence,
      'clientRuntimeEntry' => client_evidence,
      'contractVersion' => contract_version_evidence,
      'historicalCompatibilityExclusion' => [source(contract_source_path, 'RoomCreationContext compatibility-v1 is historical-only and never promoted', 'excluded')]
    }
  }
end

required_fields = %w[gameId gameCode family playVersion regionCode providerKey clientRuntimeEntry contractVersion snapshotSchemaVersion cardCodecVersion eventInterpreterVersion protocolVersion sourceHash releaseId indexGeneration]
registry = {
  'manifestKind' => 'canonical-identity-runtime-registry',
  'manifestVersion' => 'batch2-r2',
  'schemaVersion' => 1,
  'registryStatus' => 'SIGNED_CANONICAL_INPUT',
  'consumable' => rows.all? { |row| row.fetch('status') == 'CONSUMABLE' },
  'requiredFields' => required_fields,
  'sourceContract' => {
    'sourceKind' => 'current-identity-source-plus-accepted-release-manifest-plus-production-runtime-evidence',
    'noFallbacks' => true,
    'noGameIdDerivation' => true,
    'projectionBoundary' => %w[serverDescriptor provider lobby gateway client],
    'currentContractSource' => {
      'path' => options[:contract_source],
      'sha256' => sha(options[:contract_source]),
      'issuer' => current_contract_doc.fetch('issuer'),
      'contractVersion' => current_contract_version
    }
  },
  'releaseEdition' => runtime_doc.fetch('releaseEdition'),
  'sourceDigests' => {
    'identitySource' => { 'path' => options[:identity_source], 'sha256' => sha(options[:identity_source]) },
    'releaseManifest' => { 'path' => options[:runtime_manifest], 'sha256' => sha(options[:runtime_manifest]) },
    'serverCatalog' => { 'path' => options[:catalog], 'sha256' => sha(options[:catalog]) },
    'currentContractSource' => { 'path' => options[:contract_source], 'sha256' => sha(options[:contract_source]) }
  },
  'rows' => rows
}

output = options[:output]
FileUtils.mkdir_p(File.dirname(output))
temporary = "#{output}.tmp.#{$$}"
File.binwrite(temporary, JSON.pretty_generate(registry) + "\n")
File.rename(temporary, output)
puts JSON.generate(ok: true, output: output, rows: rows.length,
                   consumable: registry.fetch('consumable'),
                   failClosedRows: rows.count { |row| row.fetch('status') == 'FAIL_CLOSED' },
                   providerKeys: rows.count { |row| !row.fetch('providerKey').nil? },
                   clientRuntimeEntries: rows.count { |row| !row.fetch('clientRuntimeEntry').nil? })
rescue JSON::ParserError => error
  warn "JSON_PARSE_ERROR #{error.message}"
  exit 2
rescue RegistryError => error
  warn "FAIL_CLOSED #{error.message}"
  exit 3
end
