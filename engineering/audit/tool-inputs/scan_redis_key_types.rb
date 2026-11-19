#!/usr/bin/env ruby
# frozen_string_literal: true
require 'json';require 'open3';require 'time'
urls=ENV.fetch('REDIS_URLS','').split(',').map(&:strip).reject(&:empty?);abort('REDIS_URLS is required') if urls.empty?
observed=Hash.new{|hash,key|hash[key]=[]}
urls.each_with_index do |url,index|
 stdout,stderr,status=Open3.capture3('redis-cli','-u',url,'--scan','--count','1000');abort(stderr) unless status.success?
 stdout.lines.map(&:strip).reject(&:empty?).each do |key|
  type,err,ok=Open3.capture3('redis-cli','-u',url,'TYPE',key);abort(err) unless ok.success?
  observed[key]<<{cluster:index,type:type.strip}
 end
end
conflicts=observed.filter_map{|key,rows|{key:key,observations:rows} if rows.map{|row|row[:type]}.uniq.size>1}
puts JSON.pretty_generate({scannedAt:Time.now.utc.iso8601,clusters:urls.size,keys:observed.size,typeConflicts:conflicts})
