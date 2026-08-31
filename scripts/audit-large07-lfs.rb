#!/usr/bin/env ruby
# frozen_string_literal: true
require 'digest';require 'json';require 'open3';require 'time'
root=File.expand_path('../..',__dir__);server=File.join(root,'Server');extensions=%w[.png .jpg .jpeg .gif .webp .mp3 .wav .ogg .ttf .otf .jar .zip .bin]
paths=Dir.glob(File.join(root,'{Client,Server,Admin}/**/*')).select{|path|File.file?(path)&&extensions.include?(File.extname(path).downcase)&&File.size(path)>=5*1024*1024}.reject{|path|path.match?(%r{/(?:target|build|work|reference|library|temp|node_modules)/})}.sort
git_out,_git_err,git_status=Open3.capture3('git','-C',root,'rev-parse','--show-toplevel');lfs_out,_lfs_err,lfs_status=Open3.capture3('git','lfs','version');attributes=File.file?(File.join(root,'.gitattributes')) ? File.read(File.join(root,'.gitattributes')):''
rows=paths.map{|path|{path:path.delete_prefix(root+'/'),bytes:File.size(path),sha256:Digest::SHA256.file(path).hexdigest}}
checks={gitRepository:git_status.success?,gitLfsInstalled:lfs_status.success?,lfsPatternsDeclared:attributes.include?('filter=lfs'),largeBinaryInventory:!rows.empty?,cleanCheckoutIntegrityProof:false}
report={schemaVersion:1,generatedAt:Time.now.utc.iso8601,task:'LARGE07',passed:checks.values.all?,thresholdBytes:5*1024*1024,repository:git_out.strip,lfsVersion:lfs_out.strip,largeBinaries:rows,checks:checks,blockers:['Aoo directory is not a Git worktree in the supplied workspace','git-lfs is not available','adding LFS attributes without pointer migration and clean-clone verification would corrupt checkout expectations']}
File.write(File.join(server,'docs/generated/large07-lfs.json'),JSON.pretty_generate(report)+"\n");puts "LARGE07 FAIL binaries=#{rows.length}";exit 1
