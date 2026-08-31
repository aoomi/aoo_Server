require 'json'
require 'fileutils'
require 'digest'
require 'rexml/document'

root = File.expand_path('..', __dir__)
poms = Dir.glob(File.join(root, '**/pom.xml')).reject { |path| path.include?('/target/') }
system_scopes = []
poms.each do |path|
  document = REXML::Document.new(File.read(path))
  REXML::XPath.each(document, '//*[local-name()="dependency" and *[local-name()="scope" and text()="system"]]') do
    system_scopes << path.delete_prefix(root + '/')
  end
end

binary_extensions = %w[.jar .so .dylib .dll .a]
binaries = Dir.glob(File.join(root, '**/*')).select do |path|
  File.file?(path) && binary_extensions.include?(File.extname(path).downcase) &&
    !path.include?('/target/') && !path.include?('/work/')
end.sort
allowed_roots = %w[reference/legacy-2.22/ reference/upstream/ server/gameServer/build/ server/CDXZMJ/build/ server/NJPDK/build/ third-party/]
inventory = binaries.map do |path|
  relative = path.delete_prefix(root + '/')
  classification = relative.start_with?('third-party/') ? 'quarantined-third-party-repository' : '2.22-read-only-reference'
  { path: relative, bytes: File.size(path), sha256: Digest::SHA256.file(path).hexdigest,
    classification: classification, productionReachable: false }
end
unclassified = inventory.reject { |item| allowed_roots.any? { |prefix| item[:path].start_with?(prefix) } }
active_files = poms + Dir.glob(File.join(root, 'tools/start-*.sh')) + [File.join(root, 'tools/runtime-java26.sh')]
forbidden_refs = active_files.each_with_object([]) do |path, refs|
  text = File.binread(path).force_encoding('UTF-8').scrub
  refs << path.delete_prefix(root + '/') if text.match?(%r{(?:reference/legacy-2\.22|server/LegacyCommon/lib|server/gameServer/build|server/(?:CDXZMJ|NJPDK)/build|third-party)/})
end
checks = { no_system_scope: system_scopes.empty?, every_binary_classified_and_hashed: unclassified.empty? && inventory.all? { |i| i[:sha256].size == 64 },
           local_binaries_absent_from_production_build_and_launch: forbidden_refs.empty? }
result = { task: 'UNUSED11', passed: checks.values.all?, checks: checks, binaryCount: inventory.size,
           totalBytes: inventory.sum { |item| item[:bytes] }, systemScopes: system_scopes,
           unclassified: unclassified, productionReferences: forbidden_refs, sbom: inventory }
FileUtils.mkdir_p(File.join(root, 'work/audit'))
File.write(File.join(root, 'work/audit/unused11-local-binary-sbom.json'), JSON.pretty_generate(result))
puts JSON.generate(result.reject { |key, _| key == :sbom })
abort('UNUSED11 failed') unless result[:passed]
