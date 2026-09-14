#!/usr/bin/env ruby
require 'json';require 'open3';require 'fileutils'
root=File.expand_path('..',__dir__); path=File.join(root,'server/GameCommon/src/main/java/com/aoo/bcg/common/repair/VersionedRepairGuard.java');source=File.read(path)
jdk=File.expand_path('../.toolchains/jdk-26.0.2.1.jdk/Contents/Home',root);env={'JAVA_HOME'=>jdk,'PATH'=>"#{jdk}/bin:#{ENV['PATH']}"}
out,err,status=Open3.capture3(env,'./mvnw','-pl','server/GameCommon','-am','-Dtest=VersionedRepairGuardTest','-Dsurefire.failIfNoSpecifiedTests=false','test','-q',chdir:root)
checks={'expected_version'=>source.include?('expectedVersion'),'locked_read'=>source.include?('lockedVersionReader'),'conflict_rejected'=>source.include?('StaleRepairException'),'version_advances'=>source.include?('after <= actual'),'tests_passed'=>status.success?}
result={'task'=>'REPAIR01','passed'=>checks.values.all?,'checks'=>checks,'testStdout'=>out.strip,'testStderr'=>err.strip};evidence=File.join(root,'work/audit/repair-compare-version.json');FileUtils.mkdir_p(File.dirname(evidence));File.write(evidence,JSON.pretty_generate(result)+"\n");puts JSON.generate(result);exit(result['passed'] ? 0 : 1)
