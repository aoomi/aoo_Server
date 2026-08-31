package business.global.pk.njpdk;

import business.njpdk.c2s.cclass.NJPDK_define;
import business.njpdk.c2s.iclass.SNJPDK_OutCardList;
import jsproto.c2s.cclass.pk.Victory;

import java.util.HashMap;
import java.util.Map;

public class NJPDKGameResult {
    public NJPDKRoom room; //房间

    public NJPDKGameResult(NJPDKRoom room) {
        this.room = room;
    }

    /**
     * 结算赢家
     *
     * @return
     */
    public int calWinPos() {
        NJPDKRoomSet set = (NJPDKRoomSet) this.room.getCurSet();
        int winPos = -1;
        for (int i = 0; i < this.room.getPlayerNum(); i++) {
            if (set.isCalcList.get(i)) continue;
            NJPDKRoomPos roomPos = (NJPDKRoomPos) this.room.getRoomPosMgr().getPosByPosID(i);
            if (roomPos.cards().size() <= 0 || roomPos.type == 1) {
                winPos = i;
                break;
            }
        }

        if (winPos == -1) return -1;
        set.isCalcList.set(winPos, true);
        return winPos;
    }

    /**
     * 计算分数
     */
    public int calPoint() {
        int winPos = calWinPos();
        if (winPos == -1) {
            return -1;
        }
        NJPDKRoomSet set = (NJPDKRoomSet) this.room.getCurSet();
        int baoPeiPos = ((NJPDKRoomSet) room.getCurSet()).baoPeiPos;

        int firstPos = winPos;
        if (0 != set.cardList.size()) {
            firstPos = set.cardList.get(0).pos;
        }
        // 检查春天 反春

        Map<Integer, Integer> posCount = new HashMap<>();
        for (SNJPDK_OutCardList outCardList : set.cardList) {
            if (!posCount.containsKey(outCardList.pos)) {
                posCount.put(outCardList.pos, 0);
            }
            posCount.put(outCardList.pos, posCount.get(outCardList.pos) + 1);
        }
        NJPDKRoomPos winPosObj = (NJPDKRoomPos) this.room.getRoomPosMgr().getPosByPosID(winPos);
        // 春天 反春
        if (0 < set.cardList.size()) {
            boolean chunTian = true;
            boolean fanChun = true;
            for (Map.Entry<Integer, Integer> entry : posCount.entrySet()) {
                if (entry.getKey() == firstPos && entry.getValue() > 1) {
                    fanChun = false;
                }
                if (entry.getKey() != firstPos && entry.getValue() > 0) {
                    if (entry.getKey() != winPos && entry.getValue() > 0) {
                        fanChun = false;
                    }
                    chunTian = false;
                }
            }
            if (chunTian) {
                winPosObj.type = 2;
            } else if (fanChun && room.getRoomCfg().getKexuanwanfa().contains(NJPDK_define.KeXuanWanFa.ChunTian.getType())) {
                winPosObj.type = 3;
            }
        }
        for (int i = 0; i < this.room.getPlayerNum(); i++) {
            if (i == winPos) continue;
            if (set.isCalcList.get(i)) continue;
            // 不是炸弹+10分 最后一张牌不算分
            if (set.surplusCardRecordList.get(i) == 1) {
                continue;
            }
            int cardSize = 16;
            if (room.getRoomCfg().getKexuanwanfa().contains(NJPDK_define.KeXuanWanFa.Card15.getType())) {
                cardSize = 15;
            }
            int baseScore = 1;
            if (winPosObj.type != 0 || set.surplusCardRecordList.get(i) == cardSize) {
                baseScore = 2;
            }
            int score = baseScore * set.surplusCardRecordList.get(i);

            // 炸弹翻倍，双倍得分，4炸封顶
            if (NJPDK_define.BombScore.DOUBLE_.has(room.getRoomCfg().zhadan)) {
                NJPDKRoomSet roomSet = (NJPDKRoomSet) this.room.getCurSet();
                Integer bombCount = roomSet.roomZhaDanList.stream()
                        .map(Victory::getNum)
                        .reduce(0, (x, y) -> x + y);
//                score = score * (int) Math.max(1, Math.pow(2, bombCount > 4 ? 4 : bombCount));
                score = score * (int) Math.max(1, Math.pow(2, bombCount));
            }
            score *= room.cfg.getBeishu();

            set.pointList.set(winPos, set.pointList.get(winPos) + score);
            set.pointList.set(baoPeiPos == -1 ? i : baoPeiPos, set.pointList.get(baoPeiPos == -1 ? i : baoPeiPos) - score);
        }
        return winPos;
    }
}
