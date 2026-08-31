#!/usr/bin/env ruby
# frozen_string_literal: true

require "json"
require "fileutils"

root = File.expand_path("..", __dir__)
canonical = File.expand_path("../.toolchains/jdk-26.0.2.1.jdk/Contents/Home", root)
archive = File.expand_path("../.toolchains/reference-work-toolchains-20260823", root)
runtime_script = File.read(File.join(root, "tools", "runtime-java26.sh"))
poms = Dir[File.join(root, "{pom.xml,server/**/pom.xml}")].select { |path| File.file?(path) }
pom_text = poms.map { |path| File.read(path) }.join("\n")
checks = {
  repositoryToolchainsAbsent: !File.exist?(File.join(root, "work", "toolchains")),
  canonicalJava26Present: File.file?(File.join(canonical, "release")) && File.read(File.join(canonical, "release")).include?('JAVA_VERSION="26.0.2.1"'),
  historicalToolchainsRetainedOutsideRepository: File.directory?(archive),
  runtimeUsesExternalCanonicalToolchain: runtime_script.include?('../.toolchains/jdk-26.0.2.1.jdk/Contents/Home'),
  runtimeSupportsJavaHomeOverride: runtime_script.include?('AOO_JAVA_HOME'),
  compilerBaselineJava25: pom_text.include?('<maven.compiler.release>25</maven.compiler.release>'),
  noPomLocalToolchainReference: !pom_text.include?('work/toolchains')
}
report = {
  task: "HYGIENE04",
  status: checks.values.all? ? "passed" : "failed",
  checks: checks,
  compileRelease: 25,
  validationRuntime: "Oracle JDK 26.0.2.1",
  archivedReference: "../.toolchains/reference-work-toolchains-20260823"
}
FileUtils.mkdir_p(File.join(root, "work", "audit"))
File.write(File.join(root, "work", "audit", "hygiene04-toolchains.json"), JSON.pretty_generate(report) + "\n")
puts JSON.generate(report)
exit(checks.values.all? ? 0 : 2)
