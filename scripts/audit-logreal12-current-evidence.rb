#!/usr/bin/env ruby
require 'json'
require 'fileutils'
root = File.expand_path('..', __dir__)
items = [
  ['LOGREAL01', 'docs/generated/logreal01-backend-runtime.json', 'current generated runtime analysis'],
  ['LOGREAL02', 'docs/generated/logreal02-hall.json', 'current generated hall analysis'],
  ['LOGREAL03-04', 'docs/generated/runtime-game-log-ledger.json', 'current reproducible mapping ledger'],
  ['LOGREAL05', 'docs/generated/logreal05-repeated-errors.json', 'current fingerprint/root-cause gate'],
  ['LOGREAL06', 'server/GameCommon/src/test/java/com/aoo/bcg/common/observability/OperationLogContextTest.java', 'executable context test'],
  ['LOGREAL07', '../Client/assets/Common/Code/Runtime/network/ClientErrorCorrelation.ts', 'current V2 client implementation'],
  ['LOGREAL08', 'server/Gateway/src/test/java/com/aoo/bcg/gateway/GameOperationTimelineTest.java', 'executable timeline test'],
  ['LOGREAL09', 'docs/generated/logreal09-legacy-entrypoint-ledger.json', 'current retirement generator output'],
  ['LOGREAL10', 'docs/generated/logreal10-sensitive-logging.json', 'current source scanning gate'],
  ['LOGREAL11', 'docs/generated/logreal11-noise-policy.json', 'current source/config gate']
]
matrix = items.map do |task, path, evidence_type|
  absolute = File.expand_path(path, root)
  {task: task, evidence: path, evidenceType: evidence_type,
   exists: File.file?(absolute), nonEmpty: File.file?(absolute) && File.size(absolute).positive?}
end
passed = matrix.all? { |row| row[:exists] && row[:nonEmpty] }
report = {task: 'LOGREAL12', status: passed ? 'passed' : 'failed',
  policy: 'Historical logs are never acceptance evidence. They may create a diagnostic hypothesis only. Closure requires current source, executable test, runtime reproduction, or a reproducible source/config gate.',
  matrix: matrix,
  historicalFingerprintDisposition: 'diagnostic-backlog-not-closed-by-history'}
out = File.join(root, 'docs/generated/logreal12-current-evidence.json')
FileUtils.mkdir_p(File.dirname(out)); File.write(out, JSON.pretty_generate(report) + "\n")
abort "LOGREAL12 failed: #{matrix.reject { |row| row[:exists] && row[:nonEmpty] }}" unless passed
puts 'LOGREAL12 PASS: current-evidence matrix complete'
