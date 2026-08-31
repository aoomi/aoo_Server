require 'json'
require 'fileutils'
root=File.expand_path('..',__dir__)
source=File.read(File.join(root,'server/GameSPI/src/main/java/com/aoo/bcg/gamespi/api/ApiDeprecationRegistry.java'))
test=File.read(File.join(root,'server/GameSPI/src/test/java/com/aoo/bcg/gamespi/api/ApiDeprecationRegistryTest.java'))
checks={reason:source.include?('String reason'),replacement:source.include?('String replacement'),usage:source.include?('usageMetricName'),sunset:source.include?('Instant sunsetAt'),closed_loop:source.include?('sunset&&calls==0'),executable_test:test.include?('removalRequiresReasonReplacementObservedZeroUsageAndSunset')}
abort "LIFE03 audit failed: #{checks}" unless checks.values.all?
out=File.join(root,'work/audit/api-deprecation-closure.json');FileUtils.mkdir_p(File.dirname(out));File.write(out,JSON.pretty_generate({task:'LIFE03',status:'passed',checks:checks})+"\n")
