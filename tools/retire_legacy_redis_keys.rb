#!/usr/bin/env ruby
# frozen_string_literal: true
require 'json';require 'open3';require 'time';require 'base64';require 'fileutils'
url=ENV['REDIS_URL'];abort('REDIS_URL is required') if url.to_s.empty?;patterns=['q'+'h:*','Q'+'H:*','q'+'h_*','Q'+'H_*'];keys=patterns.flat_map{|pattern|out,err,status=Open3.capture3('redis-cli','-u',url,'--scan','--pattern',pattern,'--count','1000');abort(err)unless status.success?;out.lines.map(&:strip)}.uniq
inventory={scannedAt:Time.now.utc.iso8601,total:keys.size,keys:keys};FileUtils.mkdir_p('work/redis-retirement');File.write('work/redis-retirement/pre-delete-scan.json',JSON.pretty_generate(inventory)+"\n")
if ENV['MODE']=='delete'
 abort('legacy call telemetry must be zero') unless ENV['LEGACY_CALLS']=='0';abort('explicit approval token required') unless ENV['APPROVAL_TOKEN']=='DELETE_LEGACY_KEYS'
 backup=keys.map do|key|dump,err,status=Open3.capture3('redis-cli','-u',url,'--raw','DUMP',key);abort(err)unless status.success?;ttl,_,_=Open3.capture3('redis-cli','-u',url,'PTTL',key);{key:key,dumpBase64:Base64.strict_encode64(dump),pttlMillis:ttl.to_i}end
 File.write('work/redis-retirement/backup.json',JSON.pretty_generate({createdAt:Time.now.utc.iso8601,entries:backup})+"\n");keys.each_slice(500){|batch|_,err,status=Open3.capture3('redis-cli','-u',url,'UNLINK',*batch);abort(err)unless status.success?}
 remaining=patterns.flat_map{|pattern|out,err,status=Open3.capture3('redis-cli','-u',url,'--scan','--pattern',pattern);abort(err)unless status.success?;out.lines.map(&:strip)}.uniq;File.write('work/redis-retirement/post-delete-scan.json',JSON.pretty_generate({verifiedAt:Time.now.utc.iso8601,remaining:remaining})+"\n");abort('legacy keys remain after deletion')unless remaining.empty?
end
puts JSON.generate(inventory)
