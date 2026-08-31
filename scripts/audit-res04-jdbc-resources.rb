require 'json';require 'fileutils';root=File.expand_path('..',__dir__)
tool=File.read(File.join(root,'tools/JdbcResourceSafetyCheck.java'));pom=File.read(File.join(root,'pom.xml'))
checks={connection_acquisition:tool.include?('.getConnection('),statement_result_stream_escape:tool.include?('PreparedStatement|Statement|ResultSet|Stream'),try_with_resources:tool.include?('try-with-resources'),ownership_transfer:tool.include?('JDBC_OWNERSHIP_TRANSFER'),build_gate:pom.include?('enforce-jdbc-resource-safety')}
abort "RES04 audit failed: #{checks}" unless checks.values.all?;out=File.join(root,'work/audit/jdbc-resource-safety.json');FileUtils.mkdir_p(File.dirname(out));File.write(out,JSON.pretty_generate({task:'RES04',status:'passed',checks:checks})+"\n")
