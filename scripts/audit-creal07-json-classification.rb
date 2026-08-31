#!/usr/bin/env ruby
require'json';require'digest';require'fileutils';root=File.expand_path('..',__dir__);client=File.expand_path('../Client',root);files=Dir.glob(File.join(client,'**/*.json')).select{|p|File.file?(p)}.sort
classify=lambda do|rel|
 case rel
 when %r{^(?:library|temp|build)/} then 'GENERATED_CACHE'
 when %r{^(?:settings|profiles)/} then 'EDITOR_PROFILE'
 when %r{^development/(?:configs|schemas)/} then 'CONFIG_SOURCE'
 when %r{^development/} then 'DEVELOPMENT_TEST'
 when %r{^assets/.*/(?:Config|config)/} then 'RUNTIME_CONFIG'
 when %r{^assets/.*/(?:Spine|spine)/} then 'RESOURCE_DATA'
 when %r{^assets/} then 'ASSET_BUSINESS_OR_RESOURCE'
 when %r{^(?:extensions|tools|tests)/} then 'TOOL_OR_TEST'
 when %r{^docs/Archive/} then 'ARCHIVED_REFERENCE'
 when /\A(?:package|tsconfig)\.json\z/ then 'PROJECT_TOOLCHAIN'
 else 'UNCLASSIFIED' end
end
rows=files.map{|p|rel=p.delete_prefix(client+'/');[rel,classify.call(rel),File.size(p),Digest::SHA256.file(p).hexdigest]};tsv=File.join(root,'docs/generated/creal07-json-inventory.tsv');FileUtils.mkdir_p(File.dirname(tsv));File.write(tsv,"path\tclassification\tbytes\tsha256\n"+rows.map{|r|r.join("\t")}.join("\n")+"\n");counts=rows.group_by{|r|r[1]}.transform_values(&:length);report={schemaVersion:1,total:rows.length,classifications:counts,inventorySha256:Digest::SHA256.file(tsv).hexdigest,unclassified:rows.select{|r|r[1]=='UNCLASSIFIED'}.map(&:first)};out=File.join(root,'docs/generated/creal07-json-classification.json');File.write(out,JSON.pretty_generate(report)+"\n");abort("CREAL07 unclassified JSON: #{report[:unclassified].length}")unless report[:unclassified].empty?;puts "CREAL07 PASS: #{rows.length} JSON files classified"
