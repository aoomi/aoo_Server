require 'json'; require 'fileutils'
root=File.expand_path('..',__dir__)
reactor=File.read(File.join(root,'pom.xml')).scan(%r{<module>(server/[^<]+)</module>}).flatten
manifest_mains=[]; app_mains=[]
reactor.each do |mod|
  pom=File.join(root,mod,'pom.xml')
  manifest_mains << [mod,File.read(pom)[%r{<mainClass>([^<]+)</mainClass>},1]] if File.exist?(pom) && File.read(pom).include?('<mainClass>')
  Dir[File.join(root,mod,'src/**/*.java')].each do |path|
    name=File.basename(path,'.java'); text=File.read(path)
    app_mains << path.delete_prefix(root+'/') if name.match?(/(?:APP|App|Application|Server|Bootstrap)$/) && text.match?(/public\s+static\s+void\s+main\s*\(/)
  end
end
allowed_manifest=[['server/Bootstrap','com.aoo.bcg.bootstrap.BootstrapAPP'],['server/AdminApi','com.aoo.bcg.admin.AdminApiApplication'],['server/Gateway','com.aoo.bcg.gateway.GatewayApplication']]
allowed_sources=['server/Bootstrap/src/main/java/com/aoo/bcg/bootstrap/BootstrapAPP.java','server/AdminApi/src/main/java/com/aoo/bcg/admin/AdminApiApplication.java','server/Gateway/src/main/java/com/aoo/bcg/gateway/GatewayApplication.java']
checks={bootstrap_manifest_unique_for_player_plane:(manifest_mains-allowed_manifest).empty?,control_plane_only_exception:(manifest_mains-allowed_manifest).empty?,no_old_app_main:(app_mains-allowed_sources).empty?,bootstrap_routes_hall_and_games:File.read(File.join(root,'server/Bootstrap/src/main/java/com/aoo/bcg/bootstrap/BootstrapAPP.java')).include?('HallApplication.start')}
result={task:'REAL06',passed:checks.values.all?,checks:checks,manifestMains:manifest_mains,applicationMains:app_mains};out=File.join(root,'work/audit/real06-bootstrap-entry.json');FileUtils.mkdir_p(File.dirname(out));File.write(out,JSON.pretty_generate(result)+"\n");puts "REAL06 #{result[:passed]?'passed':'failed'}: Bootstrap is sole player-plane assembly entry";exit(result[:passed]?0:1)
