#!/usr/bin/env ruby
# frozen_string_literal: true
require 'digest'; require 'fileutils'; require 'json'; require 'optparse'; require 'pathname'; require 'time'
ROOT=Pathname.new(__dir__).parent.realpath; LOCK=ROOT/'work/remediation.lock'; AUDIT=ROOT/'work/remediation-audit.jsonl'; STATE=ROOT/'work/remediation-state.json'
FORBIDDEN=[/git\s+reset\s+--hard/,/git\s+checkout\s+--/,/rm\s+-[^\s]*r[^\s]*f\s+\/(?:\s|$)/,/DROP\s+(?:DATABASE|SCHEMA|TABLE)/i,/TRUNCATE\s+TABLE/i,/kubectl\s+delete\s+(?:namespace|pvc)/,/terraform\s+destroy/].freeze
STATUSES=%w[in_progress failed completed manual_deferred].freeze
options={evidence:[],write_scope:[]}; parser=OptionParser.new do|o|
 o.banner='usage: remediation_guard.rb --issue ID --status STATUS --evidence PATH --write-scope PATH -- command'
 o.on('--issue ID'){|v|options[:issue]=v};o.on('--status STATUS'){|v|options[:status]=v};o.on('--evidence PATH'){|v|options[:evidence]<<v};o.on('--write-scope PATH'){|v|options[:write_scope]<<v}
end
parser.order!(ARGV);ARGV.shift if ARGV.first=='--';command=ARGV
abort parser.to_s unless options[:issue]&.match?(/\A[A-Z][A-Z0-9_-]{1,63}\z/)&&STATUSES.include?(options[:status])&&!command.empty?
abort 'completed remediation requires evidence' if options[:status]=='completed'&&options[:evidence].empty?
def relative(value); path=(ROOT/value).cleanpath;abort "path escapes workspace: #{value}" unless path.to_s==ROOT.to_s||path.to_s.start_with?(ROOT.to_s+'/');path.relative_path_from(ROOT).to_s end
scopes=options[:write_scope].map{|v|relative(v)}.uniq;evidence=options[:evidence].map{|v|relative(v)}.uniq;rendered=command.join(' ')
abort "irreversible command rejected: #{rendered}" if FORBIDDEN.any?{|pattern|rendered.match?(pattern)}
def inventory(extra=[])
 patterns=%w[pom.xml .mvn/**/* config/**/* database/migrations/*.sql docs/*.md protocol/**/* tools/**/* scripts/**/* server/**/*]
 (patterns.flat_map{|pattern|Dir.glob((ROOT/pattern).to_s,File::FNM_DOTMATCH)}+extra.map{|p|(ROOT/p).to_s}).uniq.sort.map do|path|
  next unless File.file?(path);relative=Pathname.new(path).relative_path_from(ROOT).to_s;next if relative.split('/').any?{|part|%w[target build node_modules .git].include?(part)};[relative,Digest::SHA256.file(path).hexdigest]
 end.compact.to_h
end
def in_scope?(path,scopes);scopes.any?{|scope|path==scope||path.start_with?(scope.sub(%r{/+\z},'')+'/')} end
FileUtils.mkdir_p(LOCK.dirname)
if LOCK.directory?
 owner=JSON.parse((LOCK/'owner.json').read) rescue {};pid=owner['pid'].to_i;active=false
 begin;Process.kill(0,pid);active=pid.positive?;rescue Errno::ESRCH,Errno::EPERM;active=false;end
 abort "another remediation process owns workspace: #{owner}" if active
 FileUtils.remove_entry_secure(LOCK)
end
Dir.mkdir(LOCK);started=Time.now.utc;success=false
begin
 (LOCK/'owner.json').write(JSON.generate(pid:Process.pid,issue:options[:issue],startedAt:started.iso8601,status:options[:status],writeScope:scopes,command:command))
 before=inventory(scopes+evidence);command_success=system(*command,chdir:ROOT.to_s);after=inventory(scopes+evidence)
 changed=(before.keys|after.keys).select{|path|before[path]!=after[path]};outside=changed.reject{|path|in_scope?(path,scopes)};missing=evidence.reject{|path|File.file?(ROOT/path)}
 success=command_success&&outside.empty?&&missing.empty?;effective=success ? options[:status]:'failed'
 record={issue:options[:issue],requestedStatus:options[:status],effectiveStatus:effective,startedAt:started.iso8601,completedAt:Time.now.utc.iso8601,pid:Process.pid,command:command,commandSuccess:command_success,success:success,declaredWriteScope:scopes,changedFiles:changed,outOfScopeOrConcurrentChanges:outside,evidence:evidence.to_h{|path|[path,File.file?(ROOT/path) ? Digest::SHA256.file(ROOT/path).hexdigest : nil]},missingEvidence:missing,changedHashes:changed.to_h{|path|[path,{before:before[path],after:after[path]}]}}
 File.open(AUDIT,'a'){|f|f.puts(JSON.generate(record))};state=File.file?(STATE) ? JSON.parse(STATE.read):{'schemaVersion'=>1,'issues'=>{}}
 state['issues'][options[:issue]]={'status'=>effective,'updatedAt'=>record[:completedAt],'evidence'=>record[:evidence],'lastAuditLineSha256'=>Digest::SHA256.hexdigest(JSON.generate(record))}
 temporary=STATE.sub_ext('.json.tmp');temporary.write(JSON.pretty_generate(state)+"\n");temporary.rename(STATE)
 warn "out-of-scope or concurrent changes: #{outside.join(', ')}" unless outside.empty?;warn "missing evidence: #{missing.join(', ')}" unless missing.empty?
ensure
 FileUtils.remove_entry_secure(LOCK) if LOCK.directory?
end
exit(success ? 0:1)
