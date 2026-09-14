#!/usr/bin/env ruby
# frozen_string_literal: true
require 'json';require 'time'
root=File.expand_path('..',__dir__);policy=JSON.parse(File.read(File.join(root,'config/tool-safety-policy.json')))
entries=policy.fetch('controlledTools');errors=[]
errors<<'controlled tool paths duplicate' unless entries.map{|entry|entry['path']}.uniq.length==entries.length
entries.each{|entry|errors<<"missing #{entry['path']}" unless File.file?(File.join(root,entry['path']));errors<<"controls incomplete #{entry['path']}" if entry.fetch('controls').length<3}
clean=File.read(File.join(root,'tools/clean-client-generated.sh'));errors<<'client cleanup is not dry-run by default' unless clean.include?('mode=${1:---dry-run}')&&clean.include?('--apply')
migration=File.read(File.join(root,'tools/verify_fresh_migrations.sh'));errors<<'migration verifier can target unrestricted database' unless migration.include?('end in _verify')&&migration.include?('AOO_MIGRATION_MYSQL_PASSWORD')
smoke=File.read(File.join(root,'tools/prepare_backend_smoke_sandbox.py'));%w[--environment --apply AOO_SMOKE_MYSQL_PASSWORD DRY-RUN aoo_smoke_game aoo-smoke-redis].each{|token|errors<<"smoke sandbox misses #{token}" unless smoke.include?(token)}
fault=File.read(File.join(root,'tools/f03_fault_injection.sh'));%w[AOO_ENVIRONMENT F03_CONFIRM_FAULT_INJECTION F03_MAX_OUTAGE_SECONDS trap].each{|token|errors<<"fault injection misses #{token}" unless fault.include?(token)}
service=File.read(File.join(root,'tools/local_service_control.py'));%w[identity required ports already occupied SIGTERM concurrent].each{|token|errors<<"service control misses #{token}" unless service.include?(token)}
report={schemaVersion:1,generatedAt:Time.now.utc.iso8601,task:'TOOL16',passed:errors.empty?,defaultMode:policy['defaultMode'],controlledTools:entries,errors:errors}
File.write(File.join(root,'docs/generated/tool16-safety-boundaries.json'),JSON.pretty_generate(report)+"\n");puts "tool-safety-boundaries: #{errors.empty? ? 'passed':'failed'}";exit(errors.empty? ? 0:1)
