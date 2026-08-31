#!/usr/bin/env ruby
require 'json'; require 'fileutils'
root=File.expand_path('..',__dir__);client=File.expand_path('../Client',root);assets=File.join(client,'assets');out=File.join(root,'docs/generated');FileUtils.mkdir_p(out)
patterns={fetch:/\bfetch\s*\(/,websocket:/new\s+WebSocket\s*\(/,localStorage:/(?:globalThis\.)?localStorage|sys\.localStorage/,native:/\b(?:jsb|wx\.)/,bundle:/assetManager\.loadBundle|resources\.load/};hits=Hash.new{|h,k|h[k]=[]}
Dir.glob(File.join(assets,'**/*.ts')).sort.each{|p|File.foreach(p).with_index(1){|l,i|patterns.each{|k,re|hits[k]<<{path:p.delete_prefix(client+'/'),line:i+1} if l.match?(re)}}}
domain=lambda{|prefix|hits.transform_values{|rows|rows.select{|r|r[:path].start_with?("assets/#{prefix}/")}}};emit=lambda{|id,data|File.write(File.join(out,"#{id}.json"),JSON.pretty_generate({schemaVersion:1}.merge(data))+"\n")}
%w[Common Lobby Club Games Login].each_with_index{|d,i|emit.call("cnet0#{i+1}-#{d.downcase}-network",{domain:d,directCalls:domain.call(d),centralized:false,passed:false})}
emit.call('cnet06-storage',{hits:hits[:localStorage],versionedAccountIsolation:false,passed:false});emit.call('cnet07-native-bridge',{hits:hits[:native],singleAdapter:false,passed:false});emit.call('cnet08-resource-loader',{hits:hits[:bundle],referenceCounting:false,cancellation:false,failureRecovery:false,passed:false})
endpoint=[];Dir.glob(File.join(assets,'**/*.{ts,json}')).sort.each{|p|next if File.size(p)>10_000_000;File.foreach(p).with_index(1){|l,i|endpoint<<{path:p.delete_prefix(client+'/'),line:i+1} if l.match?(/ws:\/\/|localhost|127\.0\.0\.1/)}};emit.call('cnet09-dynamic-addresses',{hits:endpoint,passed:endpoint.empty?})
counts=hits.transform_values(&:length);bp=File.join(out,'cnet10-direct-call-baseline.json');if ARGV.include?('--check');b=JSON.parse(File.read(bp))['counts'];g=counts.select{|k,v|v>b.fetch(k.to_s,0)};abort "CNET10 failed: #{g}" unless g.empty?;puts 'CNET10 PASS';exit;end
emit.call('cnet10-direct-call-baseline',{counts:counts,policy:'No direct-call category may grow beyond this baseline.',passed:true});puts "CNET01-CNET10 audited: #{counts}"
