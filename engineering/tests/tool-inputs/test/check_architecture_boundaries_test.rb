# frozen_string_literal: true

require 'fileutils'
require 'minitest/autorun'
require 'open3'
require 'json'
require 'tmpdir'

class CheckArchitectureBoundariesTest < Minitest::Test
  GATE = File.expand_path('../../../audit/tool-inputs/check_architecture_boundaries.rb', __dir__)

  def test_bootstrap_is_allowed_to_assemble_concrete_games
    status, output = run_gate(
      'game-spi' => [],
      'game-common' => ['game-spi'],
      'game-category-poker' => %w[game-spi game-common],
      'zjh' => %w[game-spi game-common game-category-poker],
      'game-hall' => %w[game-spi game-common],
      'game-bootstrap' => %w[game-hall zjh]
    )

    assert status.success?, output
  end

  def test_rejects_reverse_dependency_from_spi
    status, output = run_gate('game-spi' => ['game-common'], 'game-common' => ['game-spi'])

    refute status.success?
    assert_includes output, 'game-spi has reverse/outward dependencies: game-common'
  end

  def test_rejects_concrete_game_coupling_outside_bootstrap
    status, output = run_gate('game-spi' => [], 'zjh' => ['game-spi'], 'game-hall' => %w[game-spi zjh])

    refute status.success?
    assert_includes output, 'game-hall depends on concrete games: zjh'
  end

  def test_cdxzmj_artifact_keeps_mahjong_category_dependency
    status, output = run_gate(
      'game-spi' => [],
      'game-common' => ['game-spi'],
      'game-category-mahjong' => %w[game-spi game-common],
      'cdxzmj' => %w[game-spi game-common]
    )

    refute status.success?
    assert_includes output, 'cdxzmj must depend on game-category-mahjong'
  end

  def test_deepest_registered_directory_owns_nested_sources_once
    status, output = run_gate_with_baseline({
      'scopeMode' => 'deepest-registered',
      'maximumFiles' => {
        'server/Games/Mahjong' => 1,
        'server/Games/Mahjong/CDXZMJ' => 1,
      },
    }, files: {
        'server/Games/Mahjong/Category.java' => '',
        'server/Games/Mahjong/CDXZMJ/Gameplay.java' => '',
        'server/Games/Mahjong/CDXZMJ/target/Generated.java' => '',
      },
    )

    assert status.success?, output
  end

  def test_unregistered_nested_sources_stay_with_parent_scope
    status, output = run_gate_with_baseline({
      'scopeMode' => 'deepest-registered',
      'maximumFiles' => { 'server/Games/Mahjong' => 1 },
    }, files: {
        'server/Games/Mahjong/Category.java' => '',
        'server/Games/Mahjong/UnregisteredGameplay/Gameplay.java' => '',
      },
    )

    refute status.success?
    assert_includes output, 'directory growth exceeds baseline: server/Games/Mahjong=2>1'
  end

  def test_directory_baseline_path_escape_fails_closed
    status, output = run_gate_with_baseline(
      {
        'scopeMode' => 'deepest-registered',
        'maximumFiles' => { '../outside' => 0 },
      },
      files: {},
    )

    refute status.success?
    assert_includes output, 'directory baseline path escapes root: ../outside'
  end

  private

  def run_gate(graph)
    Dir.mktmpdir('architecture-gate') do |root|
      FileUtils.mkdir_p(File.join(root, 'config'))
      File.write(File.join(root, 'config/architecture-directory-baseline.json'),
                 "{\"countedExtensions\":[\".java\"],\"maximumFiles\":{}}\n")
      module_entries = graph.size.times.map { |index| "<module>server/Module#{index}</module>" }.join
      File.write(File.join(root, 'pom.xml'),
                 "<project><modelVersion>4.0.0</modelVersion><modules>#{module_entries}</modules></project>\n")
      graph.each_with_index do |(artifact, dependencies), index|
        directory = File.join(root, 'server', "Module#{index}")
        FileUtils.mkdir_p(directory)
        dependency_xml = dependencies.map do |dependency|
          "<dependency><groupId>com.aoo.bcg</groupId><artifactId>#{dependency}</artifactId></dependency>"
        end.join
        File.write(File.join(directory, 'pom.xml'), <<~XML)
          <project><modelVersion>4.0.0</modelVersion><groupId>com.aoo.bcg</groupId>
          <artifactId>#{artifact}</artifactId><dependencies>#{dependency_xml}</dependencies></project>
        XML
        if %w[cdxzmj njpdk scjymj xcpdk zjh zypk].include?(artifact)
          service = File.join(directory, 'src/main/resources/META-INF/services/com.aoo.bcg.gamespi.GameProvider')
          FileUtils.mkdir_p(File.dirname(service))
          File.write(service, "example.#{artifact}.Provider\n")
        end
      end
      Open3.capture2e('ruby', GATE, root).then { |output, status| return [status, output] }
    end
  end

  def run_gate_with_baseline(baseline, files:)
    Dir.mktmpdir('architecture-gate-baseline') do |root|
      FileUtils.mkdir_p(File.join(root, 'config'))
      configuration = { 'countedExtensions' => ['.java'] }.merge(baseline)
      File.write(
        File.join(root, 'config/architecture-directory-baseline.json'),
        JSON.generate(configuration)
      )
      File.write(File.join(root, 'pom.xml'), "<project><modelVersion>4.0.0</modelVersion></project>\n")
      files.each do |relative, content|
        path = File.join(root, relative)
        FileUtils.mkdir_p(File.dirname(path))
        File.write(path, content)
      end
      Open3.capture2e('ruby', GATE, root).then { |output, status| [status, output] }
    end
  end
end
