#!/usr/bin/env ruby
require 'json';require 'fileutils';root=File.expand_path('..',__dir__)
g=File.read(File.join(root,'server/GameCommon/src/main/java/com/aoo/bcg/common/concurrency/LockOrderGuard.java'));club=File.read(File.join(root,'server/GameCommon/src/main/java/com/aoo/bcg/common/club/ClubAggregateTransaction.java'));billing=File.read(File.join(root,'server/Billing/src/main/java/com/aoo/bcg/billing/JdbcBillingService.java'))
checks={'complete_order'=>%w[GLOBAL CLUB ROOM PLAYER ASSET DATABASE].all?{|x|g.include?(x)},'inversion_rejected'=>g.include?('lock order violation'),
 'same_level_key_order'=>g.include?('key.compareTo(current.key())<=0'),'club_integrated'=>club.include?('LockOrderGuard.Level.CLUB'),'asset_integrated'=>billing.include?('LockOrderGuard.Level.ASSET'),'database_last'=>club.index('Level.DATABASE')>club.index('Level.CLUB')&&billing.index('Level.DATABASE')>billing.index('Level.ASSET')}
r={'task'=>'LOCK01','passed'=>checks.values.all?,'checks'=>checks};out=File.join(root,'work/audit/lock01-order.json');FileUtils.mkdir_p(File.dirname(out));File.write(out,JSON.pretty_generate(r)+"\n");puts JSON.generate(r);exit(r['passed'] ? 0 : 1)
