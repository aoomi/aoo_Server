require 'json';require 'fileutils'
root=File.expand_path('..',__dir__);client=File.expand_path('../Client',root);files=(Dir.glob(File.join(root,'server/**/*.java'))+Dir.glob(File.join(client,'assets/**/*.ts'))).reject{|path|path.include?('/target/')||path.include?('/build/')||path.include?('/test/')};rows=[];findings=[]
files.each do |path|
 lines=File.binread(path).force_encoding('UTF-8').scrub.lines
 lines.each_with_index do |line,index|
  code=line.sub(%r{//.*$},'');next unless code.match?(/\b(?:for|while)\s*\(/)
  window=lines[[index-12,0].max,37].join
  if (match=code.match(/\bfor\s*\(([^;]*);([^;]*);([^)]*)\)/))
   progress=!match[3].strip.empty?||match[2].match?(/(?:\+\+|--|=\s*[^=]|hasNext|hasMoreElements)/)||window.match?(/\b(?:next|remove|poll)\s*\(|\b(?:break|return|throw)\b/);kind='for'
  elsif (match=code.match(/\bwhile\s*\((.*)\)/))
   condition=match[1];variables=condition.scan(/\b[a-zA-Z_$][\w$]*\b/)- %w[true false null hasNext hasMoreElements isEmpty size length]
   progress=condition.match?(/=\s*[^=]|\+\+|--|hasNext|hasMoreElements|read|poll|remove|isInterrupted|isEmpty|size|compareAndSet/)||variables.any?{|name|window.match?(/(?:#{Regexp.escape(name)}\s*(?:\+\+|--|[+\-*\/]?=(?!=))|(?:remove|poll|next)\s*\()/)}||window.match?(/\b(?:break|return|throw|sleep|await|wait|tryLock)\b/);kind='while'
  else
   next
  end
  row={path:path.start_with?(root)?path.sub(root+'/',''):path.sub(client+'/',''),line:index+1,kind:kind,progressEvidence:progress};rows<<row;findings<<row unless progress
 end
end
out=File.join(root,'work/audit/loop02-progress.json');FileUtils.mkdir_p(File.dirname(out));File.write(out,JSON.pretty_generate({task:'LOOP02',status:findings.empty? ? 'passed':'failed',loopCount:rows.size,findings:findings,inventory:rows})+"\n");abort "LOOP02 loops without syntactic progress: #{findings.size}" unless findings.empty?
