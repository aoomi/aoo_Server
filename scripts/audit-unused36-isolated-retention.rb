#!/usr/bin/env ruby
require 'json';require 'pathname';require 'open3'
root=Pathname(__dir__).join('..').expand_path
stdout,stderr,status=Open3.capture3('ruby',root.join('tools/audit_legacy_build_isolation.rb').to_s,chdir:root.to_s)
legacy=JSON.parse(root.join('work/audit/legacy-build-isolation.json').read);local=JSON.parse(root.join('work/audit/unused11-local-binary-sbom.json').read)
reference_jars=Dir[root.join('reference/legacy-2.22/**/*.jar').to_s]
checks={legacyGateExecuted:status.success?,noBundledJarInProductionTree:legacy.dig('summary','bundledJarsOutsideBuildOutput')==0,noLegacyCoreSource:legacy.dig('findings','legacyCoreSources')==[],noPomLegacyReachability:legacy.dig('summary','pomsReachingLegacyRepositoryOrKernel')==0,referenceArtifactsRetained:reference_jars.length>=100,productionClasspathRejectsReference:Array(local['productionReferences']).empty?,readOnlyPolicy:root.join('reference/legacy-2.22/README.md').read.include?('生产运行时不加载')}
errors=checks.reject{|_,v|v}.keys;report={task:'UNUSED36',status:errors.empty? ? 'passed':'failed',checks:checks,referenceJarCount:reference_jars.length,isolatedSourceCount:Array(legacy.dig('findings','isolatedLegacySources')).length,toolOutput:stdout.strip,toolError:stderr.strip,errors:errors};root.join('work/audit/unused36-isolated-retention.json').write(JSON.pretty_generate(report)+"\n");abort("UNUSED36 failed: #{errors.join(', ')}")unless errors.empty?;puts "UNUSED36 passed: #{reference_jars.length} legacy jars quarantined and production reachability is zero"
