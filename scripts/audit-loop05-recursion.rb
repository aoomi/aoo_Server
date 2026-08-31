require 'json';require 'fileutils'
root=File.expand_path('..',__dir__);modern=File.read(File.join(root,'server/Mahjong/src/main/java/com/aoo/bcg/mahjong/LaiZiMahjongWinDetector.java'));decoder=File.read(File.join(root,'server/GameCommon/src/main/java/com/aoo/bcg/common/serialization/DomainJsonDecoder.java'));legacy=[]
Dir.glob(File.join(root,'server/**/*.java')).reject{|path|path.include?('/target/')||path.include?('/build/')||path.include?('/src/main/java/com/aoo/')}.each do |path|
 body=File.binread(path).force_encoding('UTF-8').scrub
 declarations=body.scan(/\b(?:public|private|protected)\s+(?:static\s+)?[\w<>, ?\[\]]+\s+(\w+)\s*\([^)]*\)\s*\{/).flatten
 declarations.select{|name|name.match?(/(?:recurs|findAndAdd|fileCopy|meld|search|split|chai|hu)/i)}.each{|name|legacy<<{path:path.sub(root+'/',''),method:name} if body.scan(/\b#{Regexp.escape(name)}\s*\(/).size>1&&!body.match?(/(?:maxDepth|maximumDepth|visited|hierarchy\s*[+]|depth\s*[+])/)}
end
checks={mahjong_depth_budget:modern.include?('budget.enter()'),mahjong_visited_set:modern.include?('visited.add'),serialization_depth_limit:decoder.include?('MAX_DEPTH')};out=File.join(root,'work/audit/loop05-recursion.json');FileUtils.mkdir_p(File.dirname(out));File.write(out,JSON.pretty_generate({task:'LOOP05',status:checks.values.all? ? 'partial':'failed',checks:checks,legacyRecursiveMethodsWithoutVisibleGuard:legacy.uniq})+"\n");abort 'LOOP05 modern recursion guards failed' unless checks.values.all?
