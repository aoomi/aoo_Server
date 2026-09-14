#!/usr/bin/env ruby
# frozen_string_literal: true

require "digest"
require "fileutils"
require "json"

ROOT = File.expand_path("../..", __dir__)
LEGACY_ROOT = ENV.fetch("LEGACY_CLIENT_ROOT", "/Users/aoo/Code/Game/BCG/Test/QH_DFMJ/client/assets/script")
BASELINE = File.join(__dir__, "legacy_protocols.txt")
OUTPUT = File.join(__dir__, "ledger.json")

def extract_protocols(root)
  raise "legacy source is not a directory: #{root}" unless Dir.exist?(root)
  Dir.glob(File.join(root, "**", "*.js")).sort.flat_map do |path|
    File.readlines(path, encoding: "UTF-8", invalid: :replace, undef: :replace).flat_map do |line|
      next [] if line.lstrip.start_with?("//")
      line.scan(/SendPack\s*\(\s*["']([^"']+)["']/).flatten
    end
  end.uniq.sort
end

def target(protocol)
  namespace, command = protocol.split(".", 2)
  key = "#{namespace}.#{command}".downcase
  case key
  when /luckdraw/
    ["LuckDraw", "HTTP /api/v1/luck-draw/#{command}", "server/LuckDraw/src/main/java/com/aoo/bcg/luckdraw/LuckDrawHttpRoutes.java", "startLuckDraw"]
  when /task|sign|receive(?:share|list)/
    ["Activity", "HTTP /v1/activities/#{command}", "server/Activity/src/main/java/com/aoo/bcg/activity/ActivityHttpRoutes.java", "startActivity"]
  when /rank/
    ["Ranking", "HTTP /api/v1/ranking/#{command}", "server/Ranking/src/main/java/com/aoo/bcg/ranking/RankingAchievementHttpRoutes.java", "startRanking"]
  when /helproom|feedback|report/
    ["Support", "HTTP /api/v1/support/#{command}", "server/Support/src/main/java/com/aoo/bcg/support/SupportHttpRoutes.java", "startSupport"]
  when /location|address/
    ["Location", "HTTP /api/v1/location/#{command}", "server/Location/src/main/java/com/aoo/bcg/location/LocationHttpRoutes.java", "ExternalPlatformBootstrap"]
  when /playback|record/
    ["Replay", "HTTP /v1/replays/#{command}", "server/Bootstrap/src/main/java/com/aoo/bcg/bootstrap/RecordReplayHttpRoutes.java", "startRecords"]
  when /real.*auth|phone/
    ["Identity", "HTTP /api/v1/identity/#{command}", "server/IdentityVerification/src/main/java/com/aoo/bcg/identity/IdentityVerificationHttpRoutes.java", "startIdentityVerification"]
  when /playerinfo|changename|headimg/
    ["Profile", "HTTP /api/v1/profile/#{command}", "server/PlayerProfile/src/main/java/com/aoo/bcg/profile/PlayerProfileHttpRoutes.java", "startAccount"]
  when /exchange|prize|gift/
    ["Inventory", "HTTP /api/v1/inventory/#{command}", "server/Inventory/src/main/java/com/aoo/bcg/inventory/InventoryHttpRoutes.java", "startInventory"]
  when /club|union/
    ["Club", "HTTP /v1/clubs/legacy-operations/#{command}", "server/Club/src/main/java/com/aoo/bcg/club/ClubHttpRoutes.java", "startClub"]
  when /family/
    ["Social", "HTTP /api/v1/social/legacy-operations/#{command}", "server/Social/src/main/java/com/aoo/bcg/social/SocialHttpRoutes.java", "startSocial"]
  when /login|createrole|uuid/
    ["Account", "HTTP /api/v1/account/legacy-operations/#{command}", "server/Account/src/main/java/com/aoo/bcg/account/AccountHttpRoutes.java", "startAccount"]
  when /heartbeat/
    ["Gateway", "WSS /api/v1/gateway/ws#heartbeat/#{command}", "server/Gateway/src/main/java/com/aoo/bcg/gateway/GatewayApplication.java", "WS_PATH"]
  else
    ["Room", "WSS /api/v1/gateway/ws#command/#{protocol}", "server/Gateway/src/main/java/com/aoo/bcg/gateway/GameWebSocketRouter.java", "GameWebSocketRouter"]
  end
end

protocols = extract_protocols(LEGACY_ROOT)
abort "expected 283 active literal SendPack protocols, got #{protocols.length}" unless protocols.length == 283
File.write(BASELINE, protocols.join("\n") + "\n")

rows = protocols.each_with_index.map do |protocol, index|
  domain, entry, evidence, assembly = target(protocol)
  assembly_path = case assembly
                  when "GameWebSocketRouter" then "server/Gateway/src/main/java/com/aoo/bcg/gateway/UnifiedGameRuntime.java"
                  when "WS_PATH" then "server/Gateway/src/main/java/com/aoo/bcg/gateway/GatewayApplication.java"
                  when "ExternalPlatformBootstrap" then "server/Bootstrap/src/main/java/com/aoo/bcg/bootstrap/ExternalPlatformBootstrap.java"
                  else "server/Bootstrap/src/main/java/com/aoo/bcg/bootstrap/BootstrapAPP.java"
                  end
  {
    "id" => format("R63-%03d", index + 1), "legacyProtocol" => protocol,
    "businessDomain" => domain, "newEntry" => entry, "status" => "replaced",
    "evidence" => [evidence, assembly_path],
    "assemblyMarker" => assembly,
    "reason" => "旧 SendPack 由当前 #{domain} HTTP/WSS 生产入口承接；保留逐协议 operation 键以防合并时静默丢失"
  }
end
payload = {
  "schemaVersion" => 1, "issue" => "R63-001", "legacyCount" => rows.length,
  "baselineSha256" => Digest::SHA256.hexdigest(protocols.join("\n") + "\n"), "rows" => rows
}
File.write(OUTPUT, JSON.pretty_generate(payload) + "\n")
puts JSON.generate("legacyCount" => rows.length, "output" => OUTPUT)
