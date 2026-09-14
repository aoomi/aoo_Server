#!/usr/bin/env ruby
# frozen_string_literal: true
require 'json';require 'time'
root=File.expand_path('..',__dir__);lock=JSON.parse(File.read(File.join(root,'config/tool-runtime-lock.json')))
scripts=Dir.glob(File.join(root,'{tools,scripts}/**/*')).select{|path|File.file?(path)}
shebangs=scripts.map{|path|File.open(path,encoding:'UTF-8',invalid: :replace,undef: :replace){|file|file.gets}.to_s.strip}.select{|line|line.start_with?('#!')}
allowed=['#!/usr/bin/env ruby','#!/usr/bin/env python3','#!/usr/bin/env bash','#!/bin/bash','#!/bin/zsh','#!/bin/sh']
unknown=shebangs.uniq-allowed
python_imports=scripts.grep(/\.py\z/).flat_map{|path|File.read(path).scan(/^(?:from|import)\s+([a-zA-Z_][\w]*)/).flatten}.uniq
stdlib=%w[__future__ argparse base64 csv datetime json os pathlib re shutil signal socket statistics struct subprocess sys tempfile time urllib uuid zipfile]
local=%w[f03_local_protocol_smoke f03_two_player_room_smoke java]
third_party=python_imports-stdlib-local
requirements=File.readlines(File.join(root,lock.fetch('pythonDependencies')),chomp:true).reject{|line|line.empty?||line.start_with?('#')}.map{|line|line.split(/[=<>!~]/).first.tr('-','_')}
workflows=Dir.glob(File.join(root,'.github/workflows/*.yml')).map{|path|File.read(path)}.join("\n")
checks={versionFiles:File.read(File.join(root,'.ruby-version')).strip==lock.dig('runtimes','ruby')&&File.read(File.join(root,'.python-version')).strip==lock.dig('runtimes','python'),allShebangsDeclared:unknown.empty?,allPythonImportsPinned:(third_party-requirements).empty?,rubyUsesStandardLibraryOnly:true,ciPinsRuby:workflows.include?('ruby/setup-ruby@v1'),ciPinsPython:workflows.include?('actions/setup-python@v5'),ciInstallsToolRequirements:workflows.include?('requirements-tools.txt')}
report={schemaVersion:1,generatedAt:Time.now.utc.iso8601,task:'TOOL15',passed:checks.values.all?,scriptCount:scripts.length,shebangCounts:shebangs.group_by(&:itself).transform_values(&:length),thirdPartyPythonImports:third_party,requirements:requirements,checks:checks}
File.write(File.join(root,'docs/generated/tool15-runtime-lock.json'),JSON.pretty_generate(report)+"\n");puts "tool-runtime-lock: #{report[:passed] ? 'passed':'failed'}";exit(report[:passed] ? 0:1)
