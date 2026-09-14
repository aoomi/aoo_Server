#!/usr/bin/env ruby
# frozen_string_literal: true

require 'csv'
require 'json'
require 'time'

ROOT = File.expand_path('..', __dir__)
CATALOG = File.join(ROOT, 'server/Bootstrap/src/main/resources/game-catalog-528.tsv')
COMPONENT_REPORT = File.join(ROOT, 'work/audit/game-component-completeness.json')
OUTPUT = File.join(ROOT, 'docs/generated/game08-catalog-expansion.json')
TASKS = File.join(ROOT, 'docs/Aoo-前后端全框架功能通信审计任务清单.md')
MANIFEST = File.join(ROOT, 'docs/generated/generated-artifact-manifest.json')

catalog_rows = CSV.read(CATALOG, headers: true, col_sep: "\t", encoding: 'UTF-8')
component_report = JSON.parse(File.read(COMPONENT_REPORT, encoding: 'UTF-8'))
summary = component_report.fetch('summary')
games = component_report.fetch('games')

report = {
  schemaVersion: 1,
  generatedAt: Time.now.utc.iso8601,
  task: 'GAME08',
  catalogRows: catalog_rows.size,
  classifiedGames: summary.fetch('gameCount'),
  metadataOnlyGames: summary.fetch('metadataOnlyGames'),
  completeGames: summary.fetch('completeGames'),
  incompleteGames: summary.fetch('incompleteGames'),
  nativeSourceGames: games.reject { |game| game.fetch('sourceType') == 'CATALOG_METADATA' }.map { |game| game.fetch('code') }.sort,
  verdict: summary.fetch('metadataOnlyGames').zero? && summary.fetch('incompleteGames').zero? ? 'passed' : 'failed',
  blockers: [
    'catalog-bridge exposes metadata lifecycle only and is not an executable per-game implementation',
    'metadata-only games have no native provider, authoritative lifecycle, rules, flow, scoring and player-view snapshot closure',
    'automatic per-game vertical test generation and execution is absent'
  ],
  sourceEvidence: {
    catalog: 'server/Bootstrap/src/main/resources/game-catalog-528.tsv',
    bridgeProvider: 'server/Bootstrap/src/main/java/com/aoo/bcg/bootstrap/CatalogGameProvider.java',
    componentReport: 'work/audit/game-component-completeness.json'
  }
}
File.write(OUTPUT, JSON.pretty_generate(report) + "\n", mode: 'w:UTF-8')

tasks = File.read(TASKS, encoding: 'UTF-8')
row = "| GAME08 | 失败跳过 | 未列玩法自动扩展 | 未完成 | 目录/分类共 #{report[:classifiedGames]} 个玩法，完整纵向实现 0 个、未完整 #{report[:incompleteGames]} 个，其中 #{report[:metadataOnlyGames]} 个仍为 catalog-bridge 元数据占位；通用桥仅提供目录生命周期，未生成原生 Provider、权威流程、规则、算分、玩家视角快照及逐玩法动态测试，按规则记录后跳过。证据：docs/generated/game08-catalog-expansion.json |"
tasks.sub!(/^\| GAME08 \|.*$/, row) or abort 'GAME08 row not found'
File.write(TASKS, tasks, mode: 'w:UTF-8')

manifest = JSON.parse(File.read(MANIFEST, encoding: 'UTF-8'))
artifacts = manifest.fetch('artifacts')
path = 'docs/generated/game08-catalog-expansion.json'
unless artifacts.any? { |artifact| artifact['path'] == path }
  artifacts << {
    'path' => path,
    'kind' => 'generated-game-audit-evidence',
    'authoritativeInputs' => [
      'server/Bootstrap/src/main/resources/game-catalog-528.tsv',
      'server/Bootstrap/src/main/java/com/aoo/bcg/bootstrap/CatalogGameProvider.java',
      'work/audit/game-component-completeness.json'
    ],
    'generator' => 'scripts/audit-game08-catalog-expansion.rb',
    'rebuild' => 'ruby scripts/audit-game08-catalog-expansion.rb',
    'owner' => 'game-migration',
    'editPolicy' => 'generated-do-not-edit'
  }
end
File.write(MANIFEST, JSON.pretty_generate(manifest) + "\n", mode: 'w:UTF-8')

puts JSON.generate(report.slice(:task, :catalogRows, :classifiedGames, :metadataOnlyGames, :completeGames, :incompleteGames, :verdict))
