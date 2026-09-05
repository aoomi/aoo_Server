#!/usr/bin/env ruby
require 'json'
require 'open3'

root = File.expand_path('..', __dir__)
stdout, stderr, status = Open3.capture3('ruby', File.join(root, 'tools/room-rules-publisher.rb'), 'preview')
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
