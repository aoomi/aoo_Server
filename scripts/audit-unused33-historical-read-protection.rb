#!/usr/bin/env ruby
require 'json';require 'pathname'
root=Pathname(__dir__).join('..').expand_path
source=root.join('server/GameCommon/src/test/java/com/aoo/bcg/common/serialization/HistoricalFormatCompatibilityTest.java').read
prior=JSON.parse(root.join('work/audit/unused26-historical-formats.json').read)
categories={event:source.include?('readsAndUpcastsHistoricalEventJson'),snapshot:source.include?('RoomSnapshot'),replay:source.include?('ReplayFrame'),config:source.include?('preservesHistoricalRuleConfigurationAndUnknownOptions'),serialization:source.include?('legacyMarker')&&source.include?('legacySpecialRule')}
checks={priorFormatGatePassed:prior['status']=='passed',allHistoricalCategoriesTested:categories.values.all?,futureSchemaRejected:source.include?('schemas.upcast("poker.played", 3'),necessaryDependenciesProtected:root.join('docs/2.22必要依赖保护清单.md').exist?}
errors=checks.reject{|_,v|v}.keys;report={task:'UNUSED33',status:errors.empty? ? 'passed':'failed',checks:checks,categories:categories,test:'HistoricalFormatCompatibilityTest',errors:errors};out=root.join('work/audit/unused33-historical-read-protection.json');out.write(JSON.pretty_generate(report)+"\n");abort("UNUSED33 failed: #{errors.join(', ')}")unless errors.empty?;puts 'UNUSED33 passed: event/snapshot/replay/config/serialization historical reads protected'
