#!/usr/bin/env ruby
# frozen_string_literal: true

require 'fileutils'
require 'json'
require 'minitest/autorun'
require 'open3'
require 'rbconfig'
require 'tmpdir'

class Unused08PluginExecutionsTest < Minitest::Test
  SCRIPT = File.expand_path('../../audit/script-inputs/audit-unused08-plugin-executions.rb', __dir__)

  def valid_pom
    <<~XML
      <project xmlns="http://maven.apache.org/POM/4.0.0">
        <modelVersion>4.0.0</modelVersion>
        <groupId>test</groupId><artifactId>test</artifactId><version>1.0</version>
        <properties><aoo.unused02.bootstrap.skip>false</aoo.unused02.bootstrap.skip></properties>
        <build><plugins><plugin>
          <artifactId>exec-maven-plugin</artifactId>
          <executions><execution>
            <id>enforce-no-accidental-transitives</id>
            <phase>validate</phase><goals><goal>exec</goal></goals>
            <configuration><skip>${aoo.unused02.bootstrap.skip}</skip><executable>ruby</executable></configuration>
          </execution></executions>
        </plugin></plugins></build>
      </project>
    XML
  end

  def run_audit(pom)
    Dir.mktmpdir('unused08-') do |root|
      scripts = File.join(root, 'engineering', 'audit', 'script-inputs')
      FileUtils.mkdir_p(scripts)
      FileUtils.cp(SCRIPT, File.join(scripts, File.basename(SCRIPT)))
      File.write(File.join(root, 'pom.xml'), pom)
      stdout, stderr, status = Open3.capture3(RbConfig.ruby, File.join(scripts, File.basename(SCRIPT)), chdir: root)
      result_path = File.join(root, 'build-only/audit/unused08-plugin-executions.json')
      result = JSON.parse(File.read(result_path))
      yield root, result, stdout, stderr, status
    end
  end

  def test_exact_bootstrap_exception_is_audited
    run_audit(valid_pom) do |_root, result, stdout, stderr, status|
      assert status.success?, stderr
      assert_equal true, result['passed']
      assert_equal true, result['checks']['bootstrap_contract']
      exception = result['bootstrapExceptions'].fetch(0)
      assert_equal 'aoo.unused02.bootstrap.skip', exception['property']
      assert_equal 'false', exception['defaultValue']
      assert_equal true, exception['wrapperOnly']
      assert_includes stdout, '"passed":true'
    end
  end

  def test_each_near_miss_remains_fail_closed
    cases = {
      alternate_id: ->(xml) { xml.sub('enforce-no-accidental-transitives', 'alternate-id') },
      duplicate_id: ->(xml) { xml.sub('</executions>', '<execution><id>enforce-no-accidental-transitives</id><phase>validate</phase><goals><goal>exec</goal></goals></execution></executions>') },
      missing_goal: ->(xml) { xml.sub('<goals><goal>exec</goal></goals>', '') },
      missing_phase: ->(xml) { xml.sub('<phase>validate</phase>', '') },
      missing_property: ->(xml) { xml.sub('<aoo.unused02.bootstrap.skip>false</aoo.unused02.bootstrap.skip>', '') },
      true_default: ->(xml) { xml.sub('<aoo.unused02.bootstrap.skip>false</aoo.unused02.bootstrap.skip>', '<aoo.unused02.bootstrap.skip>true</aoo.unused02.bootstrap.skip>') },
      duplicate_property: ->(xml) { xml.sub('</properties>', '<aoo.unused02.bootstrap.skip>false</aoo.unused02.bootstrap.skip></properties>') },
      other_dynamic_skip: ->(xml) { xml.sub('${aoo.unused02.bootstrap.skip}', '${other.skip}') },
      literal_skip: ->(xml) { xml.sub('${aoo.unused02.bootstrap.skip}', 'true') },
      profile_override: ->(xml) { xml.sub('</project>', '<profiles><profile><id>bootstrap</id><build><plugins><plugin><artifactId>exec-maven-plugin</artifactId><executions><execution><id>enforce-no-accidental-transitives</id><phase>validate</phase><goals><goal>exec</goal></goals><configuration><skip>${aoo.unused02.bootstrap.skip}</skip></configuration></execution></executions></plugin></plugins></build></profile></profiles></project>') }
    }
    cases.each do |name, mutate|
      run_audit(mutate.call(valid_pom)) do |_root, result, _stdout, _stderr, status|
        refute status.success?, name
        assert_equal false, result['passed'], name
        if name == :duplicate_id
          assert_equal false, result['checks']['unique_ids_per_pom'], name
        else
          refute_empty result['violations'], name
        end
      end
    end
  end
end
