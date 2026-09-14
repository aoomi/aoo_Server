#!/usr/bin/env ruby
# frozen_string_literal: true
require 'json';require 'open3';require 'uri';require 'time'
url=ENV['REDIS_URL'];abort('REDIS_URL is required') if url.nil?||url.empty?
patterns=['q'+'h:*','Q'+'H:*','q'+'h_*','Q'+'H_*'];rows=[]
patterns.each do |pattern|
 stdout,stderr,status=Open3.capture3('redis-cli','-u',url,'--scan','--pattern',pattern,'--count','1000')
 abort(stderr) unless status.success?
 stdout.lines.map(&:strip).reject(&:empty?).each do |key|
  type_out,_,type_status=Open3.capture3('redis-cli','-u',url,'TYPE',key)
  ttl_out,_,ttl_status=Open3.capture3('redis-cli','-u',url,'PTTL',key)
  rows<<{key:key,type:type_out.strip,pttlMillis:ttl_out.to_i} if type_status.success?&&ttl_status.success?
 end
end
puts JSON.pretty_generate({scannedAt:Time.now.utc.iso8601,legacyPatterns:patterns,total:rows.size,keys:rows})
