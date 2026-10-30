#!/usr/bin/env ruby
require 'json'
require 'open3'
require 'tmpdir'

root = File.expand_path('../../..', __dir__)
Dir.mktmpdir('aoo-room-rule-selection') do |directory|
  output = File.join(directory, 'rules.generated.json')
  registry = File.join(directory, 'rules.keys.json')
  stdout, stderr, status = Open3.capture3({
    'AOO_ROOM_RULE_GENERATED'=>output,
    'AOO_ROOM_RULE_REGISTRY'=>registry
  }, 'ruby', File.join(root, 'engineering/release/tool-inputs/room-rules-publisher.rb'), 'preview')
  abort(stderr) unless status.success?
  fields = JSON.parse(stdout).fetch('fields')
  fields.each do |field|
    control = field.fetch('control')
    if control == 'radio'
      abort("radio #{field['key']} must be required") unless field['required'] == true
      abort("radio #{field['key']} needs candidates") if field.fetch('defaultCandidateIndexes').empty?
    elsif control == 'checkbox'
      abort("checkbox #{field['key']} must be optional") unless field['required'] == false
      abort("checkbox #{field['key']} defaults must come from Excel indexes") unless field.fetch('defaultCandidateIndexes').all? { |index| index.is_a?(Integer) && index.positive? }
    else
      abort("unknown control #{control}")
    end
  end
  puts JSON.generate(fields.to_h { |field| [field['key'], {
    control: field['control'], required: field['required'], defaults: field['defaultCandidateIndexes']
  }] })
end
