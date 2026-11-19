-- Room cost policies created without the measure dimension (for example by a seed
-- that omitted measure_kind/measure_value) fall back to the column default
-- measure_value=1, which contradicts the authoritative invariant established by
-- V20260919_06__room_cost_measure_dimension.sql: measure_kind='ROUND_COUNT' must
-- carry measure_value=round_count. Such rows can never be selected by the
-- authoritative lookup (JdbcRoomSagaBillingPort queries measure_kind=? AND
-- measure_value=?), so restoring the invariant is a normalization of unreachable
-- rows, never a rewrite of a policy that is in use.
UPDATE aoo_room_cost_policy
SET measure_value = round_count
WHERE measure_kind = 'ROUND_COUNT'
  AND measure_value <> round_count
  AND NOT EXISTS (
    SELECT 1 FROM (
      SELECT game_id, play_version, region_code, round_count, player_count, payer_mode
      FROM aoo_room_cost_policy
      WHERE measure_kind = 'ROUND_COUNT' AND measure_value = round_count
    ) canonical
    WHERE canonical.game_id = aoo_room_cost_policy.game_id
      AND canonical.play_version = aoo_room_cost_policy.play_version
      AND canonical.region_code = aoo_room_cost_policy.region_code
      AND canonical.round_count = aoo_room_cost_policy.round_count
      AND canonical.player_count = aoo_room_cost_policy.player_count
      AND canonical.payer_mode = aoo_room_cost_policy.payer_mode
  );
