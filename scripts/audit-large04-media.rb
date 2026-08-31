#!/usr/bin/env ruby
# frozen_string_literal: true
require 'digest';require 'json';require 'open3';require 'time'
root=File.expand_path('..',__dir__);assets=File.expand_path('../Client/assets',root);extensions=%w[.png .jpg .jpeg .webp .mp3 .wav .ogg .m4a .atlas]
paths=Dir.glob(File.join(assets,'**/*')).select{|path|File.file?(path)&&extensions.include?(File.extname(path).downcase)&&File.size(path)>=512*1024}.sort
rows=paths.map do|path|
 ext=File.extname(path).downcase;width=height=nil;sample=nil
 if ext=='.png';header=File.binread(path,24);width,height=header.byteslice(16,8).unpack('NN') if header.start_with?("\x89PNG".b);end
 if %w[.mp3 .wav .m4a].include?(ext);out,_err,_status=Open3.capture3('afinfo',path);sample=out[/sample rate:\s*([0-9.]+)/i,1]&.to_f;end
 relative=path.delete_prefix(assets+'/');bundle=case relative when %r{\AGames/Mahjong/} then 'mahjong01' when %r{\AGames/Poker/} then 'poker01' when %r{\AClub/} then 'club' when %r{\ALobby/} then 'lobby' when %r{\ALogin/} then 'login' else 'common' end
 {path:relative,bytes:File.size(path),sha256:Digest::SHA256.file(path).hexdigest,type:ext.delete_prefix('.'),width:width,height:height,decodedBytes:width&&height ? width*height*4:nil,sampleRateHz:sample,bundle:bundle,spine:path.include?('/spine/')}
end
duplicates=rows.group_by{|row|row[:sha256]}.values.select{|group|group.length>1}.map{|group|{sha256:group.first[:sha256],bytes:group.first[:bytes],copies:group.map{|row|row[:path]}}}
checks={inventoryPresent:!rows.empty?,dimensionsCollected:rows.reject{|row|%w[mp3 wav ogg m4a atlas].include?(row[:type])}.all?{|row|row[:width]&&row[:height]},audioMetadataCollected:rows.reject{|row|!%w[mp3 wav m4a].include?(row[:type])}.all?{|row|row[:sampleRateHz]},allAssignedToBundle:rows.all?{|row|row[:bundle]},duplicatePayloadsRemoved:duplicates.empty?,oversizeDecodedTexturesAbsent:rows.none?{|row|row[:decodedBytes].to_i>16*1024*1024},compressedDeviceTextureProfiles:false}
report={schemaVersion:1,generatedAt:Time.now.utc.iso8601,task:'LARGE04',passed:checks.values.all?,thresholdBytes:512*1024,budgets:{singleSourceBytes:1024*1024,singleDecodedTextureBytes:16*1024*1024,audioSampleRateHz:48000},assets:rows,duplicates:duplicates,checks:checks,blockers:['identical media remains duplicated between Common and game packs','PNG sources do not declare verified per-device ASTC/ETC2 compression profiles']}
File.write(File.join(root,'docs/generated/large04-media.json'),JSON.pretty_generate(report)+"\n");puts "LARGE04 #{report[:passed] ? 'PASS':'FAIL'} assets=#{rows.length} duplicates=#{duplicates.length}";exit(report[:passed] ? 0:1)
