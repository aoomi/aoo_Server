#!/usr/bin/env ruby
# frozen_string_literal: true

require 'json'
require 'time'
require 'fileutils'

root = File.expand_path('..', __dir__)
client = File.expand_path('../Client/assets', root)
output = File.join(root, 'work/audit/store-ui-subscription-graph.json')
ui_patterns = {
  text: /(?:Label|\.string\s*=|setText|\btext\()/,
  button: /(?:Button|\.interactable\s*=|\.enabled\s*=|\.active\s*=)/,
  list: /(?:instantiate\(|addChild\(|Layout|ScrollView)/,
  animation: /(?:Animation|Tween|tween\(|\.play\()/,
  popup: /(?:forms?\.show\(|showForm\(|Popup|Dialog)/
}.freeze

rows = []
Dir.glob(File.join(client, '**/*.ts')).each do |path|
  text = File.read(path, encoding: 'UTF-8', invalid: :replace, undef: :replace)
  sinks = ui_patterns.keys.select { |kind| text.match?(ui_patterns.fetch(kind)) }
  next if sinks.empty?
  relative = path.delete_prefix(client + '/')
  rows << {
    path: relative,
    sinks: sinks,
    authoritativeStoreImported: text.include?('AuthoritativeRoomStore'),
    storeSubscription: text.match?(/\.subscribe\s*\(/),
    disposerPresent: text.match?(/(?:unsubscribe|dispose|onDestroy)/),
    dynamicNodeLookup: text.match?(/(?:getChildByName|\.getChildByPath|find\s*\()/)
  }
end

closed = rows.count { |row| row[:authoritativeStoreImported] && row[:storeSubscription] && row[:disposerPresent] }
summary = {uiFiles: rows.size, subscriptionClosed: closed, missingSubscription: rows.size - closed,
           dynamicNodeLookupFiles: rows.count { |row| row[:dynamicNodeLookup] },
           sinkCounts: ui_patterns.keys.to_h { |kind| [kind, rows.count { |row| row[:sinks].include?(kind) }] },
           passed: rows.any? && closed == rows.size}
report = {schemaVersion: 1, generatedAt: Time.now.utc.iso8601,
          invariant: 'Every room UI text, button, list, animation and popup is a disposable projection subscribed to AuthoritativeRoomStore.',
          summary: summary, subscriptions: rows}
FileUtils.mkdir_p(File.dirname(output))
File.write(output, JSON.pretty_generate(report) + "\n", mode: 'w:UTF-8')
puts JSON.generate(summary)
exit(summary[:passed] ? 0 : 2) if ENV['AOO_ENFORCE_UI_SUBSCRIPTION_GATE'] == '1'
