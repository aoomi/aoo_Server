require 'json'
require 'fileutils'
root=File.expand_path('..',__dir__)
manifest=JSON.parse(File.read(File.join(root,'docs/generated/generated-artifact-manifest.json'))).fetch('artifacts')
paths=['server/GameSPI/src/main/java/com/aoo/bcg/gamespi/protocol/GeneratedProtocolIds.java','../Client/assets/Common/Code/Runtime/network/GeneratedProtocolIds.ts']
checks={committed_outputs_registered:paths.all?{|path|manifest.any?{|entry|entry['path']==path&&entry['editPolicy']=='generated-do-not-edit'}},stale_output_gate:system('node',File.join(root,'tools/protocol/check-generated.mjs'),out:File::NULL),maven_validate_gate:File.read(File.join(root,'pom.xml')).include?('enforce-generated-protocol-current')}
checks[:passed]=checks.values.all?; out=File.join(root,'docs/generated/proto06-generated-policy.json');FileUtils.mkdir_p(File.dirname(out));File.write(out,JSON.pretty_generate(checks.merge(strategy:'generated outputs are committed, manifest-owned, and validated against canonical input'))+"\n");abort('PROTO06 generated policy audit failed')unless checks[:passed]
