#!/usr/bin/env ruby
require 'csv'; require 'digest'; require 'find'; require 'set'
root=File.expand_path('..',__dir__); test='/Users/aoo/Code/Game/BCG/Test'
catalog=File.join(root,'server/Bootstrap/src/main/resources/game-catalog-528.tsv')
output=File.join(root,'work/audit/v07-full-source-crosswalk.tsv')

# Only directories below a server-game project and named src are authority candidates.
source_dirs=Hash.new{|h,k|h[k]=[]}
Find.find(test) do |p|
  if File.directory?(p)
    base=File.basename(p)
    if base=='.git'||%w[target bin node_modules library temp build].include?(base); Find.prune; next end
    if base=='src' && p.match?(%r{/(?:Server_game[^/]*/|server/)([^/]+)/src$}i)
      code=p.match(%r{/(?:Server_game[^/]*/|server/)([^/]+)/src$}i)[1].upcase
      source_dirs[code] << p
      Find.prune
    end
  end
end

registered=Set.new
Dir.glob(File.join(root,'server/**/src/main/resources/META-INF/services/com.aoo.bcg.gamespi.GameProvider')).sort.each do |f|
  File.readlines(f,chomp:true).map(&:strip).reject{|x|x.empty?||x.start_with?('#')}.each{|x|registered<<x}
end
providers={}
registered.each do |klass|
  simple=klass.split('.').last
  file=Dir.glob(File.join(root,"server/**/#{simple}.java")).first
  next unless file
  text=File.read(file)
  code=text[/new GameDescriptor\s*\([^,]+,\s*"([^"]+)"/m,1]
  code ||= text[/super\s*\([^,]+,\s*"([^"]+)"/m,1]
  providers[code.downcase]=[klass,file] if code
end

rows=CSV.read(catalog,headers:true,col_sep:"\t")
CSV.open(output,'w',col_sep:"\t",write_headers:true,headers:%w[gameId code displayName category province city matchBasis nativeProvider providerFile sourceStatus sourceDirs javaFiles protocolClasses criticalFiles criticalSha256 authorityMethods evidenceStatus]) do |out|
 rows.each do |r|
  code=r['code'].downcase; module_code=r['sourceModule'].to_s.upcase; dirs=(source_dirs[module_code]+source_dirs[r['displayName'].to_s.upcase]+source_dirs[code.upcase]).uniq.sort
  java=dirs.flat_map{|d|Dir.glob(File.join(d,'**/*.java'))}.uniq.sort
  critical=java.select{|f|File.basename(f).match?(/(?:RoomSet|SetOp|CalcPosEnd|HuUtil|RoomEnum|SetCard)\.java$/)}
  protocols=java.select{|f|File.basename(f).match?(/^[CS]#{Regexp.escape(module_code)}(?:_|[A-Z]).*\.java$/i)}
  digest=critical.empty? ? '' : Digest::SHA256.hexdigest(critical.map{|f|Digest::SHA256.file(f).hexdigest+"  "+f.sub(test+'/','')}.join("\n"))
  sample=critical.first(12).map{|f|f.sub(test+'/','')}.join(';')
  text=critical.first(30).map{|f|File.read(f,encoding:'UTF-8',invalid: :replace,undef: :replace)}.join("\n")
  methods=[]; {'create/start'=>'(?:startSet|startGame|SetStart)','operate'=>'(?:opCard|doOpType|checkOpType)','settle'=>'(?:calcPosEnd|calcPoint)','restore'=>'(?:getNotify|roomSetInfo|SetInfo)','replay'=>'(?:PlayBack|playBack)'}.each{|name,re|methods<<name if text.match?(/#{re}/i)}
  provider=providers[code]
  source_status=java.empty? ? 'NO_AUTHORITY_SOURCE' : critical.empty? ? 'SOURCE_WITHOUT_CRITICAL_AUTHORITY' : 'AUTHORITY_SOURCE_FOUND'
  evidence=provider ? 'NATIVE_PROVIDER_REGISTERED' : java.empty? ? 'BLOCKED_NO_SOURCE' : (%w[create/start operate settle]-methods).empty? ? 'SOURCE_FOUND_PROVIDER_MISSING' : 'BLOCKED_CRITICAL_METHODS_MISSING'
  basis=["code=#{code}","class/package=#{module_code}","alias=#{r['displayName']}","region=#{[r['provinceCode'],r['cityCode']].map(&:to_s).reject(&:empty?).join('/')}"]
  out<<[r['gameId'],code,r['displayName'],r['category'],r['provinceCode'],r['cityCode'],basis.join(','),provider&.first,provider&&provider[1].sub(root+'/',''),source_status,dirs.map{|d|d.sub(test+'/','')}.join(';'),java.size,protocols.first(20).map{|f|File.basename(f,'.java')}.join(';'),sample,digest,methods.join(','),evidence]
 end
end
abort "expected 528 rows" unless rows.size==528
puts({rows:rows.size,registeredProviders:providers.size,sourceCodes:source_dirs.size,output:output}.inspect)
