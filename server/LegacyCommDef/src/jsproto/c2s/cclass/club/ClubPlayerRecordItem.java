package jsproto.c2s.cclass.club;

import lombok.Data;

@Data
public class ClubPlayerRecordItem {
    /**
     * 日期
     */
    private String dateTime;
    /**
     * 游戏Id
     */
    private Integer gameId;
    /**
     * 对局数
     */
    private int size;
    /**
     * 大赢家数
     */
    private int winner;
    /**
     * 分数
     */
    private double sumPoint;

    public static String getItemsName(long unionId) {
        if (unionId <= 0L) {
            return "dateTime,gameType as gameId,count(roomID) as size,sum(winner) as winner,sum(point) as sumPoint";
        } else {
            return "dateTime,gameType as gameId,count(roomID) as size,sum(winner) as winner,sum(sportsPoint) as sumPoint";
        }
    }

    public static String getItemsNameCount(long unionId) {
        if (unionId <= 0L) {
            return "count(roomID) as size,sum(winner) as winner,sum(point) as sumPoint";
        } else {
            return "count(roomID) as size,sum(winner) as winner,sum(sportsPoint) as sumPoint";
        }
    }

}
