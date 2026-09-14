#!/usr/bin/env ruby
# frozen_string_literal: true

require 'json'
require 'time'
require 'fileutils'

root = File.expand_path('..', __dir__)
client = File.expand_path('../Client/assets', root)
output = File.join(root, 'work/audit/client-single-store-audit.json')
store_path = 'Common/Code/Runtime/state/AuthoritativeRoomStore.ts'
files = Dir.glob(File.join(client, '**/*.ts'))
state_owners = []
adopters = []
files.each do |path|
  text = File.read(path, encoding: 'UTF-8', invalid: :replace, undef: :replace)
  relative = path.delete_prefix(client + '/')
  next if relative == store_path
  adopters << relative if text.include?('AuthoritativeRoomStore')
  if relative.match?(/(?:Model|Manager|Controller|Runtime)\.ts$/) && text.match?(/\b(?:private|public|protected)\s+\w+\s*(?:=|:)/)
    state_owners << relative
  end
end
summary = {authoritativeStorePresent: File.file?(File.join(client, store_path)), mutableStateOwners: state_owners.size,
           adoptingFiles: adopters.size, nonAdoptingStateOwners: (state_owners - adopters).size,
           passed: state_owners.any? && (state_owners - adopters).empty?}
report = {schemaVersion: 1, generatedAt: Time.now.utc.iso8601,
          invariant: 'Room state is mutated only through AuthoritativeRoomStore; Models, Controllers and UI are projections/subscribers.',
          summary: summary, store: store_path, adopters: adopters, nonAdoptingStateOwners: state_owners - adopters}
FileUtils.mkdir_p(File.dirname(output))
File.write(output, JSON.pretty_generate(report) + "\n", mode: 'w:UTF-8')
puts JSON.generate(summary)
exit(summary[:passed] ? 0 : 2) if ENV['AOO_ENFORCE_CLIENT_STORE_GATE'] == '1'
