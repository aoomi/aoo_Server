# frozen_string_literal: true

require "minitest/autorun"
require "open3"
require "json"
require "tmpdir"
require "fileutils"

class VerifyGateTest < Minitest::Test
  ROOT = File.expand_path("../../..", __dir__)
  VERIFY = File.join(ROOT, "tools/legacy-protocol-ledger/verify_ledger.rb")

  def test_committed_ledger_passes_the_ci_gate
    stdout, stderr, status = Open3.capture3("ruby", VERIFY, chdir: ROOT)
    assert status.success?, "#{stdout}\n#{stderr}"
    result = JSON.parse(stdout)
    assert_equal 283, result.fetch("mapped")
    assert_equal 0, result.fetch("unmapped")
    assert_equal 0, result.fetch("duplicates")
    assert_equal 0, result.fetch("unassembled")
  end

  def test_unmapped_protocol_fails
    assert_gate_rejects("unmapped legacy protocol") { |ledger| ledger["rows"].pop }
  end

  def test_duplicate_mapping_fails
    assert_gate_rejects("duplicate legacy mapping") { |ledger| ledger["rows"][1]["legacyProtocol"] = ledger["rows"][0]["legacyProtocol"] }
  end

  def test_missing_production_assembly_fails
    assert_gate_rejects("no production assembly marker") { |ledger| ledger["rows"][0]["assemblyMarker"] = "NOT_A_REAL_ASSEMBLY_MARKER" }
  end

  private

  def assert_gate_rejects(message)
    Dir.mktmpdir("r63-gate-") do |dir|
      source = File.join(ROOT, "tools/legacy-protocol-ledger")
      baseline = File.join(dir, "legacy_protocols.txt")
      ledger_path = File.join(dir, "ledger.json")
      FileUtils.cp(File.join(source, "legacy_protocols.txt"), baseline)
      ledger = JSON.parse(File.read(File.join(source, "ledger.json")))
      yield ledger
      File.write(ledger_path, JSON.generate(ledger))
      _stdout, stderr, status = Open3.capture3({"R63_BASELINE" => baseline, "R63_LEDGER" => ledger_path}, "ruby", VERIFY, chdir: ROOT)
      refute status.success?
      assert_includes stderr, message
    end
  end
end
