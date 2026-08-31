#!/usr/bin/env ruby
require 'json'; require 'fileutils'
root = File.expand_path('..', __dir__)
properties = File.readlines(File.join(root, '.mvn/wrapper/maven-wrapper.properties'), chomp: true).map { |line| line.split('=', 2) if line.include?('=') }.compact.to_h
expected = 'ed41650d42485cfc243fad22158caf9cbb5dc408ce7a09ddb94dd42a019de929ca43065bfa450612cf12bf78b5cafa3884b96c090de326ff590448c933454af3'
checks = { fixedVersion: properties.fetch('distributionUrl').include?('/3.9.16/'), trustedTlsRepository: properties.fetch('distributionUrl').start_with?('https://repo.maven.apache.org/'), officialSha512: properties['distributionSha512Sum'] == expected, wrapperVersionFixed: properties['wrapperVersion'] == '3.3.4', noFork: !File.exist?(File.join(root, 'mvnw26')) }
report = { schemaVersion: 1, source: 'Apache Maven 3.9.16 official binary SHA-512', properties: properties, checks: checks }
out = File.join(root, 'docs/generated/build01-maven-wrapper.json'); FileUtils.mkdir_p(File.dirname(out)); File.write(out, JSON.pretty_generate(report) + "\n")
abort "BUILD01 failed: #{checks}" unless checks.values.all?
puts 'BUILD01 PASS: wrapper source and checksum pinned'
