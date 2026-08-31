#!/bin/zsh
set -euo pipefail

ROOT="${0:A:h:h}"
failed=0

reject() {
  local label="$1"
  shift
  local matches
  matches="$(rg -n -i "$@" 2>/dev/null || true)"
  if [[ -n "$matches" ]]; then
    print -u2 "region classification boundary failed: $label"
    print -u2 "$matches"
    failed=1
  fi
}

# Region belongs to the gameplay catalog. Identity/profile domains must not expose a second region model.
reject "Account identity still contains region fields" \
  '(^|[^A-Za-z])(region|regionCode|showRegion|show_region|region_code)([^A-Za-z]|$)' \
  "$ROOT/server/Account/src/main"
reject "PlayerProfile identity model or repository still contains region fields" \
  '(^|[^A-Za-z])(region|regionCode|showRegion|show_region|region_code)([^A-Za-z]|$)' \
  "$ROOT/server/PlayerProfile/src/main/java/com/aoo/bcg/profile/PlayerProfileModels.java" \
  "$ROOT/server/PlayerProfile/src/main/java/com/aoo/bcg/profile/JdbcPlayerProfileRepository.java"

rg -q 'forbidIdentityRegion' "$ROOT/server/PlayerProfile/src/main/java/com/aoo/bcg/profile/PlayerProfileHttpRoutes.java" || {
  print -u2 'region classification boundary failed: PlayerProfile no longer rejects retired identity-region fields'
  failed=1
}

# These domains may carry an explicit classificationRegionCode for display/audit, but never a raw routing key.
for domain in Billing Club Gateway; do
  reject "$domain still contains raw region routing fields" \
    '(^|[^A-Za-z])(regionCode|region_code|provinceCode|province_code|cityCode|city_code)([^A-Za-z]|$)' \
    "$ROOT/server/$domain/src/main"
done

reject "Matchmaking silently consumes or forwards region routing fields" \
  '(put|get|remove)\("(regionCode|region_code|provinceCode|province_code|cityCode|city_code)"' \
  "$ROOT/server/Matchmaking/src/main"

[[ -f "$ROOT/database/migrations/V20260825_96__remove_player_profile_region_identity.sql" ]] || {
  print -u2 'region classification boundary failed: destructive player-profile cleanup migration is missing'
  failed=1
}

(( failed == 0 )) || exit 1
print 'region classification boundary passed'
