#!/usr/bin/env ruby
# frozen_string_literal: true

# DEAD03 门禁（scripts/audit-dead03-registration-reverse.rb）的“数据库登记”判定来源。
#
# 背景（根因）：该门禁原本读 work/audit/registration-conservation-audit.json，而这份台账由
# tools/audit_registration_conservation.rb 生成、其根输入 work/backend-baseline-528.tsv 是机器本地
# 手工后端导出（仓库内无生成器，work/ 又被 .gitignore 忽略）。因此任何干净检出上门禁的前置必然
# 缺失；原脚本对缺失静默降级为空表，把“前置缺失”误判成“全部未登记”，产生假红。
# 修复：前置改用仓库内的权威数据库登记源 server/Bootstrap/src/main/resources/game-catalog-528.tsv
# （数据库 games 表的 528 条登记种子，随仓库版本固定，只读、离线、确定性），前置不可用时显式 abort，
# 不再静默降级。
module Dead03
  module DatabaseRegistration
    CATALOG_PATH = 'server/Bootstrap/src/main/resources/game-catalog-528.tsv'

    module_function

    def load(root)
      path = File.join(root, CATALOG_PATH)
      abort "DEAD03 missing prerequisite: #{CATALOG_PATH} (database game catalog)" unless File.file?(path)
      lines = File.readlines(path, encoding: 'UTF-8', chomp: true).reject { |line| line.strip.empty? }
      header = lines.shift.to_s.split("\t", -1).map { |column| column.to_s.strip }
      abort "DEAD03 unusable prerequisite: #{CATALOG_PATH} (missing code column)" unless header.include?('code')
      table = {}
      lines.each do |line|
        row = header.zip(line.split("\t", -1)).to_h
        code = row['code'].to_s.strip.downcase
        next if code.empty?
        table[code] ||= {
          'code' => code,
          'databaseRegistered' => true,
          'gameId' => row['gameId'].to_s.strip,
          'source' => CATALOG_PATH
        }
      end
      abort "DEAD03 unusable prerequisite: #{CATALOG_PATH} (no code rows)" if table.empty?
      table
    end
  end
end
