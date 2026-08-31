#!/usr/bin/env ruby
# frozen_string_literal: true

require 'json'
require 'time'
require 'digest'

root = File.expand_path('..', __dir__)
legacy_roots = [
  File.expand_path('../../Test/QH_DFMJ/client-unified-3.8.6/assets', root),
  File.expand_path('../../Test/情怀后端原代码/Server_game_split', root)
].freeze
current_roots = [File.join(root, 'modules'), File.join(root, 'server'), File.expand_path('../Client/assets', root)].freeze
output_path = File.join(root, 'work/audit/precondition-equivalence.json')
task_path = File.join(root, 'docs/Aoo-前后端全框架功能通信审计任务清单.md')

categories = {
  'identity' => /(?:userId|playerId|accountId|ownerId|token|authenticated|login|identity)/i,
  'asset' => /(?:balance|currency|gold|diamond|roomCard|ticket|cost|wallet|asset)/i,
  'seatCount' => /(?:seatLimit|seatId|playerCount|peopleNum|fullRoom|roomFull|人数|座位)/i,
  'stage' => /(?:phase|stage|status|state|round|started|finished|ready)/i,
  'region' => /(?:region|province|city|location|gps|latitude|longitude|地区|定位)/i,
  'permission' => /(?:permission|role|admin|owner|creator|member|authority|privilege|权限)/i
}.freeze
condition_pattern = /(?:\bif\s*\(|\bunless\b|\brequire\s*\(|\bassert\w*\s*\(|\bthrow\b|\.filter\s*\(|\.validate\w*\s*\()/i

def source_files(roots)
  roots.flat_map do |source_root|
    next [] unless Dir.exist?(source_root)
    Dir.glob(File.join(source_root, '**', '*'), File::FNM_DOTMATCH).select do |path|
      next false unless File.file?(path)
      relative = path.delete_prefix(source_root + '/')
      next false if relative.split('/').any? { |part| %w[bin target build library temp node_modules .git logs].include?(part) }
      %w[.java .kt .ts .js .mjs .cjs].include?(File.extname(path).downcase) && File.size(path) <= 2_000_000
    end
  end.uniq
end

def collect(roots, categories, condition_pattern)
  rows = []
  source_files(roots).sort.each do |path|
    text = File.binread(path).force_encoding('UTF-8').scrub
    text.lines.each_with_index do |line, index|
      next unless line.match?(condition_pattern)
      kinds = categories.each_with_object([]) { |(kind, regex), found| found << kind if line.match?(regex) }
      next if kinds.empty?
      normalized = line.downcase.gsub(/\s+/, ' ').gsub(/\d+/, '#').strip[0, 400]
      rows << {path: path, line: index + 1, categories: kinds, expressionHash: Digest::SHA256.hexdigest(normalized), excerpt: line.strip[0, 400]}
    end
  end
  rows
end

legacy = collect(legacy_roots, categories, condition_pattern)
current = collect(current_roots, categories, condition_pattern)
current_hashes = current.map { |row| row[:expressionHash] }.to_h { |hash| [hash, true] }
legacy.each { |row| row[:candidateExactMatch] = current_hashes.key?(row[:expressionHash]) }
by_category = categories.keys.to_h do |kind|
  old_rows = legacy.select { |row| row[:categories].include?(kind) }
  new_rows = current.select { |row| row[:categories].include?(kind) }
  [kind, {legacy: old_rows.size, current: new_rows.size, exactCandidates: old_rows.count { |row| row[:candidateExactMatch] }}]
end
summary = {
  legacyConditions: legacy.size,
  currentConditions: current.size,
  exactCandidates: legacy.count { |row| row[:candidateExactMatch] },
  unmatchedLegacy: legacy.count { |row| !row[:candidateExactMatch] },
  byCategory: by_category
}
report = {
  schemaVersion: 1,
  generatedAt: Time.now.utc.iso8601,
  legacyRoots: legacy_roots,
  currentRoots: current_roots,
  summary: summary,
  legacyConditions: legacy,
  currentConditions: current,
  limitations: ['Expression hashes identify exact normalized candidates only; identity, asset, seat, stage, region and permission semantics require scenario-level differential execution.']
}
File.write(output_path, JSON.pretty_generate(report) + "\n", mode: 'w:UTF-8')

task = File.read(task_path, encoding: 'UTF-8')
row = "| EQ02 | 未完成 | 已建矩阵·未闭合 | 已按身份、资产、人数/座位、阶段、地区及权限抽取旧新前置条件；旧条件=#{summary[:legacyConditions]}、新条件=#{summary[:currentConditions]}、精确归一候选=#{summary[:exactCandidates]}、未匹配旧条件=#{summary[:unmatchedLegacy]}，尚需场景级差分执行证明语义等价。 证据：work/audit/precondition-equivalence.json；工具：tools/audit_precondition_equivalence.rb |"
task.sub!(/^\| EQ02 \|.*$/, row) or abort 'EQ02 row not found'
task.sub!(/^下一项：.*$/, '下一项：EQ03 状态转换等价') or abort 'next pointer not found'
File.write(task_path, task, mode: 'w:UTF-8')
puts JSON.generate(summary)
