#!/usr/bin/env ruby
require 'json'
require 'yaml'
require 'fileutils'

root = File.expand_path('../../..', __dir__)
manifest_path = File.join(root, 'deploy/production-services.yaml')

abort "DRIFT07 failed: manifest missing: #{manifest_path}" unless File.file?(manifest_path)

begin
  catalog = YAML.safe_load(File.read(manifest_path), permitted_classes: [], aliases: false)
rescue Psych::Exception => error
  abort "DRIFT07 failed: invalid production service catalog: #{error.message}"
end

abort 'DRIFT07 failed: production service catalog must be a mapping' unless catalog.is_a?(Hash)

current_services = catalog['services']
compatibility_workloads = catalog['compatibilityWorkloads']
abort 'DRIFT07 failed: services must be an array' unless current_services.is_a?(Array)
abort 'DRIFT07 failed: compatibilityWorkloads must be an array' unless compatibility_workloads.is_a?(Array)

current_service_specs = {
  'account' => [904],
  'hall' => [9998, 9888],
  'njpdk' => [9996, 9886]
}.freeze

current_names = current_services.map { |service| service.is_a?(Hash) ? service['name'] : nil }

service_ports = lambda do |entry|
  ports = entry.is_a?(Hash) ? entry['ports'] : nil
  return nil unless ports.is_a?(Array)

  ports.map { |port| port.is_a?(Hash) ? port['containerPort'] : nil }
end

current_shape_ok = current_names.sort == current_service_specs.keys.sort
current_entries_valid = current_services.all? do |service|
  name = service.is_a?(Hash) ? service['name'] : nil
  expected_ports = current_service_specs[name]
  expected_ports && service_ports.call(service)&.sort == expected_ports.sort &&
    service['readiness'].is_a?(Hash) && service['resources'].is_a?(Hash) &&
    service['resources']['cpuLimit'] && service['resources']['memoryLimit']
end

compatibility_entry = compatibility_workloads.find do |workload|
  workload.is_a?(Hash) && workload['name'] == 'cdxzmj'
end

compatibility_contract_ok = compatibility_workloads.count { |workload| workload.is_a?(Hash) && workload['name'] == 'cdxzmj' } == 1 &&
  compatibility_entry.is_a?(Hash) &&
  compatibility_entry['mode'] == 'compatibility' &&
  compatibility_entry['currentProduction'] == false &&
  compatibility_entry['render'] == false &&
  compatibility_entry['recoveryOnly'] == true &&
  compatibility_entry['admission'] == 'deny-current-production' &&
  compatibility_entry['allowedGameIds'] == [516] &&
  compatibility_entry['allowedPlayVersions'] == ['legacy-equivalent-1']

external_facilities = Array(catalog['externalFacilities'])
managed_facilities = Array(catalog['managedFacilities'])
managed_facility_names = managed_facilities.map do |facility|
  facility.is_a?(Hash) ? facility['name'] : nil
end.compact

checks = {
  currentServicesDeclared: current_shape_ok && current_entries_valid,
  currentServicesExcludeCompatibility: !current_names.include?('cdxzmj') && !current_names.include?(516),
  compatibilityWorkloadContract: compatibility_contract_ok,
  currentHealthAndReadiness: current_services.all? { |service| service.is_a?(Hash) && service['readiness'].is_a?(Hash) },
  currentResourceLimits: current_services.all? do |service|
    resources = service.is_a?(Hash) ? service['resources'] : nil
    resources.is_a?(Hash) && resources['memoryLimit'] && resources['cpuLimit']
  end,
  externalFacilities: %w[mysql redis rocketmq].all? { |name| external_facilities.include?(name) } && managed_facility_names.include?('mongodb')
}

report = {
  schemaVersion: 2,
  manifest: 'deploy/production-services.yaml',
  currentServices: current_services.map do |service|
    {
      name: service['name'],
      ports: service_ports.call(service),
      readiness: service['readiness'],
      resources: service['resources']
    }
  end,
  compatibilityWorkloads: compatibility_workloads.map do |workload|
    {
      name: workload['name'],
      mode: workload['mode'],
      currentProduction: workload['currentProduction'],
      render: workload['render'],
      recoveryOnly: workload['recoveryOnly'],
      admission: workload['admission'],
      allowedGameIds: workload['allowedGameIds'],
      allowedPlayVersions: workload['allowedPlayVersions']
    }
  end,
  checks: checks
}

out = File.join(root, 'docs/generated/drift07-deployment-reconciliation.json')
FileUtils.mkdir_p(File.dirname(out))
File.write(out, JSON.pretty_generate(report) + "\n")
abort "DRIFT07 failed: #{checks}" unless checks.values.all?
puts 'DRIFT07 PASS: current and compatibility deployment contracts reconciled'
