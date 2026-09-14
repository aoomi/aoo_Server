require 'json'
require 'find'
require 'fileutils'
root = File.expand_path('..', __dir__)
trees = [root, File.expand_path('../Client', root)]
skip = %r{/(?:\.git|target|build|work|logs|node_modules|temp|library)(?:/|$)}i
links = []
trees.each do |tree|
  Find.find(tree) do |path|
    if File.directory?(path) && path != tree && path.match?(skip)
      Find.prune
      next
    end
    next unless File.symlink?(path)
    target = File.readlink(path)
    resolved = File.expand_path(target, File.dirname(path))
    inside = trees.any? { |base| resolved == base || resolved.start_with?(base + '/') }
    links << { path: path.delete_prefix(tree + '/'), target: target, resolved: resolved,
      dangling: !File.exist?(resolved), outsideSourceRoots: !inside,
      legacyTarget: !!resolved.match?(%r{/(?:reference|test-move|Legacy[^/]*)(?:/|$)}), selfLoop: resolved == path }
  end
end
bad = links.select { |link| link[:dangling] || link[:outsideSourceRoots] || link[:legacyTarget] || link[:selfLoop] }
result = { task: 'FS08', passed: bad.empty?, symlinks: links, violations: bad }
out = File.join(root, 'docs/generated/fs08-symlinks.json')
FileUtils.mkdir_p(File.dirname(out)); File.write(out, JSON.pretty_generate(result) + "\n")
abort("FS08 unsafe symlinks: #{bad}") unless bad.empty?
