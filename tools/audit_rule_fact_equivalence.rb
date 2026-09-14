#!/usr/bin/env ruby
# frozen_string_literal: true

require 'json'
require 'time'
require 'digest'

root = File.expand_path('..', __dir__)
legacy_root = ENV.fetch('AOO_SPLIT_BACKEND_REFERENCE', File.expand_path('../../Test/情怀后端原代码/Server_game_split', root))
current_roots = [File.join(root, 'modules'), File.join(root, 'server')].freeze
catalog = JSON.parse(File.read(File.join(root, 'work/audit/database-runtime-mapping.json'), encoding: 'UTF-8')).fetch('mappings')
output_path = File.join(root, 'work/audit/rule-fact-equivalence.json')
task_path = File.join(root, 'docs/Aoo-前后端全框架功能通信审计任务清单.md')
codes = catalog.map { |row| row.fetch('code').downcase }.uniq.sort

fact_pattern = /(?:assert|expect|should|List\.of\s*\(|Arrays\.asList|new\s+int\s*\[|score|settle|calculate|validate|recognize|allowedOperations|hu|win)/i

def test_files(roots)
  roots.flat_map do |source_root|
    next [] unless Dir.exist?(source_root)
    Dir.glob(File.join(source_root, '**', '*'), File::FNM_DOTMATCH).select do |path|
      next false unless File.file?(path)
      relative = path.delete_prefix(source_root + '/')
      next false if relative.split('/').any? { |part| %w[bin target build .git logs].include?(part) }
      %w[.java .kt].include?(File.extname(path).downcase) &&
        (relative.match?(%r{(?:^|/)(?:test|tests|src/test)(?:/|$)}i) || File.basename(path).match?(/Test/i)) && File.size(path) <= 2_000_000
    end
  end.uniq
end

def collect(roots, codes, fact_pattern)
  test_files(roots).sort.map do |path|
    text = File.binread(path).force_encoding('UTF-8').scrub
    facts = []
    text.lines.each_with_index do |line, index|
      next unless line.match?(fact_pattern)
      normalized = line.downcase.gsub(/\s+/, ' ').strip
      facts << {line: index + 1, hash: Digest::SHA256.hexdigest(normalized), excerpt: line.strip[0, 500]}
    end
    lower = (path + ' ' + text[0, 20_000].to_s).downcase
    matched_codes = codes.select { |code| lower.match?(/(?<![a-z0-9_])#{Regexp.escape(code)}(?![a-z0-9_])/) }.first(50)
    {path: path, codes: matched_codes, facts: facts}
  end.reject { |row| row[:facts].empty? }
end

legacy = collect([legacy_root], codes, fact_pattern)
current = collect(current_roots, codes, fact_pattern)
current_hashes = current.flat_map { |row| row[:facts].map { |fact| fact[:hash] } }.to_h { |hash| [hash, true] }
legacy.each { |row| row[:facts].each { |fact| fact[:candidateExactMatch] = current_hashes.key?(fact[:hash]) } }
covered_legacy_codes = legacy.flat_map { |row| row[:codes] }.uniq.sort
covered_current_codes = current.flat_map { |row| row[:codes] }.uniq.sort
legacy_fact_count = legacy.sum { |row| row[:facts].size }
current_fact_count = current.sum { |row| row[:facts].size }
exact_count = legacy.sum { |row| row[:facts].count { |fact| fact[:candidateExactMatch] } }
summary = {
  catalogCount: codes.size,
  legacyTestFiles: legacy.size, currentTestFiles: current.size,
  legacyFacts: legacy_fact_count, currentFacts: current_fact_count,
  exactFactCandidates: exact_count, unmatchedLegacyFacts: legacy_fact_count - exact_count,
  legacyCodesWithFacts: covered_legacy_codes.size, currentCodesWithFacts: covered_current_codes.size,
  catalogCodesWithoutCurrentFacts: (codes - covered_current_codes).size
}
report = {
  schemaVersion: 1, generatedAt: Time.now.utc.iso8601, summary: summary,
  catalogCodesWithoutCurrentFacts: codes - covered_current_codes,
  legacyCodesWithoutCurrentFacts: covered_legacy_codes - covered_current_codes,
  legacyTests: legacy, currentTests: current,
  limitations: ['Static assertion matching does not prove equal candidates, legality, pattern/Hu recognition or itemized scoring; shared immutable fixtures must run through both engines.']
}
File.write(output_path, JSON.pretty_generate(report) + "\n", mode: 'w:UTF-8')

task = File.read(task_path, encoding: 'UTF-8')
row = "| EQ08 | 未完成 | 已建事实清册·未闭合 | 已盘点旧新测试中的固定牌局、候选、合法性、牌型/胡型及算分事实；目录=#{summary[:catalogCount]}、旧/新事实=#{summary[:legacyFacts]}/#{summary[:currentFacts]}、精确候选=#{summary[:exactFactCandidates]}、新端有事实玩法=#{summary[:currentCodesWithFacts]}、缺事实玩法=#{summary[:catalogCodesWithoutCurrentFacts]}，尚需共享不可变 fixture 双引擎执行。 证据：work/audit/rule-fact-equivalence.json；工具：tools/audit_rule_fact_equivalence.rb |"
task.sub!(/^\| EQ08 \|.*$/, row) or abort 'EQ08 row not found'
task.sub!(/^下一项：.*$/, '下一项：EQ09 历史数据等价') or abort 'next pointer not found'
File.write(task_path, task, mode: 'w:UTF-8')
puts JSON.generate(summary)
