#!/usr/bin/env ruby
# frozen_string_literal: true

require 'json'
require 'time'
require 'fileutils'

root = File.expand_path('..', __dir__)
output = File.join(root, 'work/audit/reference-source-isolation.json')
reference_roots = ENV.fetch('AOO_REFERENCE_ROOTS', File.expand_path('../../Test', root)).split(File::PATH_SEPARATOR)

rows = reference_roots.map do |path|
  exists = File.exist?(path)
  stat = exists ? File.stat(path) : nil
  {
    path: path,
    exists: exists,
    writableByCurrentProcess: exists && File.writable?(path),
    sameDeviceAsNewProject: exists && stat.dev == File.stat(root).dev,
    symlink: File.symlink?(path),
    readOnlyMountProven: false,
    separateCredentialProven: false,
    networkIsolationProven: false
  }
end

summary = {
  referenceRoots: rows.size,
  existing: rows.count { |row| row[:exists] },
  writable: rows.count { |row| row[:writableByCurrentProcess] },
  readOnlyMountProven: rows.count { |row| row[:readOnlyMountProven] },
  separateCredentialProven: rows.count { |row| row[:separateCredentialProven] },
  passed: rows.all? { |row| row[:exists] && !row[:writableByCurrentProcess] && row[:readOnlyMountProven] && row[:separateCredentialProven] && row[:networkIsolationProven] }
}
report = {
  schemaVersion: 1,
  generatedAt: Time.now.utc.iso8601,
  invariant: '2.22 source and original data are read-only reference inputs with separate credentials and no write network path from the new runtime.',
  summary: summary,
  roots: rows,
  remediationRequired: ['read-only mount or immutable artifact store', 'separate read-only database credentials', 'egress deny rules', 'write-attempt negative test']
}
FileUtils.mkdir_p(File.dirname(output))
File.write(output, JSON.pretty_generate(report) + "\n", mode: 'w:UTF-8')
puts JSON.generate(summary)
exit(summary[:passed] ? 0 : 2) if ENV['AOO_ENFORCE_REFERENCE_ISOLATION_GATE'] == '1'
