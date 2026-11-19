#!/usr/bin/env ruby
# frozen_string_literal: true

require "json"
require "digest"
require "open3"
require "fileutils"

root = File.expand_path("../../..", __dir__)
scan_inputs=JSON.parse(File.read(File.join(root, 'engineering/scan-input-roots.json'))); tool_input_roots=scan_inputs.fetch('toolInputs').map { |relative| File.join(root, relative) }; script_input_roots=scan_inputs.fetch('scriptInputs').map { |relative| File.join(root, relative) }; scripts = tool_input_roots.flat_map { |input_root| Dir[File.join(input_root, '*.{rb,py,sh}')] }.uniq.sort
pom = File.read(File.join(root, "pom.xml"))

entries = scripts.map do |path|
  name = File.basename(path)
  content = File.read(path)
  language, command = case File.extname(path)
                      when ".rb" then ["ruby", ["ruby", "-c", path]]
                      when ".py" then ["python", ["python3", "-c", "import ast,sys; ast.parse(open(sys.argv[1], encoding='utf-8').read())", path]]
                      else ["shell", [content.lines.first.to_s.include?("bash") || content.lines.first.to_s.include?("zsh") ? "bash" : "sh", "-n", path]]
                      end
  _stdout, stderr, status = Open3.capture3(*command)
  side_effect = if name.match?(/\A(?:start|stop|runtime|f03_|account-soak|retire_|remediation_|verify_fresh)/)
                  "controlled-side-effect"
                elsif name.match?(/\A(?:generate|freeze_|compute_)/)
                  "repeatable-generator"
                else
                  "read-only-audit"
                end
  semantic_evidence = pom.include?(name) || name.start_with?("audit_") || name.start_with?("check_")
  {
    path: path.delete_prefix(root + "/"),
    language: language,
    version: "sha256:#{Digest::SHA256.file(path).hexdigest}",
    hasShebang: content.start_with?("#!"),
    syntaxValid: status.success?,
    syntaxError: status.success? ? nil : stderr.lines.first.to_s.strip,
    sideEffectClass: side_effect,
    semanticEvidenceDeclared: semantic_evidence,
    destructivePrimitive: content.match?(/(?:DROP\s+DATABASE|rm\s+-rf|FileUtils\.rm_rf|os\.remove|shutil\.rmtree)/i)
  }
end

uncovered = entries.reject { |entry| entry[:semanticEvidenceDeclared] }.map { |entry| entry[:path] }
destructive = entries.select { |entry| entry[:destructivePrimitive] }.map { |entry| entry[:path] }
checks = {
  policyPresent: File.file?(File.join(root, "docs", "工具脚本治理规范.md")),
  noCompiledArtifactsBesideSources: tool_input_roots.all? { |input_root| Dir[File.join(input_root, "*.{class,pyc,pyo}")].empty? },
  everyScriptHasShebang: entries.all? { |entry| entry[:hasShebang] },
  everyScriptSyntaxValid: entries.all? { |entry| entry[:syntaxValid] },
  everyScriptVersionedAndClassified: entries.all? { |entry| entry[:version] && entry[:sideEffectClass] },
  everyScriptHasSemanticEvidence: uncovered.empty?,
  noUnguardedDestructivePrimitive: destructive.empty?
}

ledger = File.join(root, "docs", "generated", "tool-script-inventory.json")
FileUtils.mkdir_p(File.dirname(ledger))
File.write(ledger, JSON.pretty_generate(entries) + "\n")
report = {
  task: "HYGIENE08",
  status: checks.values.all? ? "passed" : "failed",
  checks: checks,
  inventory: { scripts: entries.length, ruby: entries.count { |e| e[:language] == "ruby" }, python: entries.count { |e| e[:language] == "python" }, shell: entries.count { |e| e[:language] == "shell" } },
  semanticEvidenceMissing: uncovered,
  destructivePrimitiveFiles: destructive,
  ledger: "docs/generated/tool-script-inventory.json"
}
FileUtils.mkdir_p(File.join(root, "build-only", "audit"))
File.write(File.join(root, "build-only", "audit", "hygiene08-tool-quality.json"), JSON.pretty_generate(report) + "\n")
puts JSON.generate(report)
exit(checks.values.all? ? 0 : 2)
