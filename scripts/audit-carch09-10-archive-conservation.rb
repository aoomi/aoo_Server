#!/usr/bin/env ruby
require 'json'
require 'find'
require 'digest'
require 'fileutils'

root = File.expand_path('..', __dir__)
client = File.expand_path('../Client', root)
archive_roots = Dir.glob(File.join(client, 'development/archived-*')).sort +
                [File.join(client, 'development/migration/aoo_DFMJ-3.8.8')]

rows = []
archive_roots.each do |archive_root|
  Find.find(archive_root) do |path|
    next unless File.file?(path)
    relative = path.delete_prefix(client + '/')
    next if File.basename(path) == '.DS_Store'
    payload = !path.end_with?('.meta')
    disposition = if relative.include?('archived-game-folders/')
                    'deprecated-empty-structure'
                  elsif relative.include?('archived-orientation-folders/')
                    payload ? 'blocked-pending-layout-equivalence' : 'retained-meta-evidence'
                  elsif relative.include?('archived-direct-game-folders/')
                    payload ? 'blocked-pending-game-mapping' : 'retained-meta-evidence'
                  elsif relative.include?('migration/aoo_DFMJ-3.8.8/')
                    'retained-historical-reference'
                  else
                    payload ? 'blocked-pending-feature-equivalence' : 'retained-meta-evidence'
                  end
    rows << {
      path: relative,
      bytes: File.size(path),
      sha256: Digest::SHA256.file(path).hexdigest,
      payload: payload,
      disposition: disposition,
      activeReplacement: nil
    }
  end
end

summary = rows.group_by { |row| row[:disposition] }.transform_values(&:length)
conservation = {
  schemaVersion: 1,
  generatedFrom: 'Client/development archive roots',
  archiveRoots: archive_roots.map { |path| path.delete_prefix(client + '/') },
  entries: rows,
  summary: summary,
  checks: {
    everyFileHasDisposition: rows.all? { |row| !row[:disposition].empty? },
    everyFileHasHash: rows.all? { |row| row[:sha256].match?(/\A[0-9a-f]{64}\z/) },
    noUnclassifiedDisposition: rows.none? { |row| row[:disposition].start_with?('unclassified') }
  }
}

deletion = {
  schemaVersion: 1,
  archiveRoots: conservation[:archiveRoots],
  hashesRecorded: conservation[:checks][:everyFileHasHash],
  historicalTraceabilityRecorded: conservation[:checks][:everyFileHasDisposition],
  uiRegressionEvidenceComplete: false,
  unresolvedEntries: rows.count { |row| row[:disposition].start_with?('blocked-') },
  deletionAllowed: false,
  decision: 'retain',
  reason: 'Archive deletion is blocked until every blocked disposition has an equivalent active replacement and Creator/browser UI regression evidence is complete.'
}

out = File.join(root, 'docs/generated')
FileUtils.mkdir_p(out)
File.write(File.join(out, 'carch09-archive-conservation.json'), JSON.pretty_generate(conservation) + "\n")
File.write(File.join(out, 'carch10-archive-deletion-readiness.json'), JSON.pretty_generate(deletion) + "\n")
abort 'CARCH09 failed: archive file without disposition/hash' unless conservation[:checks].values.all?
puts "CARCH09 PASS: #{rows.length} archive files conserved; CARCH10 RETAIN: #{deletion[:unresolvedEntries]} blocked entries"
