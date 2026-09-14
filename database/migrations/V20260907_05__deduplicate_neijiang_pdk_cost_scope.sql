-- Billing currently keys a policy by game/version/round/player/payer and does not
-- include region. NJ201 is now activated only in CN-51-10, so retire the older
-- province fallback rows after the complete city policy set was published.

UPDATE aoo_room_cost_policy
SET status='RETIRED',policy_version=policy_version+1,
    content_hash=SHA2(CONCAT(content_hash,'|retired-for-CN-51-10'),256)
WHERE game_id=629 AND play_version='legacy-equivalent-1' AND region_code='CN-51'
  AND status='ACTIVE';
