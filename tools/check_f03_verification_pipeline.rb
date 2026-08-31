#!/usr/bin/env ruby
# frozen_string_literal: true

require 'json'
require 'time'

root = File.expand_path('..', __dir__)
source = File.join(root, 'config/f03-verification-pipeline.json')
report_path = File.join(root, 'docs/generated/tool08-f03-verification-pipeline.json')
pipeline = JSON.parse(File.read(source))
stages = pipeline.fetch('stages')
errors = []

%w[id owner responsibility evidence].each do |field|
  errors << "duplicate #{field}" unless stages.map { |stage| stage.fetch(field) }.uniq.size == stages.size
end
stages.each do |stage|
  errors << "missing owner #{stage['owner']}" unless File.file?(File.join(root, stage.fetch('owner')))
end
required = %w[
  runtime-workload deep-room-workload dependency-fault-injection authority-recovery
  resource-soak-sampling evidence-finalization evidence-orchestration
]
errors << 'stage set is incomplete' unless stages.map { |stage| stage['id'] }.sort == required.sort

finalizer = File.read(File.join(root, 'tools/finalize_f03_soak.py'))
errors << 'finalizer must consume resource analyzer output' unless finalizer.include?('analyze_account_soak.py')
errors << 'finalizer must consume protocol workload evidence' unless finalizer.include?('args.workload')
errors << 'finalizer must consume deep room workload evidence' unless finalizer.include?('args.deep_workload')
fault = File.read(File.join(root, 'tools/f03_fault_injection.sh'))
errors << 'fault injection must guarantee dependency restoration' unless fault.include?('trap cleanup EXIT INT TERM')
errors << 'fault injection must bound expected failures' unless fault.include?('alarm $timeout')
recovery = File.read(File.join(root, 'tools/f03_v2_authority_recovery_smoke.py'))
errors << 'authority recovery must compare exact state' unless recovery.include?('assert actual == saved["expected"]')

report = {
  schemaVersion: 1,
  generatedAt: Time.now.utc.iso8601,
  task: 'TOOL08',
  passed: errors.empty?,
  stageCount: stages.size,
  stages: stages,
  errors: errors
}
File.write(report_path, JSON.pretty_generate(report) + "\n")
puts "f03-verification-pipeline: #{errors.empty? ? 'passed' : 'failed'}"
exit(errors.empty? ? 0 : 1)
