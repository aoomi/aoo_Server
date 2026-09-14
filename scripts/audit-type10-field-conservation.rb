#!/usr/bin/env ruby
require 'json'
root=File.expand_path('..',__dir__)
factory=File.read(File.join(root,'server/GameCommon/src/main/java/com/aoo/bcg/common/invite/RoomShareSummaryFactory.java'))
test=File.read(File.join(root,'server/GameCommon/src/test/java/com/aoo/bcg/common/invite/RoomShareSummaryFactoryTest.java'))
checks={'exact_target_schema'=>factory.include?('requireExactRecordFields'),'sensitive_denylist'=>factory.include?('requireNoSensitiveFields'),'no_silent_defaults'=>test.include?('rejectsMissingFieldsInsteadOfInventingDefaults'),'privacy_test'=>test.include?('sensitiveSourceFieldsNeverEnterProjection'),'schema_drift_test'=>test.include?('contractDetectsAddedOrForgottenTargetFields')}
out={'task'=>'TYPE10','passed'=>checks.values.all?,'checks'=>checks}
path=File.join(root,'work/audit/mapper-field-conservation.json');File.write(path,JSON.pretty_generate(out)+"\n");abort JSON.generate(out) unless out['passed'];puts JSON.generate(out)
