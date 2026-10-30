package core.server;

import com.ddm.server.common.CommLogD;
import com.ddm.server.common.utils.CommTime;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;


public class OpenSeverTime {
    Date date;
    private long startServerTime;

    public static OpenSeverTime getInstance() {
        return Holder.INSTANCE;
    }

    private static final class Holder {
        private static final OpenSeverTime INSTANCE = new OpenSeverTime();
    }

    public void init() {
        try {
            startServerTime = CommTime.nowMS();
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            String time = System.getProperty("GameServer.OpenTime");
            time = time.trim();
            String minDate = "2016-04-30 00:00:00";
            if (time.isEmpty() || "0".equalsIgnoreCase(time)) {
                date = sdf.parse(minDate);
                return;
            }
            date = sdf.parse(time);
            Date min = sdf.parse(minDate);
            if (date.before(min)) {
                date = min;
            }
        } catch (Exception e) {
            CommLogD.info(e.getMessage(), e);
        }
    }


    public long getStartServerTime() {
        return startServerTime;
    }

    public Date getOpenDate() {
        return date;
    }

    public boolean isOverStartTime() {
        Date nowDate = new Date();
        return nowDate.after(date);
    }

    public int getOpenZeroTime() {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return (int) (calendar.getTimeInMillis() / 1000);
    }

    public int getOpenDays() {
        if (date == null) {
            return -1;
        }
        int today = CommTime.getZeroClockS(CommTime.nowSecond());
        int openday = getOpenZeroTime();
        int DaySec = 24 * 60 * 60;
        int diff = (today - openday) / DaySec;

        return diff >= 0 ? diff + 1 : diff;

    }
}
