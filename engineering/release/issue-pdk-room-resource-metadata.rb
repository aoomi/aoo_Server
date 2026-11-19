#!/usr/bin/env ruby
# frozen_string_literal: true
require 'json'
require 'digest'
require 'optparse'
require 'time'

# One explicitly authorized local release; never allocates IDs or reads a DB.
module PdkRoomResourceIssuance
  EDITION = 'r78-pdk-room-resource-v2'
  ISSUER = '/root/dating_low'
  BASELINE_SHA = 'a453cb986a674f0f6044a581f8e69927a4cfbf94f531567ef021dfaee07a8282'
  BOOTSTRAP_SHA = '56b2ccb0db8149547c7c13399153d38b0b66f81af61104b40286c950ce8ccb92'
  INPUT_SHA = '9177acdff40943ef28359b9fe7e8d11a12e44f0738bada799776e201b08d7125'
  COLLISION_SHA = 'db738796308a1044878333b9211a4ad49f66ce42eb253ccd8d64a07cb7f1d8ee'
  PAIRS = {2001 => [730000001003, 740000001003], 2003 => [730000001004, 740000001004]}.freeze
  ACTIVATION = {2001=>[730000001001,740000001001,4],2003=>[730000001002,740000001002,4]}.freeze
  ROUTE = {'bundle' => 'paodekuai-common', 'scene' => 'GameRoom2D'}.freeze
  def self.require!(ok, code)
    raise ArgumentError, code unless ok
  end
  def self.read(path, sha)
    bytes = File.binread(path)
    require!(Digest::SHA256.hexdigest(bytes) == sha, 'PDK_INPUT_SHA_MISMATCH')
    JSON.parse(bytes)
  end
  def self.project(baseline, publications, input)
    require!(baseline['rows'].is_a?(Array) && baseline['rows'].length == 32, 'PDK_BASELINE_COUNT')
    require!(input['newIssuer'] == ISSUER && input['newEdition'] == EDITION, 'PDK_ISSUER_EDITION')
    require!(input['newReleaseIds'] == PAIRS.to_h { |id, pair| [id.to_s, pair[0]] } &&
             input['newIndexGenerations'] == PAIRS.to_h { |id, pair| [id.to_s, pair[1]] }, 'PDK_LITERAL_IDS')
    require!(input['newReleaseVersions'] == {'2001'=>3,'2003'=>3}, 'PDK_RELEASE_VERSION_LITERAL')
    require!(input['targetRoute'] == ROUTE, 'PDK_RESOURCE_AUTHORITY')
    require!(input['feeTuple'] == {'measureKind'=>'ROUND_COUNT','measureValue'=>8,'playerCount'=>2,
      'payerMode'=>'OWNER','currencyCode'=>'CRYSTAL','costMinor'=>0}, 'PDK_FEE_DRIFT')
    activation = input.fetch('activationExpectedTargets').to_h { |row| [row.fetch('gameId'),row.values_at('releaseId','indexGeneration','cacheEpoch')] }
    require!(activation == ACTIVATION, 'PDK_ACTIVATION_PREIMAGE')
    expected = input.fetch('expectedOldTargets').to_h { |row| [row.fetch('gameId'), row] }
    rows = Marshal.load(Marshal.dump(baseline.fetch('rows')))
    require!(rows.map { |row| row['gameId'] }.uniq.length == 32, 'PDK_DUPLICATE_ID')
    targets = PAIRS.map do |id, pair|
      row = rows.find { |item| item['gameId'] == id }
      old = expected.fetch(id)
      require!(old.all? { |key, value| row[key] == value }, 'PDK_OLD_IDENTITY_DRIFT')
      original = publications.fetch('publications').find { |item| item.dig('metadata','gameId') == id }
      require!(!original.nil?, 'PDK_PUBLICATION_MISSING')
      require!(old.all? { |key, value| !original['metadata'].key?(key) || original['metadata'][key] == value }, 'PDK_PUBLICATION_IDENTITY_DRIFT')
      pub = Marshal.load(Marshal.dump(original))
      require!(pub['uiSchema']['roomRuleSourceHash'] == old['sourceHash'], 'PDK_RULE_HASH_DRIFT')
      require!(!pub['uiSchema'].key?('bundle') && !pub['uiSchema'].key?('scene'), 'PDK_ROUTE_ALREADY_PRESENT')
      pub['uiSchema'].merge!(ROUTE)
      %w[metadata catalog].each do |field|
        pub[field]['releaseId'], pub[field]['indexGeneration'] = pair
      end
      row['releaseId'], row['indexGeneration'] = pair
      row['issuanceReason'] = "Explicit #{ISSUER} #{EDITION} literal resource-route release; rules and source identity retained"
      current = old.merge('releaseId'=>activation.fetch(id)[0],'indexGeneration'=>activation.fetch(id)[1],'cacheEpoch'=>activation.fetch(id)[2])
      {'old' => current, 'publication' => pub}
    end
    require!(rows.reject { |r| PAIRS.key?(r['gameId']) } == baseline['rows'].reject { |r| PAIRS.key?(r['gameId']) }, 'PDK_OTHER30_DRIFT')
    require!(rows.map { |r| r['releaseId'] }.uniq.length == 32 && rows.map { |r| r['indexGeneration'] }.uniq.length == 32, 'PDK_INTERNAL_ID_COLLISION')
    manifest = Marshal.load(Marshal.dump(baseline))
    manifest['releaseIssuer'], manifest['releaseEdition'], manifest['rows'] = ISSUER, EDITION, rows
    {'schemaVersion'=>1,'kind'=>'pdk-room-resource-forward-publication','issuer'=>ISSUER,'edition'=>EDITION,
     'baselineSHA256'=>BASELINE_SHA,'literalInputSHA256'=>INPUT_SHA,'manifest'=>manifest,'targets'=>targets,
     'nonTargetRowsUnchanged'=>true,'feeTuple'=>input['feeTuple']}
  end
  def self.counts(root)
    lines = File.readlines(File.join(root,'stdout.log'), chomp: true)
    footer = JSON.parse(lines.pop)
    require!(footer == {'mysqlExit'=>0,'seedToolExit'=>nil,'stderr'=>'NOT_RETAINED_SENSITIVE_STREAM_BOUNDARY','SQLCaptured'=>false}, 'PDK_PREFLIGHT_FOOTER')
    require!(lines.all? { |line| line.match?(/\A[0-9]+\z/) }, 'PDK_PREFLIGHT_COUNT_FORMAT')
    lines.map { |line| Integer(line,10) }
  end
  def self.gate!(collision_path, lease_path, version_path)
    require!(collision_path == version_path, 'PDK_COMBINED_PREFLIGHT_REQUIRED')
    receipt = read(collision_path, COLLISION_SHA)
    root = File.dirname(collision_path)
    %w[collision.stdout collision.json currentPointer.stdout currentPointer.json HANDOFF.json].each do |name|
      require!(Digest::SHA256.file(File.join(root,name)).hexdigest == receipt.fetch(name), 'PDK_COLLISION_EVIDENCE_DRIFT')
    end
    %w[collision currentPointer].each do |name|
      require!(JSON.parse(File.read(File.join(root,name+'.json')))['exit'] == 0, 'PDK_COLLISION_EXIT')
      lines = File.readlines(File.join(root,name+'.stdout'),chomp:true)
      footer = JSON.parse(lines.pop)
      require!(footer == {'mysqlExit'=>0,'seedToolExit'=>nil,'stderr'=>'NOT_RETAINED_SENSITIVE_STREAM_BOUNDARY','SQLCaptured'=>false}, 'PDK_PREFLIGHT_FOOTER')
      if name == 'collision'
        require!(lines == ['0']*6, 'PDK_COLLISION_NOT_CLEAR')
      else
        expected = ["2001\t730000001001\t740000001001\t4\t2\tca8ffd29cc851946e304a1e942725adebfe661b40323b3dc3614d9707078ac6b",
                    "2003\t730000001002\t740000001002\t4\t2\tc5fe7d0e7078992537ba68c5ae1d96614d4ae0e32822c6dd7a1e0b4e862250c3"]
        require!(lines == expected, 'PDK_CURRENT_POINTER_DRIFT')
      end
    end
    lease = JSON.parse(File.read(lease_path))
    require!(lease['owner'] == ISSUER && lease['state'] == 'HELD' && lease['edition'] == EDITION &&
      lease['pairs'] == PAIRS.transform_keys(&:to_s), 'PDK_EXCLUSIVE_LEASE')
    now = Time.now.utc
    require!(Time.iso8601(lease.fetch('createdAt')) <= now && now < Time.iso8601(lease.fetch('expiresAt')) &&
      Time.iso8601(lease['expiresAt']) - Time.iso8601(lease['createdAt']) <= 7200, 'PDK_LEASE_EXPIRED')
  end
  def self.canonical_ui_json(value)
    case value
    when Hash
      "{" + value.keys.map(&:to_s).sort.map { |key| JSON.generate(key) + ":" + canonical_ui_json(value[key]) }.join(",") + "}"
    when Array
      "[" + value.map { |item| canonical_ui_json(item) }.join(",") + "]"
    else
      JSON.generate(value)
    end
  end
  def self.sql(bundle, reverse: false)
    quote = ->(value) { "'" + value.to_s.gsub("\\") { "\\\\" }.gsub("'") { "''" } + "'" }
    json = ->(value) { "CAST(" + quote.call(JSON.generate(value)) + " AS JSON)" }
    hash = ->(expression) { "SHA2(CAST(#{expression} AS CHAR CHARACTER SET utf8mb4),256)" }
    lines = ["-- MySQL8.4; exact local two-target transaction. Never use mysql --force.",
      "CREATE TEMPORARY TABLE aoo_pdk_resource_cas_guard (ok INT NOT NULL CHECK(ok=1));", "START TRANSACTION;"]
    bundle.fetch('targets').each do |target|
      old = target.fetch('old'); p = target.fetch('publication'); m = p.fetch('metadata')
      from_release, from_index = reverse ? PAIRS.fetch(old['gameId']) : old.values_at('releaseId','indexGeneration')
      to_release, to_index = reverse ? old.values_at('releaseId','indexGeneration') : PAIRS.fetch(old['gameId'])
      where = "game_id=#{old['gameId']} AND region_code=#{quote.call(old['regionCode'])} AND play_version=#{quote.call(old['playVersion'])} AND release_id=#{from_release} AND index_generation=#{from_index} AND cache_epoch=#{old.fetch('cacheEpoch')+(reverse ? 1 : 0)}"
      lines << "INSERT INTO aoo_pdk_resource_cas_guard SELECT IF(COUNT(*)=1,1,0) FROM aoo_compiled_index_active WHERE #{where} FOR UPDATE;"
      unless reverse
        snapshots = %w[catalogSnapshot ruleValidator uiSchema componentSnapshot componentChain].to_h { |key| [key,json.call(p.fetch(key))] }
        combined = "JSON_OBJECT(" + snapshots.flat_map { |key,value| [quote.call(key),value] }.join(',') + ")"
        bundle_hash = hash.call(combined)
        columns = %w[release_id game_id play_version snapshot_schema_version card_codec_version event_interpreter_version protocol_version source_hash release_version release_scope catalog_snapshot rule_snapshot ui_snapshot component_snapshot catalog_hash rule_hash ui_hash component_hash bundle_hash status rollout_percent created_by reason validated_at activated_at]
        values = [to_release,m['gameId'],quote.call(m['playVersion']),quote.call(m['snapshotSchemaVersion']),quote.call(m['cardCodecVersion']),quote.call(m['eventInterpreterVersion']),quote.call(m['protocolVersion']),quote.call(m['sourceHash']),3,quote.call(p['identity'].fetch('releaseScope')),snapshots['catalogSnapshot'],snapshots['ruleValidator'],snapshots['uiSchema'],snapshots['componentSnapshot'],hash.call(snapshots['catalogSnapshot']),hash.call(snapshots['ruleValidator']),quote.call(Digest::SHA256.hexdigest(canonical_ui_json(p.fetch('uiSchema')))),hash.call(snapshots['componentChain']),bundle_hash,"'ACTIVE'",100,1,quote.call(EDITION),'CURRENT_TIMESTAMP(3)','CURRENT_TIMESTAMP(3)']
        lines << "INSERT INTO aoo_game_release (#{columns.join(',')}) VALUES (#{values.join(',')});"
        lines << "INSERT INTO aoo_game_release_region(release_id,region_code,rollout_percent,status) VALUES(#{to_release},#{quote.call(m['regionCode'])},100,'ACTIVE');"
        lookup = Digest::SHA256.hexdigest([m['gameId'],m['regionCode'],m['playVersion'],m['sourceHash']].join('|'))
        lines << "INSERT INTO aoo_compiled_room_create_index(game_id,region_code,play_version,index_generation,release_id,component_chain,rule_validator,ui_schema,lookup_hash,bundle_hash,lifecycle_state,validated_at,activated_at) VALUES(#{m['gameId']},#{quote.call(m['regionCode'])},#{quote.call(m['playVersion'])},#{to_index},#{to_release},#{snapshots['componentChain']},#{snapshots['ruleValidator']},#{snapshots['uiSchema']},#{quote.call(lookup)},#{bundle_hash},'ACTIVE',CURRENT_TIMESTAMP(3),CURRENT_TIMESTAMP(3));"
      end
      lines << "UPDATE aoo_compiled_index_active SET release_id=#{to_release},index_generation=#{to_index},cache_epoch=cache_epoch+1,activation_reason=#{quote.call(reverse ? 'rollback-'+EDITION : EDITION)},activated_at=CURRENT_TIMESTAMP(3) WHERE #{where};"
      lines << "INSERT INTO aoo_pdk_resource_cas_guard VALUES(ROW_COUNT());"
    end
    lines << 'COMMIT;'
    lines.join("\n") + "\n"
  end
  def self.main(argv)
    args = {}
    OptionParser.new do |p|
      %w[baseline publications literal-input collision-manifest version-collision-manifest lease output].each do |key|
        p.on("--#{key} PATH") { |value| args[key] = value }
      end
    end.parse!(argv)
    require!(args.keys.sort == %w[baseline publications literal-input collision-manifest version-collision-manifest lease output].sort, 'PDK_ARGUMENTS_REQUIRED')
    gate!(args['collision-manifest'], args['lease'], args['version-collision-manifest'])
    result = project(read(args['baseline'], BASELINE_SHA), read(args['publications'], BOOTSTRAP_SHA), read(args['literal-input'], INPUT_SHA))
    Dir.mkdir(args['output']) # exclusive output: never overwrite an earlier issuance
    bytes = JSON.pretty_generate(result['manifest']) + "\n"
    File.write(File.join(args['output'],'runtime-metadata-canonical-manifest.json'),bytes,mode:'wx')
    result['manifestSHA256'] = Digest::SHA256.hexdigest(bytes)
    File.write(File.join(args['output'],'publication-content.json'),JSON.pretty_generate(result)+"\n",mode:'wx')
    puts JSON.generate(ok:true,edition:EDITION,manifestSHA256:result['manifestSHA256'],targets:2)
  end
end
PdkRoomResourceIssuance.main(ARGV) if $PROGRAM_NAME == __FILE__
