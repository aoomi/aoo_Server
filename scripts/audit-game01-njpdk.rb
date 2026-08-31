require 'json'
require 'rexml/document'
root=File.expand_path('..',__dir__)
report=File.join(root,'server/NJPDK/target/surefire-reports/TEST-business.global.pk.njpdk.NJPDKGameProviderTest.xml')
passed=File.file?(report)&&REXML::Document.new(File.read(report)).root.attributes['failures'].to_i.zero?&&REXML::Document.new(File.read(report)).root.attributes['errors'].to_i.zero?
round=File.read(File.join(root,'server/NJPDK/src/business/global/pk/njpdk/NJPDKRoomSetRound.java'))
out={'task'=>'GAME01','status'=>'blocked-real-three-player-set-end','completed'=>{'moduleBuildAndProviderTest'=>passed,'authoritativeOperationValidation'=>round.include?('checkIsMyCard'),'trusteeOperationPath'=>round.include?('机器人托管'),'setEndDetection'=>round.include?('m_bSetEnd')} ,'unresolved'=>['no repeatable real three-player full-round test proves trustee progression through SNJPDK_SetEnd','previous real three-client run timed out after initial opPos synchronization']}
File.write(File.join(root,'docs/generated/game01-njpdk-vertical.json'),JSON.pretty_generate(out)+"\n")
puts'GAME01 evidence recorded'
