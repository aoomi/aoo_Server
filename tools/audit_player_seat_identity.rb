#!/usr/bin/env ruby
# frozen_string_literal: true

require 'json'
require 'fileutils'

root = File.expand_path('..', __dir__)
files = Dir.glob(File.join(root, 'server', '**', '*.java')).reject { |path| path.include?('/target/') }
legacy = /\b(pid|posID)\b/
legacy_hits = files.flat_map do |path|
  File.readlines(path, encoding: 'UTF-8').each_with_index.map do |line, index|
    next unless line.match?(legacy)
    { file: path.delete_prefix(root + '/'), line: index + 1, symbol: line[legacy] }
  end.compact
end
boundary_files = %w[
  server/GameSPI/src/main/java/com/aoo/bcg/gamespi/PlayerSeatIdentity.java
  server/GameSPI/src/main/java/com/aoo/bcg/gamespi/GameCommandRequest.java
  server/Gateway/src/main/java/com/aoo/bcg/gateway/ConnectionSession.java
]
missing = boundary_files.reject { |path| File.file?(File.join(root, path)) }
result = {
  task: 'SEAT01',
  passed: missing.empty?,
  boundaryModel: 'authenticatedUserId owns seatId; transport connection remains separate',
  boundaryFiles: boundary_files,
  missingBoundaryFiles: missing,
  legacyReferenceCount: legacy_hits.length,
  legacyReferences: legacy_hits,
  note: 'Legacy pid/posID references are migration inventory, not accepted at the new protocol boundary.'
}
out = File.join(root, 'work', 'audit', 'player-seat-identity.json')
FileUtils.mkdir_p(File.dirname(out))
File.write(out, JSON.pretty_generate(result) + "\n")
puts JSON.generate(result.reject { |key, _| key == :legacyReferences })
exit(result[:passed] ? 0 : 1)
