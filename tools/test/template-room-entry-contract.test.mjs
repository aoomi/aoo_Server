import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import test from 'node:test';

const root = new URL('../../', import.meta.url);
const read = path => readFileSync(new URL(path, root), 'utf8');

test('legacy template desks enter through the server-owned empty-room chain', () => {
    const handler = read('server/gameServer/src/core/network/client2game/handler/room/CBaseEnterRoom.java');
    const playerRoom = read('server/gameServer/src/business/player/feature/PlayerRoom.java');
    assert.match(handler, /findAndEnter\(req\.getPosID\(\), req\.getRoomKey\(\), req\.getClubId\(\), req\.getPassword\(\)\)/);
    assert.match(playerRoom, /if \(roomImpl\.isNoneRoom\(\)\) \{\s*return createAndEnter\(roomImpl, posID, roomKey, clubId\)/);
});

test('automatic club and union rooms charge only their owners', () => {
    const club = read('server/gameServer/src/business/global/club/Club.java');
    const union = read('server/gameServer/src/business/global/union/Union.java');
    assert.match(club, /getOwnerPlayer\(\)\.getFeature\(PlayerWallet\.class\)\.checkAndClubConsumeRoom/);
    assert.match(union, /getOwnerPlayer\(\)\.getFeature\(PlayerWallet\.class\)\.checkAndUnionConsumeRoom/);
});
