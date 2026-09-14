#!/usr/bin/env ruby
# frozen_string_literal: true
require 'csv';require 'digest';require 'fileutils'
source_path,provider_path,database_path,output_path=ARGV
abort 'usage: generate_game_classification.rb SOURCE_CLASSIFICATION_TSV PROVIDER_CATALOG_TSV DATABASE_CATALOG_TSV OUTPUT_TSV' unless output_path
def rows(path);CSV.read(path,headers:true,col_sep:"\t").map(&:to_h);end
source=rows(source_path).reject{|r|r['category']=='INFRASTRUCTURE'};provider=rows(provider_path);database=rows(database_path)
canonical=lambda{|code|code.to_s.downcase.gsub('qh','aoo')};source_by=source.to_h{|r|[canonical.call(r['module']||r['code']),r]};provider_by=provider.group_by{|r|canonical.call(r['code'])};database_by=database.group_by{|r|canonical.call(r['code'])};errors=[]
codes=(source_by.keys|provider_by.keys|database_by.keys).sort;result=codes.map do|code|
  s=source_by[code];p=provider_by[code]||[];d=database_by[code]||[];issues=[];issues<<'missing_source' unless s;issues<<'provider_count' unless p.length==1;issues<<'database_count' unless d.length==1
  if s&&p.length==1;issues<<'provider_category' unless s['category']==p[0]['category'];issues<<'provider_family' unless s['family']==p[0]['family'];end
  if p.length==1&&d.length==1;issues<<'database_game_id' unless p[0]['gameId']==d[0]['gameId'];issues<<'database_version' unless p[0]['version']==d[0]['version'];end
  errors<<[code,*issues] unless issues.empty?;base=(p.first||d.first||{});[base['gameId'],code,base['displayName']||s&.fetch('module',code),s&.fetch('category',''),s&.fetch('family',''),base['regionScope'],base['provinceCode'],base['cityCode'],base['version'],base['enabled'],s&.fetch('module','')]
end
FileUtils.mkdir_p(File.dirname(output_path));File.open(output_path,'w:UTF-8'){|f|f.puts %w[gameId code displayName category family regionScope provinceCode cityCode version enabled sourceModule].join("\t");result.each{|r|f.puts r.join("\t")}}
abort "classification mismatch count=#{errors.length}: #{errors.first(20)}" unless errors.empty?&&result.length==528
warn "classification=528 sha256=#{Digest::SHA256.file(output_path).hexdigest}"
