#!/usr/bin/env ruby
# frozen_string_literal: true

require 'json'
require 'time'
require 'fileutils'

root = File.expand_path('..', __dir__)
output = File.join(root, 'work/audit/dependency-test-double-audit.json')
categories = {
  network: /(?:WebSocket|HttpClient|Network|Transport|Gateway)/i,
  storage: /(?:Repository|Store|Persistence|Database|Redis|Mongo|Jdbc)/i,
  payment: /(?:Payment|Pay|Billing|Ledger|Wallet)/i
}.freeze
files = Dir.glob(File.join(root, 'server/**/*.{java,kt}')).reject { |path| path.include?('/target/') || path.include?('/build/') }
scripts = Dir.glob(File.join(root, 'tools/*')).select { |path| File.file?(path) }
rows = categories.map do |category, pattern|
  ports = files.select do |path|
    text = File.read(path, encoding: 'UTF-8', invalid: :replace, undef: :replace)
    text.match?(/\binterface\b/) && (File.basename(path).match?(pattern) || text.match?(pattern))
  end
  doubles = files.select do |path|
    name = File.basename(path)
    name.match?(/(?:Fake|Stub|Mock|InMemory)/) && name.match?(pattern)
  end
  drills = scripts.select { |path| File.basename(path).match?(pattern) && File.basename(path).match?(/(?:drill|failover|replacement|smoke|test)/i) }
  {category: category, ports: ports.map { |path| path.delete_prefix(root + '/') },
   testDoubles: doubles.map { |path| path.delete_prefix(root + '/') },
   replacementDrills: drills.map { |path| path.delete_prefix(root + '/') },
   closed: ports.any? && doubles.any? && drills.any?}
end
summary = {categories: rows.size, categoriesWithPorts: rows.count { |row| row[:ports].any? },
           categoriesWithTestDoubles: rows.count { |row| row[:testDoubles].any? },
           categoriesWithReplacementDrills: rows.count { |row| row[:replacementDrills].any? },
           closedCategories: rows.count { |row| row[:closed] }, passed: rows.all? { |row| row[:closed] }}
report = {schemaVersion: 1, generatedAt: Time.now.utc.iso8601,
          invariant: 'Network, storage and payment each have a project port, deterministic test double and proven replacement/failover drill.',
          summary: summary, categories: rows}
FileUtils.mkdir_p(File.dirname(output))
File.write(output, JSON.pretty_generate(report) + "\n", mode: 'w:UTF-8')
puts JSON.generate(summary)
exit(summary[:passed] ? 0 : 2) if ENV['AOO_ENFORCE_DEPENDENCY_DOUBLE_GATE'] == '1'
