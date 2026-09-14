#!/usr/bin/env ruby
# frozen_string_literal: true

require 'json'
require 'pathname'
require 'time'

root = Pathname(ARGV.fetch(0, '../Client/assets')).expand_path
out = Pathname(ARGV.fetch(1, 'work/audit/cocos-interactions.tsv')).expand_path
summary_out = out.sub_ext('.summary.json')
types = %w[cc.Button cc.Toggle cc.Slider cc.EditBox cc.ScrollView cc.PageView cc.BlockInputEvents].freeze

rows = []
errors = []
Dir.glob(root.join('**/*.{prefab,scene}')).sort.each do |file|
  begin
    objects = JSON.parse(File.binread(file).force_encoding(Encoding::UTF_8))
    next unless objects.is_a?(Array)

    node_paths = {}
    node_path = lambda do |id|
      return node_paths[id] if node_paths.key?(id)
      node = objects[id]
      return "<missing:#{id}>" unless node.is_a?(Hash)
      name = node['_name'].to_s.empty? ? "<node:#{id}>" : node['_name'].to_s
      parent = node.dig('_parent', '__id__')
      node_paths[id] = parent.is_a?(Integer) ? "#{node_path.call(parent)}/#{name}" : name
    end

    objects.each_with_index do |obj, id|
      next unless obj.is_a?(Hash) && types.include?(obj['__type__'])
      node_id = obj.dig('node', '__id__')
      events = Array(obj['_clickEvents']) + Array(obj['clickEvents'])
      handlers = events.each_with_object([]) do |event_ref, found|
        event_id = event_ref.is_a?(Hash) ? event_ref['__id__'] : nil
        event = event_id.is_a?(Integer) ? objects[event_id] : nil
        next unless event.is_a?(Hash)
        found << [event['component'], event['handler'], event['customEventData']].compact.join('#')
      end
      rows << [file.delete_prefix(root.to_s + '/'), node_id, node_id.is_a?(Integer) ? node_path.call(node_id) : '<unbound>',
               obj['__type__'], obj.fetch('_enabled', true), events.length, handlers.join(',')]
    end
  rescue StandardError => e
    errors << [file.delete_prefix(root.to_s + '/'), e.class.name, e.message]
  end
end

out.dirname.mkpath
File.open(out, 'w') do |io|
  io.puts %w[file nodeId nodePath component enabled serializedEventCount handlers].join("\t")
  rows.each { |row| io.puts row.join("\t") }
end

counts = rows.group_by { |row| row[3] }.transform_values(&:length)
summary = {
  generatedAt: Time.now.utc.iso8601,
  assetRoot: root.to_s,
  assetFilesScanned: Dir.glob(root.join('**/*.{prefab,scene}')).length,
  interactiveComponents: rows.length,
  serializedEventBindings: rows.sum { |row| row[5] },
  componentsWithoutSerializedEvents: rows.count { |row| row[5].zero? },
  componentCounts: counts.sort.to_h,
  parseErrors: errors.map { |file, type, message| { file: file, type: type, message: message } }
}
File.write(summary_out, JSON.pretty_generate(summary) + "\n")
warn "wrote #{rows.length} interactions to #{out} (#{errors.length} parse errors)"
exit(errors.empty? ? 0 : 2)
