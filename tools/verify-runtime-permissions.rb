#!/usr/bin/env ruby
require 'json'; require 'find'
root=File.expand_path(ARGV.fetch(0)); abort "runtime root missing: #{root}" unless Dir.exist?(root)
violations=[]
%w[config secrets backups logs uploads replays temp].each do |name|
  path=File.join(root,name); next violations<<{"path"=>name,"reason"=>'missing'} unless Dir.exist?(path)
  mode=File.stat(path).mode&0777; violations<<{"path"=>name,"reason"=>'directory-mode',"mode"=>mode.to_s(8)} if (mode&0027)!=0
end
Find.find(root) do |path|
  next if path==root||File.directory?(path)
  relative=path.delete_prefix(root+'/'); mode=File.stat(path).mode&0777
  limit=relative.start_with?('secrets/') ? 0600 : 0640
  violations<<{"path"=>relative,"reason"=>'file-mode',"mode"=>mode.to_s(8),"maximum"=>limit.to_s(8)} if (mode&~limit)!=0
end
puts JSON.pretty_generate({"root"=>root,"passed"=>violations.empty?,"violations"=>violations})
exit(violations.empty? ? 0 : 2)
