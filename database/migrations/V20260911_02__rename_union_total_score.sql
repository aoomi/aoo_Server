UPDATE aoo_club_state
SET state_json = JSON_REMOVE(
    JSON_SET(state_json, '$.settings.unionTotalScore',
        COALESCE(JSON_EXTRACT(state_json, '$.settings.unionInitSports'), 2000)),
    '$.settings.unionInitSports')
WHERE JSON_CONTAINS_PATH(state_json, 'one', '$.settings.unionInitSports');
