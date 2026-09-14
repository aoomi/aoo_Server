#!/usr/bin/env ruby
# frozen_string_literal: true

require 'digest'
require 'find'

root = File.expand_path('..', __dir__)
included_roots = %w[pom.xml server database/migrations scripts tools].map { |path| File.join(root, path) }
excluded = %r{/(?:target|build|work|logs|node_modules|\.idea|\.vscode)/}
files = []
included_roots.each do |entry|
  if File.file?(entry)
    files << entry
  elsif File.directory?(entry)
    Find.find(entry) { |path| files << path if File.file?(path) && !path.match?(excluded) }
  end
end
digest = Digest::SHA256.new
files.sort.each do |path|
  relative = path.delete_prefix(root + '/')
  digest << relative << "\0" << File.binread(path) << "\0"
end
puts "1.0.0-#{digest.hexdigest[0, 16]}"
