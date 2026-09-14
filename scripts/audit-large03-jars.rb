#!/usr/bin/env ruby
# frozen_string_literal: true
require 'digest';require 'json';require 'time'
root=File.expand_path('..',__dir__);paths=Dir.glob(File.join(root,'**/*.jar')).reject{|path|path.include?('/.git/')}.sort
rows=paths.select{|path|File.size(path)>=5*1024*1024}.map do|path|
 relative=path.delete_prefix(root+'/');zone=case relative when %r{\Aserver/[^/]+/target/} then 'maven-output' when %r{\Awork/} then 'work-evidence-or-quarantine' when %r{\Areference/} then 'legacy-reference' when %r{/build/} then 'legacy-build-output' else 'unclassified' end
 {path:relative,bytes:File.size(path),sha256:Digest::SHA256.file(path).hexdigest,zone:zone}
end
provenance=JSON.parse(File.read(File.join(root,'docs/generated/pkg03-jar-provenance.json')));duplicates=JSON.parse(File.read(File.join(root,'docs/generated/pkg05-duplicate-jar-classes.json')));strategy=JSON.parse(File.read(File.join(root,'docs/generated/pkg09-package-strategy.json')));release=JSON.parse(File.read(File.join(root,'docs/generated/pkg08-release-inputs.json')))
checks={allLargeJarsClassified:rows.none?{|row|row[:zone]=='unclassified'},releaseUsesReactorOnly:release['passed']==true,thinJarStrategy:strategy['passed']==true,allLicensesKnown:provenance['allLicenses']==true,noDuplicateRuntimeClasses:duplicates['passed']==true}
report={schemaVersion:1,generatedAt:Time.now.utc.iso8601,task:'LARGE03',passed:checks.values.all?,largeJarThresholdBytes:5*1024*1024,largeJars:rows,zoneCounts:rows.group_by{|row|row[:zone]}.transform_values(&:length),checks:checks,blockers:['82 JAR components lack declared license metadata','runtime dependency analysis still reports duplicate classes; platform-native and shaded collisions require per-artifact disposition']}
File.write(File.join(root,'docs/generated/large03-jars.json'),JSON.pretty_generate(report)+"\n");puts "LARGE03 FAIL large=#{rows.length}";exit 1
