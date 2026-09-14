#!/usr/bin/env ruby
# frozen_string_literal: true
require 'json';require 'csv';require 'fileutils'
frontend_path,backend_path,database_path,output_path=ARGV
abort 'usage: reconcile_game_catalogs.rb FRONTEND_JSON_OR_TSV BACKEND_TSV DATABASE_TSV OUTPUT_JSON' unless output_path
def tsv(path);CSV.read(path,headers:true,col_sep:"\t").map(&:to_h);end
def frontend(path)
  return tsv(path) unless File.extname(path)=='.json'
  raw=JSON.parse(File.read(path));values=raw.is_a?(Hash) ? raw.values : raw
  values.map{|g|{'code'=>g['gameName'].to_s.downcase,'gameId'=>g['id'].to_s,'name'=>g['name']||g['gameName'],'region'=>g['region'],'category'=>g['gameType'].to_s.upcase}}
end
front=frontend(frontend_path);back=tsv(backend_path).map{|r|r.merge('code'=>(r['code']||r['module']).to_s.downcase)};db=tsv(database_path).map{|r|r.merge('code'=>(r['code']||r['module']).to_s.downcase)}
indices={frontend:front.group_by{|r|r['code']},backend:back.group_by{|r|r['code']},database:db.group_by{|r|r['code']}};codes=indices.values.flat_map(&:keys).uniq.sort
rows=codes.map do|code|;sets=indices.transform_values{|idx|idx.fetch(code,[])};ids=sets.transform_values{|v|v.map{|r|(r['gameId']||r['id']).to_s}.reject(&:empty?).uniq};issues=[];sets.each{|side,v|issues<<"missing_#{side}" if v.empty?;issues<<"duplicate_#{side}" if v.length>1};known=ids.values.flatten.reject(&:empty?).uniq;issues<<'game_id_mismatch' if known.length>1;{code:code,status:issues.empty? ? 'MATCHED':'MISMATCH',issues:issues,gameIds:ids,counts:sets.transform_values(&:length)};end
summary={total:rows.length,matched:rows.count{|r|r[:status]=='MATCHED'},mismatched:rows.count{|r|r[:status]!='MATCHED'}};report={schemaVersion:1,inputs:{frontend:frontend_path,backend:backend_path,database:database_path},summary:summary,rows:rows};FileUtils.mkdir_p(File.dirname(output_path));File.write(output_path,JSON.pretty_generate(report)+"\n");warn "total=#{summary[:total]} matched=#{summary[:matched]} mismatched=#{summary[:mismatched]}";exit 2 unless summary[:mismatched].zero?
