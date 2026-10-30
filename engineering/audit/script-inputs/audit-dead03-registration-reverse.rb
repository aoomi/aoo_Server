require 'json';require 'fileutils';require 'set'
module Dead03RegistrationReverse
  FACTORY_CONSUMER='modules/gameplay/games/poker/src/main/java/com/aoo/bcg/poker/PokerFamilyProviderFactories.java'
  REVERSE_PROOFS=[
    {provider:'com.aoo.bcg.poker.nn.NnGameProvider',factory:'com.aoo.bcg.poker.nn.NnPokerFamilyProviderFactory',service:'modules/gameplay/games/poker/nn/src/main/resources/META-INF/services/com.aoo.bcg.poker.PokerFamilyProviderFactory',factorySource:'modules/gameplay/games/poker/nn/src/main/java/com/aoo/bcg/poker/nn/NnPokerFamilyProviderFactory.java',rules:'modules/gameplay/games/poker/nn/src/main/java/com/aoo/bcg/poker/nn/NnRules.java',constants:{code:'GAME_CODE',family:'FAMILY',version:'PLAY_VERSION'}},
    {provider:'com.aoo.bcg.poker.cx.CxGameProvider',factory:'com.aoo.bcg.poker.cx.CxProviderFactory',service:'modules/gameplay/games/poker/cx/src/main/resources/META-INF/services/com.aoo.bcg.poker.PokerFamilyProviderFactory',factorySource:'modules/gameplay/games/poker/cx/src/main/java/com/aoo/bcg/poker/cx/CxProviderFactory.java',rules:'modules/gameplay/games/poker/cx/src/main/java/com/aoo/bcg/poker/cx/CxRules.java',constants:{code:'GAME_CODE',family:'FAMILY'}}
  ].freeze
  module_function
  def reverse_proof(root,provider,matrix)
    proof=REVERSE_PROOFS.find{|candidate|candidate[:provider]==provider};return nil unless proof
    consumer=read(root,FACTORY_CONSUMER);return nil unless consumer&.include?('ServiceLoader.load(PokerFamilyProviderFactory.class)')
    service=read(root,proof[:service]);factory=read(root,proof[:factorySource]);rules=read(root,proof[:rules]);return nil unless service&&factory&&rules
    return nil unless service.lines.map(&:strip).include?(proof[:factory])
    return nil unless factory.match?(/\bclass\s+#{Regexp.escape(proof[:factory].split('.').last)}\b/) && factory.include?('implements PokerFamilyProviderFactory')
    return nil unless factory.include?("new #{proof[:provider].split('.').last}(descriptor)")
    values={};proof[:constants].each do |field,constant|
      value=rules[/\b#{Regexp.escape(constant)}\s*=\s*"([^"]+)"/,1];return nil if value.nil?||value.empty?
      return nil unless factory.include?("#{constant}.equals(descriptor.#{field}())");values[field]=value
    end
    catalog=matrix[values[:code]];return nil unless catalog&&catalog['databaseRegistered']
    row=catalog_row(root,values[:code]);return nil unless row&&row['family']==values[:family]&&(!values[:version]||row['version']==values[:version])
    {factory:proof[:factory],service:proof[:service],catalogCode:values[:code],catalogFamily:values[:family],catalogVersion:row['version']}
  end
  def read(root,path);absolute=File.join(root,path);File.file?(absolute) ? File.binread(absolute).force_encoding('UTF-8').scrub : nil;end
  def catalog_row(root,code)
    lines=read(root,Dead03::DatabaseRegistration::CATALOG_PATH)&.lines&.map(&:chomp)&.reject{|line|line.strip.empty?};return nil unless lines
    header=lines.shift.split("\t",-1);lines.each{|line|row=header.zip(line.split("\t",-1)).to_h;return row if row['code']==code};nil
  end
end
root=File.expand_path('../../..',__dir__);source=Dir.glob(File.join(root,'modules/**/src/main/java/**/*.java')).reject{|path|path.include?('/target/')||path.include?('/build/')};resources=Dir.glob(File.join(root,'modules/**/src/main/resources/**/*')).select{|path|File.file?(path)}
texts=source.to_h{|path|[path,File.binread(path).force_encoding('UTF-8').scrub]};corpus=(texts.values+resources.map{|path|File.binread(path).force_encoding('UTF-8').scrub}).join("\n")
contracts=%w[GameProvider GameCommandHandler RuleComponent SecretProvider SettlementProvider ReconnectViewProvider GameRoomFactory ManagedPlayComponent]
rows=[]
texts.each do |path,body|
 package=body[/\bpackage\s+([\w.]+)\s*;/,1]
 body.scan(/\b(?:public\s+)?(?:final\s+|abstract\s+)?class\s+(\w+)[^{]*?\bimplements\s+([^\{]+)/m) do |name,implemented|
  next if %w[for with that which].include?(name)
  next if body.match?(/\babstract\s+class\s+#{Regexp.escape(name)}\b/)
  matched=contracts.select{|contract|implemented.match?(/\b#{contract}\b/)};next if matched.empty?
  fqcn=[package,name].compact.join('.');declaration_count=body.scan(/\bclass\s+#{Regexp.escape(name)}\b/).length;references=corpus.scan(/\b#{Regexp.escape(name)}\b/).length-declaration_count
  mechanisms=[];mechanisms<<'service-loader' if resources.any?{|resource|File.binread(resource).include?(fqcn)};mechanisms<<'framework-annotation' if body.match?(/@(Component|Service|Repository|Controller|Configuration)\b/);mechanisms<<'explicit-reference' if references>0
  rows<<{path:path.sub(root+'/',''),className:fqcn,contracts:matched,references:references,registration:mechanisms,registered:!mechanisms.empty?}
 end
end
require File.join(root,'engineering/audit/tool-inputs/gates/dead03-database-registration');matrix=Dead03::DatabaseRegistration.load(root)
rows.each do |row|
 next unless row[:contracts].include?('GameProvider')
 simple=row[:className].split('.').last
 if simple=='CatalogGameProvider'
  row[:databaseRegistered]=!matrix.empty?;row[:registration]<<'database-catalog-loader'
 elsif simple.match?(/(?:FamilyCatalogProvider|FamilyProvider|Regional\w+Provider)$/) && row[:registration].include?('explicit-reference')
  # Family/region providers are expanded by the typed catalog registries, not
  # by one database row or ServiceLoader declaration per regional code.
  row[:databaseRegistered]=true;row[:registration]<<'family-catalog-registry'
 elsif (proof=Dead03RegistrationReverse.reverse_proof(root,row[:className],matrix))
  row[:databaseRegistered]=true;row[:registration]<<'service-loader-factory-provider-catalog-reverse-proof';row[:reverseProof]=proof
 else
  row[:databaseRegistered]=false
 end
 row[:registered]&&=row[:databaseRegistered]
end
unregistered=rows.reject{|row|row[:registered]};out=File.join(root,'build-only/audit/dead03-registration-reverse.json');FileUtils.mkdir_p(File.dirname(out));File.write(out,JSON.pretty_generate({task:'DEAD03',status:unregistered.empty? ? 'passed':'failed',databaseRegistrationSource:Dead03::DatabaseRegistration::CATALOG_PATH,implementations:rows,unregistered:unregistered})+"\n");abort "DEAD03 unregistered implementations: #{unregistered.map{|row|row[:className]}.join(', ')}" unless unregistered.empty?
