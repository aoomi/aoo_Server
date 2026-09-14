#!/usr/bin/env ruby
# frozen_string_literal: true
require 'json'; require 'fileutils'; require 'open3'
root=File.expand_path('..',__dir__);jdk=File.expand_path('../.toolchains/jdk-26.0.2.1.jdk/Contents/Home',root)
spi=File.read(File.join(root,'server/GameSPI/src/main/java/com/aoo/bcg/gamespi/AuthoritativeGameSession.java'))
implementations=Dir[File.join(root,'server/{Mahjong,Poker,LongCard,WordCard}/src/main/java/**/*.java')].select{|p|File.read(p).include?('implements AuthoritativeGameSession')}
covered=implementations.all?{|p|s=File.read(p);s.include?('OperationDeadline operationDeadline')&&s.include?('"operationDeadline"')}
stdout,stderr,status=Open3.capture3({'JAVA_HOME'=>jdk,'PATH'=>"#{jdk}/bin:#{ENV['PATH']}"},'./mvnw','-q','-pl','server/Mahjong,server/Poker','-am','-Dtest=OperationDeadlineTest,MahjongAuthoritativeSessionTest,PokerAuthoritativeSessionTest','-Dsurefire.failIfNoSpecifiedTests=false','test',chdir:root)
checks={spi_requires_deadline:spi.include?('OperationDeadline operationDeadline()'),all_authorities_persist_deadline:covered,tests_passed:status.success?};result={task:'TIME02',passed:checks.values.all?,checks:checks,implementations:implementations.map{|p|p.delete_prefix(root+'/')},testStdout:stdout.strip,testStderr:stderr.strip};out=File.join(root,'work/audit/unified-operation-deadline.json');FileUtils.mkdir_p(File.dirname(out));File.write(out,JSON.pretty_generate(result)+"\n");puts JSON.generate(result);exit(result[:passed] ? 0 : 1)
