/*
 * Decompiled with CFR 0.152.
 */
package BaseCommon;

import java.text.SimpleDateFormat;
import java.util.Calendar;

public class BaseCommonFun {
    public static long getNowTimeMS() {
        Calendar objCalendar = Calendar.getInstance();
        return objCalendar.getTimeInMillis();
    }

    public static String getNowTimeString() {
        Calendar objCalendar = Calendar.getInstance();
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return df.format(objCalendar.getTime());
    }

    public static void setSystemTime() {
        String osName = System.getProperty("os.name");
        String cmd = "";
        try {
            if (osName.matches("^(?i)Windows.*$")) {
                cmd = "  cmd /c time 22:35:00";
                Runtime.getRuntime().exec(cmd);
                cmd = " cmd /c date 2009-03-26";
                Runtime.getRuntime().exec(cmd);
            } else {
                cmd = "  date -s 20090326";
                Runtime.getRuntime().exec(cmd);
                cmd = "  date -s 22:35:00";
                Runtime.getRuntime().exec(cmd);
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
}

