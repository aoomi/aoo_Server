#!/usr/bin/env ruby
# frozen_string_literal: true
require 'digest';require 'json';require 'time'
root=File.expand_path('..',__dir__);client=File.expand_path('../Client',root)
paths=Dir.glob(File.join(root,'**/roomcost.json'))+Dir.glob(File.join(client,'**/roomcost.json'))
paths.reject!{|path|path.include?('/build/')||path.include?('/library/')||path.include?('/temp/')};paths.uniq!
server_source=File.join(root,'server/LegacyCommon/conf/jsonData/roomcost.json');client_source=File.join(client,'assets/Common/Config/Legacy/assets/jsonData/roomcost.json')
rows=paths.sort.map{|path|{path:path.start_with?(root) ? path.delete_prefix(root+'/'):path,bytes:File.size(path),sha256:Digest::SHA256.file(path).hexdigest,generated:path.include?('/work/local-runtime/'),client:path.start_with?(client),activeSource:[server_source,client_source].include?(path)}}
legacy=File.read(File.join(root,'server/gameServer/src/core/config/refdata/ref/RefRoomCost.java'));billing=Dir.glob(File.join(root,'server/Billing/src/main/**/*.java')).map{|path|File.read(path)}.join("\n")
checks={inventoryComplete:rows.length>=2,activeCopiesDeduplicated:false,databaseBackedAuthority:false,legacyServerValidationPresent:legacy.include?('GetCost(')&&legacy.include?('NotEnough_RoomCost_Error'),modernBillingAuthorityPresent:billing.include?('CurrencyAuthority')||billing.include?('Billing'),clientAndServerHashEqual:Digest::SHA256.file(server_source)==Digest::SHA256.file(client_source),runtimeCopiesGeneratedOnly:rows.reject{|row|row[:activeSource]}.all?{|row|row[:generated]}}
report={schemaVersion:1,generatedAt:Time.now.utc.iso8601,task:'LARGE02',passed:checks.values.all?,files:rows,uniqueHashes:rows.map{|row|row[:sha256]}.uniq.length,totalBytes:rows.sum{|row|row[:bytes]},checks:checks,blockers:['Legacy RefDataMgr still loads the 5 MB server JSON','Client source retains a full display copy instead of a compact server-issued pricing view','No database/config-center room-cost publication table is the sole authority']}
File.write(File.join(root,'docs/generated/large02-roomcost.json'),JSON.pretty_generate(report)+"\n");puts "LARGE02 FAIL copies=#{rows.length} unique=#{report[:uniqueHashes]}";exit 1
