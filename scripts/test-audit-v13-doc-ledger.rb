#!/usr/bin/env ruby
require 'json'
require 'open3'

root = File.expand_path('..', __dir__)
stdout, stderr, status = Open3.capture3('ruby', File.join(root, 'scripts/audit-v13-doc-ledger.rb'), chdir: root)
report = JSON.parse(File.read(File.join(root, 'docs/generated/v13-doc-ledger-consistency.json')))
abort stderr unless stderr.empty?
abort "unexpected output: #{stdout}" unless stdout.start_with?('V13 ')
abort 'canonical ledger changed' unless report['canonicalLedger'] == 'docs/Aoo-当前剩余问题与处理任务清单.md'
abort 'active status set changed' unless report['activeStatuses'].keys.sort == %w[68 77 87 88]
abort 'audit must remain blocked while evidence claims are unresolved' if status.success? || report['verdict'] != 'blocked'
abort 'ledger cleanup regressed' unless report.dig('checks', 'obsoleteCurrentLedgersRemoved')
abort 'self-acceptance guard regressed' unless report.dig('checks', 'activeStatusesNotSelfAccepted')
puts 'V13 TEST PASS: unresolved evidence blocks acceptance without invalidating ledger cleanup'
