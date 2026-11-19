#!/usr/bin/env ruby
require 'fileutils'
require 'tmpdir'

path = ARGV.fetch(0)
sheet_name = ARGV.fetch(1, '程序自动维护，请勿修改')
Dir.mktmpdir('aoo-xlsx-hide-') do |directory|
  abort('无法解压工作簿') unless system('unzip', '-q', path, '-d', directory)
  workbook = File.join(directory, 'xl/workbook.xml')
  xml = File.read(workbook, encoding: 'UTF-8')
  pattern = /(<(?:\w+:)?sheet\b[^>]*\bname="#{Regexp.escape(sheet_name)}"[^>]*)(\/>)/
  abort("找不到技术工作表: #{sheet_name}") unless xml.match?(pattern)
  xml = xml.sub(pattern) do
    prefix = Regexp.last_match(1)
    ending = Regexp.last_match(2)
    "#{prefix.gsub(/\sstate=\"[^\"]*\"/, '')} state=\"veryHidden\"#{ending}"
  end
  File.write(workbook, xml, encoding: 'UTF-8')
  temporary = "#{path}.tmp"
  FileUtils.rm_f(temporary)
  abort('无法重新打包工作簿') unless system('zip', '-q', '-X', '-r', temporary, '.', chdir: directory)
  File.rename(temporary, path)
end
