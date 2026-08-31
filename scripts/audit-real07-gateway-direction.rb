require 'json'; require 'fileutils'
root=File.expand_path('..',__dir__); pom=File.read(File.join(root,'server/Gateway/pom.xml'))
sources=Dir[File.join(root,'server/Gateway/src/main/java/**/*.java')]
text=sources.map{|p|File.read(p)}.join("\n")
deps=pom.scan(%r{<artifactId>([^<]+)</artifactId>}).flatten.drop(1)
concrete=%w[game-category-mahjong game-category-poker game-category-long-card game-category-word-card game-families cdxzmj njpdk scjymj xcpdk zjh zypk games game-server-framework]
checks={depends_only_on_contract_and_transport:(deps&concrete).empty?,spi_dependency:pom.include?('<artifactId>game-spi</artifactId>'),common_dependency:pom.include?('<artifactId>game-common</artifactId>'),no_concrete_imports:!text.match?(/import\s+(?:business\.global\.(?:mj|pk)|com\.aoo\.bcg\.(?:mahjong|poker|families))\./),no_bootstrap_reverse_dependency:!pom.include?('<artifactId>game-bootstrap</artifactId>')}
result={task:'REAL07',passed:checks.values.all?,checks:checks,dependencies:deps,sourceCount:sources.length};out=File.join(root,'work/audit/real07-gateway-direction.json');FileUtils.mkdir_p(File.dirname(out));File.write(out,JSON.pretty_generate(result)+"\n");puts "REAL07 #{result[:passed]?'passed':'failed'}: Gateway depends inward on contracts, never concrete games";exit(result[:passed]?0:1)
