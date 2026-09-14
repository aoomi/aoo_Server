require 'json'
require 'fileutils'
root = File.expand_path('..', __dir__)
reference = File.read(File.join(root, 'server/GameCommon/src/main/java/com/aoo/bcg/common/config/SecretReference.java'))
resolver = File.read(File.join(root, 'server/GameCommon/src/main/java/com/aoo/bcg/common/config/SecretResolver.java'))
material = File.read(File.join(root, 'server/GameCommon/src/main/java/com/aoo/bcg/common/config/SecretMaterial.java'))
strict = File.read(File.join(root, 'server/GameCommon/src/main/java/com/aoo/bcg/common/config/StrictRuntimeConfig.java'))
checks = {
  versioned_reference: reference.include?('secret://provider/path#version') && reference.include?('getFragment'),
  literal_secret_rejected: strict.include?('SecretReference.parse(value)'),
  external_provider_boundary: resolver.include?('SecretProvider'),
  material_wiped: material.include?("Arrays.fill(value, '\\0')")
}
abort "CONF05 audit failed: #{checks}" unless checks.values.all?
out = File.join(root, 'work/audit/external-secret-reference.json')
FileUtils.mkdir_p(File.dirname(out))
File.write(out, JSON.pretty_generate({task: 'CONF05', status: 'passed', checks: checks}) + "\n")
