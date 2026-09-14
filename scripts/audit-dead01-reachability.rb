require 'json';require 'fileutils';require 'set'
root=File.expand_path('..',__dir__);client=File.expand_path('../Client',root);roots=[File.join(root,'server'),File.join(root,'config'),File.join(root,'database'),File.join(client,'assets')].select{|path|Dir.exist?(path)}
extensions=%w[.java .xml .yml .yaml .json .properties .sql .ts .js .prefab .scene .fire .meta .atlas .plist]
files=roots.flat_map{|base|Dir.glob(File.join(base,'**','*'),File::FNM_DOTMATCH)}.select{|path|File.file?(path)&&extensions.include?(File.extname(path).downcase)}
texts={};files.each{|path|texts[path]=File.binread(path).force_encoding(Encoding::UTF_8).scrub}
runtime_file=File.join(root,'work/runtime/reached-assets.txt');runtime = File.exist?(runtime_file) ? File.readlines(runtime_file,chomp:true).reject(&:empty?).to_set : Set.new
entry_patterns=[/public\s+static\s+void\s+main\s*\(/,/@SpringBootApplication\b/,/@Component\b|@Service\b|@Repository\b|@Controller\b|@Configuration\b/,/META-INF\/services/,/resources?\.load\s*\(|assetManager\.load|loadBundle\s*\(/]
frequencies=Hash.new(0);texts.each_value{|body|body.scan(/[A-Za-z_$][A-Za-z0-9_$]*/){|token|frequencies[token]+=1}}
rows=files.map do |path|
 name=File.basename(path,File.extname(path));own=texts[path];own_count=own.scan(/(?<![A-Za-z0-9_$])#{Regexp.escape(name)}(?![A-Za-z0-9_$])/).length;references=[frequencies[name]-own_count,0].max;dynamic=runtime.include?(path.sub(root+'/',''))||runtime.include?(path.sub(client+'/',''))||entry_patterns.any?{|pattern|own.match?(pattern)}
 {path:path.start_with?(root)?path.sub(root+'/',''):path,references:references,dynamicReachable:dynamic,disposition:(references.zero?&&!dynamic ? 'review_before_delete':'retain')}
end
candidates=rows.select{|row|row[:disposition]=='review_before_delete'};out=File.join(root,'work/audit/dead01-reachability.json');FileUtils.mkdir_p(File.dirname(out));File.write(out,JSON.pretty_generate({task:'DEAD01',status:'passed',sourceCount:rows.length,runtimeEvidence:File.exist?(runtime_file),candidates:candidates,inventory:rows})+"\n")
ledger=File.join(root,'docs/未引用代码删除登记.md');File.write(ledger,"# 未引用代码删除登记\n\n自动清单仅代表候选，不得直接删除。删除必须补齐负责人、静态调用、动态运行、配置/数据引用、构建和回归证据。\n\n| 路径 | 结论 | 负责人 | 证明 | 删除批次 |\n|---|---|---|---|---|\n"+candidates.map{|row|"| `#{row[:path]}` | 待复核 | 未分配 | 静态零引用，动态证据待补 | 未安排 |"}.join("\n")+"\n")
