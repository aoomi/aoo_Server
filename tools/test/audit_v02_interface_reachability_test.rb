# frozen_string_literal: true

require 'json'
require 'open3'
require 'minitest/autorun'

class AuditV02InterfaceReachabilityTest < Minitest::Test
  SCRIPT = File.expand_path('../audit_v02_interface_reachability.rb', __dir__)

  def setup
    @stdout, @stderr, @status = Open3.capture3('ruby', SCRIPT)
    @report = JSON.parse(@stdout)
  end

  def test_process_status_matches_zero_break_policy
    assert_equal '', @stderr
    expected = @report.fetch('findings').empty? ? 'PASS' : 'FAIL'
    assert_equal expected, @report.fetch('result')
    assert_equal(expected == 'PASS', @status.success?)
  end

  def test_every_break_has_one_machine_stable_id_and_responsibility_boundary
    findings = @report.fetch('findings')
    ids = findings.map { |row| row.fetch('id') }
    assert_equal ids.uniq, ids
    findings.each do |row|
      refute_empty row.fetch('boundary')
      refute_empty row.fetch('evidence')
    end
  end

  def test_gate_requires_zero_findings_to_pass
    assert_equal 'Only zero findings passes; manual/real-device acceptance is excluded.', @report.fetch('policy')
    assert_equal(@report.fetch('findings').empty?, @status.success?)
  end
end
