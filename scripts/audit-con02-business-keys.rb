#!/usr/bin/env ruby
require 'json'
root=File.expand_path('..',__dir__)
ddl=File.read(File.join(root,'database/migrations/V20260823_12__play_member_template_business_keys.sql'))
test=File.read(File.join(root,'server/GameCommon/src/test/java/com/aoo/bcg/common/persistence/BusinessKeyConcurrencyIntegrationTest.java'))
checks={'play_business_key'=>ddl.include?('PRIMARY KEY (game_id, play_version, region_code)'),'member_business_key'=>ddl.include?('uk_club_member_business'),'template_business_key'=>ddl.include?('PRIMARY KEY (club_id, template_code, template_version)'),'profile_business_key'=>ddl.include?('uk_game_profile_business'),'real_database_race_test'=>test.include?('race(16')&&test.include?('getErrorCode()!=1062'),'all_three_domains_tested'=>%w[aoo_play_variant aoo_club_member aoo_room_template].all?{|name|test.include?(name)}}
out={'task'=>'CON02','passed'=>checks.values.all?,'checks'=>checks};path=File.join(root,'work/audit/business-key-concurrency.json');File.write(path,JSON.pretty_generate(out)+"\n");abort JSON.generate(out) unless out['passed'];puts JSON.generate(out)
