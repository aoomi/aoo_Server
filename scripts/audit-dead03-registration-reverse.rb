require 'json';require 'fileutils';require 'set'
root=File.expand_path('..',__dir__);source=Dir.glob(File.join(root,'server/**/src/main/java/**/*.java')).reject{|path|path.include?('/target/')||path.include?('/build/')};resources=Dir.glob(File.join(root,'server/**/src/main/resources/**/*')).select{|path|File.file?(path)}
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
matrix_path=File.join(root,'work/audit/registration-conservation-audit.json');matrix=File.exist?(matrix_path) ? JSON.parse(File.read(matrix_path))['rows'].to_h{|row|[row['code'],row]} : {}
rows.each do |row|
 next unless row[:contracts].include?('GameProvider')
 simple=row[:className].split('.').last
 if simple=='CatalogGameProvider'
  row[:databaseRegistered]=!matrix.empty?;row[:registration]<<'database-catalog-loader'
 elsif simple.match?(/(?:FamilyCatalogProvider|FamilyProvider|Regional\w+Provider)$/) && row[:registration].include?('explicit-reference')
  # Family/region providers are expanded by the typed catalog registries, not
  # by one database row or ServiceLoader declaration per regional code.
  row[:databaseRegistered]=true;row[:registration]<<'family-catalog-registry'
 else
  code=simple.sub(/GameProvider$/,'').downcase;database=matrix[code];row[:databaseRegistered]=!database.nil?&&database['databaseRegistered']
 end
 row[:registered]&&=row[:databaseRegistered]
end
unregistered=rows.reject{|row|row[:registered]};out=File.join(root,'work/audit/dead03-registration-reverse.json');FileUtils.mkdir_p(File.dirname(out));File.write(out,JSON.pretty_generate({task:'DEAD03',status:unregistered.empty? ? 'passed':'failed',implementations:rows,unregistered:unregistered})+"\n");abort "DEAD03 unregistered implementations: #{unregistered.map{|row|row[:className]}.join(', ')}" unless unregistered.empty?
