require 'json'
require 'minitest/autorun'

ROOT = File.expand_path('../../../..', __dir__)
CLIENT = File.join(ROOT, 'Client')

class Unused17NpmDevelopmentContractTest < Minitest::Test
  def package
    JSON.parse(File.read(File.join(CLIENT, 'package.json')))
  end

  def lockfile
    File.read(File.join(CLIENT, 'pnpm-lock.yaml'))
  end

  def verify_script
    File.read(File.join(CLIENT, 'scripts/verify-client-manager.mjs'))
  end

  def test_project_toolchain_is_pinned_to_the_authoritative_contract
    assert_equal 'pnpm@11.19.0', package.fetch('packageManager')
    assert_equal '24.19.0', package.fetch('engines').fetch('node')
    assert_equal '11.19.0', package.fetch('engines').fetch('pnpm')
    assert_equal '3.8.8', package.fetch('engines').fetch('cocos')
  end

  def test_standalone_typescript_drift_is_absent
    refute_match(/typescript\s*:\s*5\.6\.3/, File.read(File.join(CLIENT, 'package.json')))
    refute_match(/typescript@5\.6\.3|typescript@5\.9\.2/, lockfile)
    refute_match(/pnpm[^\n]*typescript@(?:5\.6\.3|5\.9\.2)/, verify_script)
  end

  def test_client_manager_uses_creator_compiler_and_fails_closed
    assert_includes verify_script, 'Cocos/Creator/3.8.8'
    assert_includes verify_script, '4.9.5'
    assert_includes verify_script, 'Cocos Creator 3.8.8 TypeScript compiler is unavailable'
  end
end
