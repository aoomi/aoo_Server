#!/usr/bin/env ruby
# frozen_string_literal: true
require 'json'
require 'fileutils'
root = File.expand_path('..', __dir__)
required = %w[SeatAdmissionPhase.java SeatAdmission.java SeatAdmissionCoordinator.java].map do |name|
  "server/GameSPI/src/main/java/com/aoo/bcg/gamespi/#{name}"
end
handler = File.read(File.join(root, 'server/GameSPI/src/main/java/com/aoo/bcg/gamespi/AuthoritativeSessionCommandHandler.java'))
phases = %w[AUTHENTICATED RESOURCES_READY SNAPSHOT_READY ADMITTED REJECTED]
missing = required.reject { |path| File.file?(File.join(root, path)) }
missing_phases = phases.reject { |phase| Dir.glob(File.join(root, 'server/GameSPI/src/main/java/**/*.java')).any? { |path| File.read(path).include?(phase) } }
checks = {
  admission_files_present: missing.empty?, phases_present: missing_phases.empty?,
  join_prepared_before_execute: handler.index('admissions.prepare') &&
    handler.index('.execute(request)', handler.index('admissions.prepare')) &&
    handler.index('admissions.prepare') < handler.index('.execute(request)', handler.index('admissions.prepare')),
  failure_rolls_back_admission: handler.include?('admissions.rejected'),
  success_marks_admitted: handler.include?('admissions.admitted')
}
result = { task: 'SEAT02', passed: checks.values.all?, checks: checks, missingFiles: missing, missingPhases: missing_phases }
out = File.join(root, 'work/audit/seat-admission-lifecycle.json'); FileUtils.mkdir_p(File.dirname(out)); File.write(out, JSON.pretty_generate(result) + "\n")
puts JSON.generate(result); exit(result[:passed] ? 0 : 1)
