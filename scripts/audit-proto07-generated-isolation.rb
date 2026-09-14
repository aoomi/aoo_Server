require 'json'
require 'fileutils'
root=File.expand_path('..',__dir__)
generated='server/GameSPI/src/main/java/com/aoo/bcg/gamespi/protocol/GeneratedProtocolIds.java'
allowed=['server/LegacyCommon/src/com/ddm/server/protocol/v2/ProtocolRegistry.java','server/LegacyCommon/src/com/ddm/server/protocol/v2/ProtocolV2Bridge.java']
consumers=Dir.glob(File.join(root,'server/**/*.java')).select{|path|File.read(path).include?('GeneratedProtocolIds')}.map{|path|path.delete_prefix(root+'/')}.reject{|path|path==generated}
root_pom=File.read(File.join(root,'pom.xml'))
checks={generated_header:File.readlines(File.join(root,generated),chomp:true).first.include?('Do not edit'),adapter_only_consumers:(consumers-allowed).empty?,legacy_commdef_outside_active_reactor:!root_pom.include?('<module>server/LegacyAccountServer</module>'),manifest_owned:JSON.parse(File.read(File.join(root,'docs/generated/generated-artifact-manifest.json'))).fetch('artifacts').any?{|e|e['path']==generated&&e['editPolicy']=='generated-do-not-edit'}}
checks[:passed]=checks.values.all?;out=File.join(root,'docs/generated/proto07-generated-isolation.json');FileUtils.mkdir_p(File.dirname(out));File.write(out,JSON.pretty_generate(checks.merge(consumers:consumers,allowedAdapters:allowed))+"\n");abort('PROTO07 generated isolation audit failed')unless checks[:passed]
