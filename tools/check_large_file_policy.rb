#!/usr/bin/env ruby
# frozen_string_literal: true
require 'digest';require 'json';require 'pathname';require 'time'
server=Pathname.new(__dir__).parent.realpath;root=server.parent;policy=JSON.parse((server/'config/large-file-policy.json').read);declaration_path=server/policy['declarations']
excluded=policy['excludedSegments'];thresholds=policy['thresholds'];files=[]
policy['scopes'].each do|scope|
 base=root/scope;next unless base.directory?
 Dir.glob((base/'**/*').to_s,File::FNM_DOTMATCH).sort.each do|path|
  next unless File.file?(path);relative=Pathname.new(path).relative_path_from(root).to_s;parts=relative.split('/')
  next if excluded.any?{|segment|segment.include?('/') ? relative.include?("/#{segment}/") : parts.include?(segment)}
  threshold=thresholds.fetch(File.extname(path).downcase,thresholds['default']);next unless File.size(path)>threshold
  files<<{path:relative,sha256:Digest::SHA256.file(path).hexdigest,bytes:File.size(path),thresholdBytes:threshold}
 end
end
def classification(path)
 ext=File.extname(path).downcase
 return ['immutable 2.22 offline database baseline','release-excluded; optional offline compression only','never loaded by production','database-archive'] if ext=='.sql'
 return ['Cocos native UI prefab','Creator serialized source; bundle compression at build','load on owning feature Bundle entry','client-prefabs'] if ext=='.prefab'
 return ['runtime visual or audio asset',%w[.mp3 .ogg .webp].include?(ext) ? 'already encoded; preserve source quality':'Creator device-profile compression required','load on owning feature Bundle entry','client-assets'] if %w[.png .jpg .jpeg .webp .mp3 .wav .ogg .ttf .otf .bin].include?(ext)
 return ['legacy configuration compatibility source','gzip/Brotli at delivery; split migration tracked','versioned configuration cache, never parse repeatedly','configuration-assets'] if ext=='.json'
 ['project audit or source artifact','text compression in artifact transport','developer/build-time only','architecture']
end
if ARGV.include?('--initialize')
 declarations=files.map do|file|purpose,compression,loading,owner=classification(file[:path]);file.merge(maximumBytes:(file[:bytes]*1.10).ceil,purpose:purpose,compression:compression,loadingStrategy:loading,owner:owner)end
 declaration_path.write(JSON.pretty_generate({schemaVersion:1,capturedAt:Time.now.utc.iso8601,declarations:declarations})+"\n")
end
document=declaration_path.file? ? JSON.parse(declaration_path.read):{'declarations'=>[]};declared=document['declarations'].to_h{|entry|[entry['path'],entry]};required=policy['requiredDeclarationFields'];errors=[]
files.each do|file|
 entry=declared[file[:path]];errors<<"undeclared #{file[:path]}" and next unless entry
 errors<<"incomplete declaration #{file[:path]}" unless required.all?{|field|entry[field]&&!entry[field].to_s.empty?}
 errors<<"hash changed #{file[:path]}" unless entry['sha256']==file[:sha256]
 errors<<"size exceeds approved maximum #{file[:path]}" unless file[:bytes]<=entry['maximumBytes'].to_i
end
orphans=declared.keys-files.map{|file|file[:path]};errors.concat(orphans.map{|path|"stale declaration #{path}"})
report={schemaVersion:1,generatedAt:Time.now.utc.iso8601,task:'LARGE10',passed:errors.empty?,policy:'config/large-file-policy.json',largeFileCount:files.length,declaredCount:declared.length,files:files,errors:errors}
(server/'docs/generated/large10-file-gate.json').write(JSON.pretty_generate(report)+"\n");puts "large-file-policy: #{errors.empty? ? 'passed':'failed'} files=#{files.length}";exit(errors.empty? ? 0:1)
