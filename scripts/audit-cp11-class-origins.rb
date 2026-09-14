#!/usr/bin/env ruby
# frozen_string_literal: true

require 'json'
require 'open3'
require 'time'

ROOT = File.expand_path('..', __dir__)
OUTPUT = File.join(ROOT, 'docs/generated/cp11-class-origins.json')
TASKS = File.join(ROOT, 'docs/Aoo-前后端全框架功能通信审计任务清单.md')
MANIFEST = File.join(ROOT, 'docs/generated/generated-artifact-manifest.json')
CLASSES = %w[
  com.aoo.bcg.gamespi.GameProvider
  com.aoo.bcg.gamespi.GameRegistry
  business.global.mj.cdxzmj.CDXZMJGameProvider
  business.global.pk.njpdk.NJPDKGameProvider
  core.network.client2game.handler.room.CPlayerRoomReconnectV2
  com.fasterxml.jackson.databind.ObjectMapper
  com.google.protobuf.Message
  com.mysql.cj.jdbc.Driver
  io.netty.channel.ChannelHandler
].freeze

bootstrap_jars = Dir[File.join(ROOT, 'server/Bootstrap/target/*.jar')].reject { |path| path.end_with?('-sources.jar', '-javadoc.jar') }
bootstrap_jar = bootstrap_jars.max_by { |path| File.mtime(path) }
classpath = "#{bootstrap_jar}:#{File.join(ROOT, 'server/Bootstrap/target/runtime/lib/*')}"
jdk = File.expand_path('../.toolchains/jdk-26.0.2.1.jdk/Contents/Home', ROOT)
stdout, stderr, status = Open3.capture3({ 'JAVA_HOME' => jdk, 'PATH' => "#{jdk}/bin:#{ENV.fetch('PATH')}" }, File.join(jdk, 'bin/java'), '-cp', classpath, File.join(ROOT, 'tools/ClassOriginDiagnostics.java'), *CLASSES, chdir: ROOT)
diagnostics = stdout.empty? ? { 'passed' => false, 'classes' => [] } : JSON.parse(stdout)
diagnostics.fetch('classes').each do |row|
  row['codeSource'] = row.fetch('codeSource').gsub(ROOT, '${PROJECT_ROOT}')
  row['resources'] = row.fetch('resources').map { |value| value.gsub(ROOT, '${PROJECT_ROOT}') }
end
checks = {
  diagnosticCommandPassed: status.success?,
  allCriticalClassesLoaded: diagnostics.fetch('classes').size == CLASSES.size,
  everyClassHasOneResource: diagnostics.fetch('classes').all? { |row| row.fetch('resourceCount') == 1 },
  noProjectClassFromLocalRepository: diagnostics.fetch('classes').reject { |row| row.fetch('className').start_with?('com.fasterxml', 'com.google', 'com.mysql', 'io.netty') }.none? { |row| row.fetch('codeSource').include?('/.m2/repository/com/aoo/bcg/') }
}
report = {
  schemaVersion: 1,
  generatedAt: Time.now.utc.iso8601,
  task: 'CP11',
  commandClasspathContract: 'game-bootstrap.jar:game-bootstrap/target/runtime/lib/*',
  checks: checks,
  classes: diagnostics.fetch('classes'),
  stderr: stderr.strip,
  passed: checks.values.all?
}
File.write(OUTPUT, JSON.pretty_generate(report) + "\n", mode: 'w:UTF-8')

tasks = File.read(TASKS, encoding: 'UTF-8')
status_text = report[:passed] ? '已完成' : '失败跳过'
result = report[:passed] ? '通过' : '未完成'
detail = report[:passed] ? "已提供统一 ClassOriginDiagnostics，并在生产 Bootstrap 类路径实际加载 #{CLASSES.size} 个关键 SPI、Provider、房间 Handler、Jackson/Protobuf 序列化器、MySQL 驱动及 Netty 接口；每类仅一个资源来源，项目类不从本地仓库阴影加载。" : "类来源诊断未通过：#{checks.reject { |_name, passed| passed }.keys.join('、')}，已记录后跳过。"
tasks.sub!(/^\| CP11 \|.*$/, "| CP11 | #{status_text} | 类来源诊断 | #{result} | #{detail} 证据：docs/generated/cp11-class-origins.json、tools/ClassOriginDiagnostics.java |")
File.write(TASKS, tasks, mode: 'w:UTF-8')

manifest = JSON.parse(File.read(MANIFEST, encoding: 'UTF-8'))
path = 'docs/generated/cp11-class-origins.json'
unless manifest.fetch('artifacts').any? { |artifact| artifact['path'] == path }
  manifest.fetch('artifacts') << {
    'path' => path,
    'kind' => 'generated-classpath-evidence',
    'authoritativeInputs' => ['pom.xml', 'server/Bootstrap/pom.xml', 'tools/ClassOriginDiagnostics.java'],
    'generator' => 'scripts/audit-cp11-class-origins.rb',
    'rebuild' => './mvnw -Dexec.skip=true -DskipTests package && ruby scripts/audit-cp11-class-origins.rb',
    'owner' => 'build-and-classpath',
    'editPolicy' => 'generated-do-not-edit'
  }
end
File.write(MANIFEST, JSON.pretty_generate(manifest) + "\n", mode: 'w:UTF-8')
puts JSON.generate(report.slice(:task, :checks, :passed))
exit 1 unless report[:passed]
