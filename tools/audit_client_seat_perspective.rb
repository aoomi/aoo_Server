#!/usr/bin/env ruby
# frozen_string_literal: true
require 'json'; require 'fileutils'; require 'open3'
server = File.expand_path('..', __dir__); client = File.expand_path('../Client', server)
source = 'assets/Common/Code/Runtime/state/SeatPerspective.ts'; test = 'tests/unit/seat-perspective.test.mjs'
stdout, stderr, status = Open3.capture3('node', test, chdir: client)
result = { task: 'SEAT04', passed: status.success?, source: source, test: test,
  coverage: { seatCounts: '2..8', localSeats: 'all', reconnectSeatChanges: 'all pairs', inverseMapping: true },
  stdout: stdout.strip, stderr: stderr.strip }
out = File.join(server, 'work/audit/client-seat-perspective.json'); FileUtils.mkdir_p(File.dirname(out)); File.write(out, JSON.pretty_generate(result) + "\n")
puts JSON.generate(result); exit(result[:passed] ? 0 : 1)
