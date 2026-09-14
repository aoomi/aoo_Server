#!/usr/bin/env ruby
require 'json'; require 'rexml/document'; require 'fileutils'
root = File.expand_path('..', __dir__)
text = File.read(File.join(root, '.mvn/settings.xml')); REXML::Document.new(text)
checks = { centralTls: text.scan('https://repo.maven.apache.org/maven2').length >= 3, snapshotsDisabled: text.scan('<snapshots><enabled>false</enabled></snapshots>').length == 2, checksumFail: text.scan('<checksumPolicy>fail</checksumPolicy>').length == 2, immutableReleaseResolution: text.scan('<updatePolicy>never</updatePolicy>').length == 2, noEmbeddedCredentials: text !~ /<username>|<password>|<url>http:\/\//, settingsActivated: File.read(File.join(root, '.mvn/maven.config')).include?('.mvn/settings.xml') }
report = { schemaVersion: 1, settings: '.mvn/settings.xml', checks: checks }
out = File.join(root, 'docs/generated/build02-repository-policy.json'); FileUtils.mkdir_p(File.dirname(out)); File.write(out, JSON.pretty_generate(report) + "\n")
abort "BUILD02 failed: #{checks}" unless checks.values.all?; puts 'BUILD02 PASS: trusted immutable repository policy active'
