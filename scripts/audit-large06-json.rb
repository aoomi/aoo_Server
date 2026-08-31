#!/usr/bin/env ruby
# frozen_string_literal: true
require 'digest';require 'json';require 'stringio';require 'time';require 'zlib'
root=File.expand_path('..',__dir__);client=File.expand_path('../Client',root);paths=(Dir.glob(File.join(root,'server/**/*.json'))+Dir.glob(File.join(client,'assets/**/*.json'))).select{|path|File.file?(path)&&File.size(path)>=1024*1024}.sort
def depth(value);case value;when Hash then 1+(value.values.map{|item|depth(item)}.max||0);when Array then 1+(value.map{|item|depth(item)}.max||0);else 0 end end
rows=paths.map do|path|
 bytes=File.binread(path);started=Process.clock_gettime(Process::CLOCK_MONOTONIC);data=JSON.parse(bytes);elapsed=(Process.clock_gettime(Process::CLOCK_MONOTONIC)-started)*1000;buffer=StringIO.new;gz=Zlib::GzipWriter.new(buffer);gz.write(bytes);gz.close
 {path:path.start_with?(root) ? path.delete_prefix(root+'/') : path,bytes:bytes.bytesize,gzipBytes:buffer.string.bytesize,gzipRatio:(buffer.string.bytesize.to_f/bytes.bytesize).round(4),sha256:Digest::SHA256.hexdigest(bytes),parseMilliseconds:elapsed.round(3),rootType:data.class.name,topLevelItems:data.respond_to?(:length) ? data.length : nil,maxDepth:depth(data),schema:false}
rescue JSON::ParserError=>error
 {path:path,bytes:File.size(path),parseError:error.message,schema:false}
end
duplicates=rows.group_by{|row|row[:sha256]}.values.select{|group|group.length>1}.map{|group|group.map{|row|row[:path]}}
checks={allParse:rows.none?{|row|row[:parseError]},schemasPresent:rows.all?{|row|row[:schema]},sourceFilesUnderTwoMiB:rows.all?{|row|row[:bytes]<2*1024*1024},mainThreadParseBudget:rows.all?{|row|row[:parseMilliseconds].to_f<=16.0},compressedTransferPlanned:false,cacheVersioningPlanned:false,noDuplicates:duplicates.empty?}
report={schemaVersion:1,generatedAt:Time.now.utc.iso8601,task:'LARGE06',passed:checks.values.all?,thresholdBytes:1024*1024,budgets:{sourceBytes:2*1024*1024,mainThreadParseMilliseconds:16,maxDepth:32},json:rows,duplicates:duplicates,checks:checks,blockers:['GameHelp/gameCreate/roomcost remain monolithic multi-megabyte JSON','no per-document JSON Schema and versioned cache contract','no compressed transfer or worker/incremental parsing proof']}
File.write(File.join(root,'docs/generated/large06-json.json'),JSON.pretty_generate(report)+"\n");puts "LARGE06 #{report[:passed] ? 'PASS':'FAIL'} json=#{rows.length}";exit(report[:passed] ? 0:1)
