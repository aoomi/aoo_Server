#!/usr/bin/env ruby
require 'json';require 'pathname'
root=Pathname(__dir__).join('..').expand_path;doc=root.join('docs/必要插件产物追踪.md');text=doc.read
artifact_report=root.join('work/audit/unused27-artifact-comparison.json');artifact_passed=artifact_report.exist?&&JSON.parse(artifact_report.read)['status']=='passed'
plugins=['Lombok annotation processor','build-helper-maven-plugin','maven-dependency-plugin','maven-resources-plugin','maven-jar-plugin','exec-maven-plugin audit suite','Cocos Creator 3.8.8 asset importer','jprotobuf runtime schema']
columns=['输入','生成/发布产物','消费方','删除影响','保护方式']
pom=root.join('pom.xml').read;game_pom=root.join('server/gameServer/pom.xml').read
checks={allArtifactsTracked:plugins.all?{|v|text.include?(v)},completeLineage:columns.all?{|v|text.include?(v)},annotationProcessorConfigured:pom.include?('<annotationProcessorPaths>'),sharedSourceExecution:game_pom.include?('<id>shared-sources</id>'),currentArtifactsPresent:Dir[root.join('server/**/target/*.jar').to_s].length>=20||artifact_passed,creatorOutputPresent:Dir[root.join('../Client/build/web-desktop/**/*').expand_path.to_s].any?}
errors=checks.reject{|_,v|v}.keys;report={task:'UNUSED32',status:errors.empty? ? 'passed':'failed',checks:checks,trackedPlugins:plugins,errors:errors};out=root.join('work/audit/unused32-plugin-artifacts.json');out.write(JSON.pretty_generate(report)+"\n");abort("UNUSED32 failed: #{errors.join(', ')}")unless errors.empty?;puts "UNUSED32 passed: #{plugins.length} build/import artifact chains tracked"
