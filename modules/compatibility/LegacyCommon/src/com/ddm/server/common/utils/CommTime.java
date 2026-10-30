package com.ddm.server.common.utils;

import com.ddm.server.common.CommLogD;
import org.joda.time.*;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

public class CommTime {

    private record TimeConfig(TimeZone legacy, ZoneId business) {}
    private static volatile TimeConfig timeConfig = config(TimeZone.getTimeZone(System.getenv().getOrDefault("AOO_BUSINESS_ZONE", "Asia/Shanghai")));
    private static Locale _loc = Locale.getDefault(Locale.Category.FORMAT);
    /** Stable business zone: explicit env override, otherwise the process zone captured at startup. */
    private static TimeConfig config(TimeZone zone){TimeZone copy=(TimeZone)zone.clone();return new TimeConfig(copy,copy.toZoneId());}

    public static ZoneId businessZone() { return timeConfig.business(); }
    public static LocalDate accountingDay(Instant instant) { TimeConfig snapshot=timeConfig; return instant.atZone(snapshot.business()).toLocalDate(); }
    public static String accountingDayYmd(Instant instant) { return accountingDay(instant).toString().replace("-", ""); }
    public static boolean sameAccountingDay(Instant first, Instant second) { TimeConfig snapshot=timeConfig; return accountingDay(first,snapshot).equals(accountingDay(second,snapshot)); }
    private static LocalDate accountingDay(Instant instant,TimeConfig snapshot){return instant.atZone(snapshot.business()).toLocalDate();}

    public static int RecentSec = 0;
    public static final int MinSec = 60;
    public static final int HourSec = 60 * 60;
    public static final int DaySec = 24 * 60 * 60;
    public static final int WeekSec = 7 * 24 * 60 * 60;
    public static final int MonthSec = 30 * 24 * 60 * 60; // 一个月
    public static final int DayMs = 1000 * 3600 * 24;

    /**
     * 返回当前时区相对于UTC的原始偏移毫秒数
     *
     * @return
     */
    public static long getTimezoneRawOffset() {
        return timeConfig.legacy().getRawOffset();
    }

    /**
     * 返回夏令时偏移毫秒数
     *
     * @return
     */
    public static long getTimezoneDSTSavings() {
        return timeConfig.legacy().getDSTSavings();
    }

    /**
     * 返回指定时间相对于UTC的偏移时区
     *
     * @return
     */
    public static int getDateOffsetTiemZone(long timeMS) {
        return timeConfig.legacy().getOffset(timeMS) / (HourSec * 1000);
    }

    /**
     * 返回当前时区
     *
     * @return
     */
    public static TimeZone timezone() {
        return (TimeZone)timeConfig.legacy().clone();
    }

    /**
     * 设置某个时区
     *
     * @param tz
     */
    public static synchronized void setTimezone(TimeZone tz) {
        if(tz==null)throw new IllegalArgumentException("timezone is required");
        timeConfig=config(tz);
    }

    public static Locale getLocale() {
        return _loc;
    }

    public static void setLocale(Locale loc) {
        _loc = loc;
    }

    /**
     * 获取时间标记 - 精确到秒
     *
     * @return
     */
    public static int getTodaySecond() {
        return nowSecond() - getTodayZeroClockS();
    }

    /**
     * 获取时间标记 - 精确到分钟
     *
     * @return
     */
    public static int getTodayMinute() {
        Calendar objCalendar = newCalendar();
        int hour = objCalendar.get(Calendar.HOUR_OF_DAY);
        int minute = objCalendar.get(Calendar.MINUTE);

        return (hour * 100) + minute;
    }

    /**
     * 获取当前时间的小时部分（24小时制）
     *
     * @return
     */
    public static int getTodayHour() {
        Calendar objCalendar = newCalendar();
        return objCalendar.get(Calendar.HOUR_OF_DAY);
    }

    /**
     * 获取今天指定小时的毫秒数
     *
     * @param hour
     * @return
     */
    public static long getTodayHourMS(int hour) {
        return getTodayZeroClockMS(0) + hour * 3_600_000L;
    }

    /**
     * 获取当前时间距纪元所经历的毫秒数
     *
     * @return
     */
    public static long nowMS() {
        Calendar objCalendar = newCalendar();
        // TimeZone tz = TimeZone.getTimeZone("UTC");
        // objCalendar.setTimeZone(tz);
        return objCalendar.getTimeInMillis();
    }

    /**
     * 获取当前时间距纪元所经历的秒数 可表示从1970.1.1后 68年时间
     *
     * @return
     */
    public static int nowSecond() {
        Calendar objCalendar = newCalendar();
        int iNowTime = (int) (objCalendar.getTimeInMillis() / 1000);
        return iNowTime;
    }

    /**
     * 根据传入的字符串获取当前时间距纪元所经历的秒数 可表示从1970.1.1后 68年时间
     *
     * @return
     */
    public static int getNowSecond(String timeStr) {
        int timestamp = 0;

        try {
            SimpleDateFormat df = new SimpleDateFormat("yyyyMMdd");
            Date date = df.parse(timeStr);
            Calendar cal = Calendar.getInstance();
            cal.setTime(date);
            timestamp = (int) (cal.getTimeInMillis() / 1000);
        } catch (ParseException e) {
            // TODO 自动生成的 catch 块
            System.getLogger("legacy").log(System.Logger.Level.ERROR, "Legacy operation failed", e);
        }
        return timestamp;
    }

    /**
     * 获取指定时间当天0点的时间,单位毫秒
     *
     * @param timeMS
     * @return
     */
    public static long getZeroClockMS(long timeMS) {
        Calendar objCalendar = newCalendar();
        objCalendar.setTimeInMillis(timeMS);
        int year = objCalendar.get(Calendar.YEAR);
        int month = objCalendar.get(Calendar.MONTH);
        int day = objCalendar.get(Calendar.DAY_OF_MONTH);
        objCalendar.set(year, month, day, 0, 0, 0);

        return objCalendar.getTimeInMillis();
    }

    /**
     * 获取指定时间当天0点的时间,单位秒
     *
     * @param timeSec
     * @return
     */
    public static int getZeroClockS(int timeSec) {
        return (int) (getZeroClockMS((long) timeSec * (long) 1000) / 1000);
    }

    /**
     * 获取指定时间当天24点的时间,单位毫秒
     *
     * @param timeMS
     * @return
     */
    public static long get24ClockMS(long timeMS) {
        Calendar objCalendar = newCalendar();
        objCalendar.setTimeInMillis(timeMS);
        int year = objCalendar.get(Calendar.YEAR);
        int month = objCalendar.get(Calendar.MONTH);
        int day = objCalendar.get(Calendar.DAY_OF_MONTH);
        objCalendar.set(year, month, day + 1, 0, 0, 0);

        return objCalendar.getTimeInMillis();
    }

    /**
     * 获取指定时间当天24点的时间,单位秒
     *
     * @param timeSec
     * @return
     */
    public static int get24ClockS(int timeSec) {
        return (int) (getZeroClockMS((long) timeSec * (long) 1000) / 1000);
    }

    /**
     * 获取今天0点时间，毫秒
     *
     * @return
     */
    public static long getTodayZeroClockMS() {
        return getTodayZeroClockMS(0);
    }

    /**
     * 获取今天0点时间，毫秒
     *
     * @param offsetSec (偏移量)
     * @return
     */
    public static long getTodayZeroClockMS(int offsetSec) {
        Calendar objCalendar = newCalendar();
        objCalendar.add(Calendar.HOUR_OF_DAY, -offsetSec / 3600);

        int year = objCalendar.get(Calendar.YEAR);
        int month = objCalendar.get(Calendar.MONTH);
        int day = objCalendar.get(Calendar.DAY_OF_MONTH);
        objCalendar.set(year, month, day, 0, 0, 0);

        return objCalendar.getTimeInMillis();
    }

    /**
     * 获取今天0点时间，秒
     *
     * @return
     */
    public static int getTodayZeroClockS() {
        return (int) (getTodayZeroClockMS(0) / 1000);
    }

    /**
     * 获取今天0点时间，秒
     *
     * @param offsetSec (偏移量)
     * @return
     */
    public static int getTodayZeroClockS(int offsetSec) {
        return (int) (getTodayZeroClockMS(offsetSec) / 1000);
    }

    /**
     * 获取指定时间是第几天
     *
     * @param time
     * @return
     */
    public static int getDayIndex(int time) {
        return getZeroClockS(time) / (60 * 60 * 24);
    }

    /**
     * 获取今天是第几天
     *
     * @param time
     * @return
     */
    public static int getTodayIndex() {
        return getDayIndex(nowSecond());
    }

    /**
     * 获取今天是周几
     *
     * @return
     */
    public static int getDayOfWeek() {
        Calendar cal = Calendar.getInstance();
        int w = cal.get(Calendar.DAY_OF_WEEK) - 1; // 周0 - 6， 0是周日
        if (0 == w) {
            w = 7;
        }
        return w;
    }

    /**
     * 到下一小时的秒数
     *
     * @return
     */
    public static int getNextHourSecond() {
        Calendar objCalendar = newCalendar();
        int minute = objCalendar.get(Calendar.MINUTE);
        int second = objCalendar.get(Calendar.SECOND);
        return (60 - minute) * 60 - second;
    }

    /**
     * 获取距离下一个12点整的时间
     *
     * @return
     */
    public static int getNext12ColockLeft() {
        Calendar objCalendar = newCalendar();
        int year = objCalendar.get(Calendar.YEAR);
        int month = objCalendar.get(Calendar.MONTH);
        int day = objCalendar.get(Calendar.DAY_OF_MONTH);

        Calendar targetCalendar = newCalendar();
        targetCalendar.set(year, month, day, 0, 0, 0);
        targetCalendar.add(Calendar.DAY_OF_MONTH, 1);

        long nextRound = targetCalendar.getTimeInMillis() - objCalendar.getTimeInMillis();

        return (int) (nextRound / 1000);
    }

    /**
     * 获取本周第一天0点时间，毫秒
     *
     * @return
     */
    public static long getFirstDayOfWeekZeroClockMS(long timeMS) {

        Calendar objCalendar = newCalendar();
        objCalendar.setTimeInMillis(timeMS);
        int year = objCalendar.get(Calendar.YEAR);
        int month = objCalendar.get(Calendar.MONTH);
        int day = objCalendar.get(Calendar.DAY_OF_MONTH);
        int weekday = objCalendar.get(Calendar.DAY_OF_WEEK);

        Calendar targetCalendar = newCalendar();
        targetCalendar.set(year, month, day - weekday + 1, 0, 0, 0); // 周日那天0点的时间
        return targetCalendar.getTimeInMillis();
    }

    /**
     * 获取本周第一天0点时间，毫秒
     *
     * @return
     */
    public static long getFirstDayOfWeekZeroClockMS() {
        Calendar objCalendar = newCalendar();
        int year = objCalendar.get(Calendar.YEAR);
        int month = objCalendar.get(Calendar.MONTH);
        int day = objCalendar.get(Calendar.DAY_OF_MONTH);
        int weekday = objCalendar.get(Calendar.DAY_OF_WEEK);
        Calendar targetCalendar = newCalendar();
        targetCalendar.set(year, month, day - weekday + 1, 0, 0, 0); // 周日那天0点的时间

        return targetCalendar.getTimeInMillis();
    }

    /**
     * 获取上周第一天0点时间，毫秒
     *
     * @return
     */
    public static long getFirstDayOfLastWeekZeroClockMS() {
        Calendar objCalendar = newCalendar();
        int year = objCalendar.get(Calendar.YEAR);
        int month = objCalendar.get(Calendar.MONTH);
        int day = objCalendar.get(Calendar.DAY_OF_MONTH) - 7;
        int weekday = objCalendar.get(Calendar.DAY_OF_WEEK);
        Calendar targetCalendar = newCalendar();
        targetCalendar.set(year, month, day - weekday + 1, 0, 0, 0); // 周日那天0点的时间

        return targetCalendar.getTimeInMillis();
    }

    /**
     * 获取上周第一天0点时间，秒
     *
     * @return
     */
    public static int getFirstDayOfLatsWeekZeroClockS() {
        return (int) (getFirstDayOfLastWeekZeroClockMS() / 1000);
    }

    /**
     * 获取本周第一天0点时间，秒
     *
     * @return
     */
    public static int getFirstDayOfWeekZeroClockS() {
        return (int) (getFirstDayOfWeekZeroClockMS() / 1000);
    }

    /**
     * 获取本月第一天0点时间，毫秒
     *
     * @param offsetSecond (偏移量：秒)
     * @return
     */
    public static long getFirstDayOfMonthZeroClockMS(int offsetSecond) {
        Calendar targetCalendar = newCalendar();
        if (offsetSecond != 0) {
            int offsetHour = offsetSecond / 3600;
            targetCalendar.add(Calendar.HOUR_OF_DAY, -offsetHour);
        }
        int year = targetCalendar.get(Calendar.YEAR);
        int month = targetCalendar.get(Calendar.MONTH);

        targetCalendar.set(year, month, 1, 0, 0, 0);
        targetCalendar.set(Calendar.MILLISECOND, 0);

        return targetCalendar.getTimeInMillis();
    }

    /**
     * 获取本月第一天0点时间，秒
     *
     * @return
     */
    public static int getFirstDayOfMonthZeroClockS(int offsetSecond) {
        return (int) (getFirstDayOfMonthZeroClockMS(0) / 1000);
    }

    /**
     * 获取一个数值表示当前的周编号 小时 偏移。 eg: 3 则周一3点才认为是周一
     *
     * @return 返回YYWW形式
     */
    public static int getNowWeekNum(int offsetHour) {
        Calendar objCalendar = newCalendar();

        objCalendar.add(Calendar.HOUR_OF_DAY, -offsetHour);
        objCalendar.add(Calendar.DAY_OF_YEAR, -1);

        int yearNum = objCalendar.get(Calendar.YEAR);
        int weekNum = objCalendar.get(Calendar.WEEK_OF_YEAR);

        if (weekNum == 1) {
            Calendar tmpWeek = newCalendar();
            tmpWeek.add(Calendar.HOUR_OF_DAY, -offsetHour);
            objCalendar.add(Calendar.DAY_OF_YEAR, -1);

            tmpWeek.set(Calendar.DAY_OF_MONTH, 1);
            if (tmpWeek.get(Calendar.DAY_OF_WEEK) > 1) {
                // 本周第一天不是本月第一天年份需要减一
                yearNum--;

                // get the max week count for last year
                tmpWeek.set(Calendar.YEAR, yearNum);
                weekNum = tmpWeek.getActualMaximum(Calendar.WEEK_OF_YEAR);
            }
        }

        int weekSerialize = yearNum * 100 + weekNum;

        return weekSerialize;
    }

    /**
     * 获取当前时间的礼拜值,星期
     *
     * @return
     */
    public static int getNowDay() {
        Calendar cal = newCalendar();
        cal.setTime(new Date());
        int calDay = cal.get(Calendar.DAY_OF_WEEK) - 1;
        if (calDay == 0) {
            return 7;
        }
        return calDay;
    }

    /**
     * 获取当前时间-指定偏移量的礼拜值,星期(参数：秒偏移量)
     *
     * @return
     */
    public static int getNowDay(int offsetSecond) {
        Calendar objCalendar = newCalendar();
        objCalendar.setTime(new Date());

        int offsetHour = offsetSecond / 3600;

        objCalendar.add(Calendar.HOUR_OF_DAY, -offsetHour);
        int calDay = objCalendar.get(Calendar.DAY_OF_WEEK) - 1;
        if (calDay == 0) {
            return 7;
        }
        return calDay;
    }

    /**
     * 获取指定时间的礼拜值,星期
     *
     * @return
     */
    public static int getWeekDay(int nowTime) {
        Calendar cal = newCalendar();
        Date date = new Date();
        date.setTime(nowTime * 1000);
        cal.setTime(date);
        int calDay = cal.get(Calendar.DAY_OF_WEEK);
        if (calDay == 0) {
            return 7;
        }
        return calDay;
    }

    /**
     * 获取一个数值表示当前的月编号
     *
     * @return
     */
    public static int getNowMonthNum() {
        Calendar objCalendar = newCalendar();
        int yearNum = objCalendar.get(Calendar.YEAR);
        int monthNum = objCalendar.get(Calendar.MONTH);

        int monthSerialize = yearNum * 100 + monthNum;

        return monthSerialize;
    }

    /**
     * 获取当前日期的月份(加上指定偏移量【秒】)
     *
     * @return
     */
    public static int getNowMonth(int offsetSecond) {
        Calendar objCalendar = newCalendar();

        if (offsetSecond != 0) {
            int offsetHour = offsetSecond / 3600;
            objCalendar.add(Calendar.HOUR_OF_DAY, -offsetHour);
        }

        int monthNum = objCalendar.get(Calendar.MONTH);

        return monthNum + 1;
    }

    public static int getFirstDayOfMonth() {
        Calendar objCalendar = newCalendar();
        int day = objCalendar.get(Calendar.DAY_OF_MONTH);
        return day;
    }

    /**
     * 获取当前日期的月份
     *
     * @return
     */
    public static int getNowMonth() {
        Calendar objCalendar = newCalendar();
        int monthNum = objCalendar.get(Calendar.MONTH);

        return monthNum;
    }

    /**
     * 获取当前时间的，获取年月日时分秒
     *
     * @return
     */
    public static Timestamp getNowTimestamp() {
        Calendar objCalendar = newCalendar();
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", _loc);
        return Timestamp.valueOf(df.format(objCalendar.getTime()));
    }

    public static Timestamp getTimestamp(int timeSec) {
        if (timeSec <= 0) {
            return null;
        }
        Calendar objCalendar = newCalendar();
        objCalendar.setTimeInMillis(timeSec * 1000L);
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", _loc);
        return Timestamp.valueOf(df.format(objCalendar.getTime()));
    }

    // 当前日期描述为指定格式

    /**
     * 获取当前时间的字符串描述 yyyy-MM-dd HH:mm:ss
     *
     * @return
     */
    public static String getNowTimeString() {
        Calendar objCalendar = newCalendar();
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", _loc);
        return df.format(objCalendar.getTime());
    }

    /**
     * 获取当前时间的字符串描述yyyy-MM-dd HH:mm:ss.SSS
     *
     * @return
     */
    public static String getStringMS() {
        Calendar objCalendar = newCalendar();
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", _loc);
        return df.format(objCalendar.getTime());
    }

    /**
     * 获取当前时间的字符串描述，只获取年月日 yyyy
     *
     * @return
     */
    public static String getNowTimeStringY() {
        Calendar objCalendar = newCalendar();
        SimpleDateFormat df = new SimpleDateFormat("yyyy", _loc);
        return df.format(objCalendar.getTime());
    }

    /**
     * 获取当前时间的字符串描述，只获取年月日 yyyyMM
     *
     * @return
     */
    public static String getNowTimeStringYM() {
        Calendar objCalendar = newCalendar();
        SimpleDateFormat df = new SimpleDateFormat("yyyyMM", _loc);
        return df.format(objCalendar.getTime());
    }

    /**
     * 获取当前时间的字符串描述，只获取年月日
     *
     * @return
     */
    public static String getNowTimeStringYMD() {
        return DateTime.now().toString("yyyyMMdd");
    }

    /**
     * 获取当前时间的字符串描述，获取年月日时分秒，中间不分隔 yyyyMMddHHmmss
     *
     * @return
     */
    public static String getNowTimeStringYMDHMS() {
        return DateTime.now().toString("yyyyMMddHHmmss");
    }

    /**
     * 获取昨天时间的字符串描述，只获取年月日
     *
     * @return
     */
    public static String getYesterDayStringYMD(int dayCount) {
        return DateTime.now().minusDays(dayCount).toString("yyyyMMdd");
    }

    /**
     * 获取昨天时间的字符串描述，只获取年月日
     *
     * @return
     */
    public static String getYesterDayStringYMDSix(int dayCount) {
        if (dayCount == 0) {
            return String.valueOf(CommTime.getNowTime6YMD());
        } else {
            return String.valueOf(CommTime.getYesterDay6ByCount(dayCount));
        }
    }

    /**
     * 获取昨天时间的字符串描述，只获取年月日
     *
     * @return
     */
    public static DateTime getYesterDayDateTime(int dayCount) {
        return DateTime.now().minusDays(dayCount);
    }

    /**
     * 获取后天时间的字符串描述，只获取年月日
     *
     * @return
     */
    public static String getplusDayStringYMD(int dayCount) {
        return DateTime.now().plusDays(dayCount).toString("yyyyMMdd");
    }

    /**
     * 获取指定时间点的字符串描述 yyyy-MM-dd HH:mm:ss，秒
     *
     * @param lTimeS 距纪元所经历的秒数
     * @return
     */

    public static String getTimeString(long lTimeS) {
        Calendar objCalendar = newCalendar();
        objCalendar.setTimeInMillis(lTimeS * 1000);
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", _loc);
        return df.format(objCalendar.getTime());
    }


    /**
     * 获取指定时间点的字符串描述 HH:mm:ss，秒 获取时分秒格式
     *
     * @param lTimeS 距纪元所经历的秒数
     * @return
     */
    public static String getTimeStringHMS(long lTimeS) {
        Calendar objCalendar = newCalendar();
        objCalendar.setTimeInMillis(lTimeS * 1000);
        SimpleDateFormat df = new SimpleDateFormat("HH:mm:ss", _loc);
        return df.format(objCalendar.getTime());
    }


    private static Calendar newCalendar() {
        TimeConfig snapshot=timeConfig;
        return Calendar.getInstance(snapshot.legacy(), _loc);
    }

    /**
     * 判断时间是否同一天
     *
     * @param a 秒级时间戳
     * @param b 秒级时间戳
     * @return
     */
    public static boolean isSameDayWithInTimeZone(int a, int b) {
        return sameAccountingDay(Instant.ofEpochSecond(a), Instant.ofEpochSecond(b));
    }

    public static Date getDate(int timeInt) throws ParseException {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date d1 = sdf.parse(CommTime.getTimeString(timeInt));
        return d1;

    }

    /**
     * 计算两个日期之间相差的天数
     *
     * @param smdateInt 较小的时间
     * @param bdateInt  较大的时间
     * @return 相差天数
     * @throws ParseException
     */
    public static int daysBetween(int smdateInt, int bdateInt) {
        long between_days = 0L;
        try {
            Date smdate = getDate(smdateInt);
            Date bdate = getDate(bdateInt);

            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            smdate = sdf.parse(sdf.format(smdate));
            bdate = sdf.parse(sdf.format(bdate));
            Calendar cal = Calendar.getInstance();
            cal.setTime(smdate);
            long time1 = cal.getTimeInMillis();
            cal.setTime(bdate);
            long time2 = cal.getTimeInMillis();
            between_days = (time2 - time1) / (1000 * 3600 * 24);
        } catch (ParseException e) {
            System.getLogger("legacy").log(System.Logger.Level.ERROR, "Legacy operation failed", e);
        }
        return Integer.parseInt(String.valueOf(between_days));
    }


    /**
     * 分 时间差
     *
     * @return
     */
    public static int minTimeDifference(int endtime) {
        int nowTime = nowSecond();
        // 获得两个时间的毫秒时间差异
        int diff = endtime - nowTime;
        // 计算差多少分钟
        int min = diff / MinSec;
        return min;
    }

    /**
     * 分 时间差
     *
     * @return
     */
    public static int minTimeDifference(int nowTime, int endtime) {
        // int nowTime = nowSecond();
        // 获得两个时间的毫秒时间差异
        int diff = endtime - nowTime;
        // 计算差多少分钟
        int min = diff / MinSec;
        return min;
    }

    /**
     * 秒 时间差
     *
     * @return
     */
    public static int secondTimeDifference(int endtime) {
        int nowTime = nowSecond();
        // 获得两个时间的毫秒时间差异
        int diff = endtime - nowTime;
        // 计算差多少分钟
        int min = diff;
        return min;
    }

    /**
     * 秒 时间差
     *
     * @return
     */
    public static int secondTimeDifference(int nowTime, int endtime) {
        // int nowTime = nowSecond();
        // 获得两个时间的毫秒时间差异
        int diff = endtime - nowTime;
        // 计算差多少分钟
        int min = diff;
        return min;
    }

    /**
     * 检查时间是否本周内
     *
     * @return
     */
    public static boolean checkTimeThisWeek(int nowTime) {
        // 本周一
        int mondayClock = (getFirstDayOfWeekZeroClockS() + DaySec);
        // 本周日
        int SundayClock = (mondayClock + WeekSec) - 1;
        // 检查时间是否本周内
        if (mondayClock <= nowTime && nowTime <= SundayClock) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * 检查时间区间 检查当前时间是否在指定的时间区间内
     *
     * @param startTime
     * @param endTime
     * @return
     */
    public static boolean checkTimeIntervale(int startTime, int endTime) {
        int nowTime = nowSecond();
        if (startTime <= nowTime && nowTime < endTime) {
            return true;
        }
        return false;
    }


    /**
     * 将时间戳的时间往后推一天
     *
     * @return
     */
    public static int getDayAfter(int nowTime) {
        Calendar objCalendar = newCalendar();
        objCalendar.setTimeInMillis(SecToMsec(nowTime));
        objCalendar.add(Calendar.DATE, 1);
        return (int) (objCalendar.getTime().getTime() / 1000);
    }

    /**
     * 将时间戳的时间往后推一周
     *
     * @return
     */
    public static int getWeekAfter(int nowTime) {
        Calendar objCalendar = newCalendar();
        objCalendar.setTimeInMillis(SecToMsec(nowTime));
        objCalendar.add(Calendar.WEEK_OF_YEAR, 1);
        return (int) (objCalendar.getTime().getTime() / 1000);
    }

    /**
     * 通过指定时间戳获取 获取时分秒时间戳
     *
     * @param nowTime
     * @return
     */
    public static int getHMSTimeInMillis(int nowTime) {
        long timestamp = 0;
        try {
            DateFormat df = new SimpleDateFormat("HH:mm:ss");
            Date date = df.parse(getTimeStringHMS(nowTime));
            Calendar cal = Calendar.getInstance();
            cal.setTime(date);
            timestamp = cal.getTimeInMillis();
        } catch (ParseException e) {
            System.getLogger("legacy").log(System.Logger.Level.ERROR, "Legacy operation failed", e);
        }
        return (int) (timestamp / 1000);
    }

    /**
     * 检查时间区间 检查当前时间是否在指定的时间区间内
     *
     * @param startTime
     * @param endTime
     * @return
     */
    public static boolean checkTimeAllDay(int startTime, int endTime) {
        DateTime now = new DateTime();
        DateTime start = new DateTime(SecToMsec(startTime));
        DateTime end = new DateTime(SecToMsec(endTime));
        int nowInt = Integer.parseInt(now.toString("yyyyMMdd"));
        int startInt = Integer.parseInt(start.toString("yyyyMMdd"));
        int endInt = Integer.parseInt(end.toString("yyyyMMdd"));
        return startInt <= nowInt && nowInt <= endInt;
    }


    /**
     * 检查时间区间 检查当前时间是否在指定的时间区间内 每天
     *
     * @param startTime 开始时间
     * @param endTime   结束时间
     */
    public static boolean checkEveryDayTimeIntervale(int startTime, int endTime) {
        DateTime now = new DateTime();
        DateTime start = new DateTime(SecToMsec(startTime));
        DateTime end = new DateTime(SecToMsec(endTime));
        if (start.getSecondOfDay() <= now.getSecondOfDay() && now.getSecondOfDay() < end.getSecondOfDay()) {
            return true;
        }
        return false;
    }

    /**
     * 检查时间区间 检查当前时间是否在指定的时间区间内 每周
     * 具体时间
     *
     * @param startTime 开始时间
     * @param endTime   结束时间
     */
    public static boolean checkWeeklyTimeIntervale(int startTime, int endTime) {
        DateTime now = new DateTime();
        DateTime start = new DateTime(SecToMsec(startTime));
        DateTime end = new DateTime(SecToMsec(endTime));
        // 判断同一周。
        if (now.getDayOfWeek() >= start.getDayOfWeek() && now.getDayOfWeek() <= end.getDayOfWeek() && end.getDayOfWeek() >= start.getDayOfWeek()) {
            // 开始和结束时间同为一天
            if (now.getDayOfWeek() == start.getDayOfWeek() && now.getDayOfWeek() == end.getDayOfWeek()) {
                return start.getSecondOfDay() <= now.getSecondOfDay() && now.getSecondOfDay() < end.getSecondOfDay();
            }
            // 当前周与开始时间同一天
            if (now.getDayOfWeek() == start.getDayOfWeek()) {
                // 检查是否开始
                return start.getSecondOfDay() <= now.getSecondOfDay();
            }

            // 当前周与结束时间同一天
            if (now.getDayOfWeek() == end.getDayOfWeek()) {
                // 检查是否结束
                return now.getSecondOfDay() < end.getSecondOfDay();
            }
            return true;
        }
        return false;
    }

    /**
     * 检查时间区间 检查当前时间是否在指定的时间区间内 每周
     * 整天
     *
     * @param startTime 开始时间
     * @param endTime   结束时间
     */
    public static boolean checkWeeklyTimeAllDay(int startTime, int endTime) {
        DateTime now = new DateTime();
        DateTime start = new DateTime(SecToMsec(startTime));
        DateTime end = new DateTime(SecToMsec(endTime));
        // 判断同一周。
        if (now.getDayOfWeek() >= start.getDayOfWeek() && now.getDayOfWeek() <= end.getDayOfWeek() && end.getDayOfWeek() >= start.getDayOfWeek()) {
            return true;
        }
        return false;
    }


    /**
     * 小时 时间差
     *
     * @return
     */
    public static int hourTimeDifference(int endtime) {
        int nowTime = nowSecond();
        // 获得两个时间的毫秒时间差异
        int diff = endtime - nowTime;
        // 计算差多少小时
        int hour = diff / HourSec;
        return hour;
    }


    /**
     * 小时 时间差
     * 毫秒级时间戳
     *
     * @return
     */
    public static int hourTimeDifference(long startTime, long endTime) {
        return Hours.hoursBetween(new DateTime(startTime), new DateTime(endTime)).getHours();
    }


    /**
     * 检查时间区间 检查当前时间是否在指定的时间区间内
     *
     * @param startTime 开始时间
     * @param endTime   结束时间
     */
    public static boolean checkTimeIntervale(int startTime, int endTime, int nowTime) {
        if (startTime <= nowTime && nowTime < endTime) {
            return true;
        }
        return false;
    }

    /**
     * 判断是否同一周
     *
     * @param a
     * @param b
     * @return
     */
    public static boolean isSameWeekWithInTimeZone(int a, int b) {
        return getFirstDayOfWeekZeroClockMS(a) == getFirstDayOfWeekZeroClockMS(b);
    }

    /**
     * 秒转毫秒
     *
     * @return
     */
    public static long SecToMsec(int sec) {
        return Long.parseLong(String.format("%d%s", sec, "000"));
    }


    /**
     * 相差天数
     *
     * @param sec 时间戳(秒)
     * @return
     */
    public static int DaysBetween(int sec) {
        return Math.abs(Days.daysBetween(new DateTime(SecToMsec(sec)), DateTime.now()).getDays());
    }


    /**
     * 相差秒
     *
     * @param start 时间戳(ms)
     * @param end   (ms)
     * @return
     */
    public static int SecondsBetween(long start, long end) {
        return Math.abs(Seconds.secondsBetween(new DateTime(start), new DateTime(end)).getSeconds());
    }


    /**
     * 相差分钟
     *
     * @param start 时间戳(ms)
     * @param end   (ms)
     * @return
     */
    public static int MinutesBetween(long start, long end) {
        return Math.abs(Minutes.minutesBetween(new DateTime(start), new DateTime(end)).getMinutes());
    }

    /**
     * 相差分钟
     *
     * @param start 时间戳(ms)
     * @param end   (ms)
     * @return
     */
    public static int MinutesBetween(int start, int end) {
        return Math.abs(Minutes.minutesBetween(new DateTime(SecToMsec(start)), new DateTime(SecToMsec(end))).getMinutes());
    }

    /**
     * 开始时间+间隔天数 = 结束时间
     *
     * @param start 开始时间
     * @param rate  频率（间隔天数）
     * @return
     */
    public static int StartTimeCalcToEndTime(int start, int rate) {
        DateTime startTime = new DateTime(SecToMsec(start));
        return (int) (startTime.plusDays(rate).getMillis() / 1000);
    }

    /**
     * 获取当天指定点的时间戳(0：分，0：秒，0：毫秒)
     *
     * @param hour 几点
     * @return
     */
    public static int getWithHourOfDay(int hour) {
        return (int) (DateTime.now().withHourOfDay(hour).withMinuteOfHour(0).withSecondOfMinute(0).withMillisOfSecond(0).getMillis() / 1000);
    }


    /**
     * 开始时间+间隔天数 = 结束时间
     *
     * @param start 开始时间
     * @param rate  频率（间隔天数）
     * @return
     */
    public static int StartTimeCalcToEndTimet(int start, int rate) {
        DateTime startTime = new DateTime(SecToMsec(start));
        return (int) (startTime.minusDays(rate).getMillis() / 1000);
    }

    /**
     * 时间戳转年月日
     *
     * @param sec 时间戳(秒)
     * @return
     */
    public static int getSecToYMD(int sec) {
        DateTime dateTime = new DateTime(SecToMsec(sec));
        return Integer.parseInt(dateTime.toString("yyyyMMdd"));
    }

    /**
     * 时间戳转年月日
     *
     * @param sec 时间戳(秒)
     * @return
     */
    public static String getSecToYMDStr(int sec) {
        DateTime dateTime = new DateTime(SecToMsec(sec));
        return dateTime.toString("yyyyMMdd");
    }

    /**
     * 时间戳转年月日
     *
     * @param sec 时间戳(秒)
     * @return
     */
    public static String getSecToYMDStr2(int sec) {
        DateTime dateTime = new DateTime(SecToMsec(sec));
        return dateTime.toString("yyyy-MM-dd");
    }

    /**
     * 时间戳转年月日
     * 当天
     *
     * @return
     */
    public static String getNowTimeYMD() {
        return DateTime.now().toString("yyyyMMdd");
    }


    /**
     * 时间戳转年月日
     * 周期记录的时间戳
     *
     * @return
     */
    public final static int getCycleNowTime6YMD() {
        int nowTime6 = (int) (DateTime.now().withHourOfDay(6).withMinuteOfHour(0).withSecondOfMinute(0).withMillisOfSecond(0).getMillis() / 1000);
        return CommTime.nowSecond() < nowTime6 ? (int) (DateTime.now().minusDays(1).withHourOfDay(6).withMinuteOfHour(0).withSecondOfMinute(0).withMillisOfSecond(0).getMillis() / 1000) : nowTime6;
    }

    /**
     * 时间戳转年月日
     * 当天
     *
     * @return
     */
    public final static int getNowTime6YMD() {
        return (int) (DateTime.now().withHourOfDay(6).withMinuteOfHour(0).withSecondOfMinute(0).withMillisOfSecond(0).getMillis() / 1000);
    }

    /**
     * 获取特地时间的6点字符串
     *
     * @return
     */
    public static String getYesterDay6ByCount(int dayCount) {
        return String.valueOf(DateTime.now().minusDays(dayCount).withHourOfDay(6).withMinuteOfHour(0).withSecondOfMinute(0).withMillisOfSecond(0).getMillis() / 1000);
    }

    /**
     * 时间戳转年月日
     * 明天
     *
     * @return
     */
    public final static String getNextTimeYMD() {
        return DateTime.now().plusDays(1).toString("yyyyMMdd");
    }

    /**
     * 时间戳转年月日
     * 明天6点
     *
     * @return
     */
    public final static int getNextTime6YMD() {
        return (int) (DateTime.now().plusDays(1).withHourOfDay(6).withMinuteOfHour(0).withSecondOfMinute(0).withMillisOfSecond(0).getMillis() / 1000);
    }

    /**
     * 时间戳转年月日
     * 昨天
     *
     * @return
     */
    public final static String getBeforeTimeYMD() {
        return DateTime.now().minusDays(1).toString("yyyyMMdd");
    }

    /**
     * 时间戳转年月日
     * 昨天
     *
     * @return
     */
    public final static int getBeforeTime6YMD() {
        return (int) (DateTime.now().minusDays(1).withHourOfDay(6).withMinuteOfHour(0).withSecondOfMinute(0).withMillisOfSecond(0).getMillis() / 1000);
    }


    /**
     * 距离今天结束还剩下多少秒
     *
     * @return
     */
    public final static int RemainingTime() {
        return Math.toIntExact((DateTime.now().dayOfWeek().roundCeilingCopy().getMillis() - DateTime.now().getMillis()) / 1000L);
    }

    /**
     * 根据传进来的字符串 转为日期
     *
     * @param dateTime
     * @return
     */
    public DateTime getDateTimeByString(String dateTime) {
        try {
            DateTimeFormatter fmt = DateTimeFormat.forPattern("yyyyMMdd");
            return fmt.parseDateTime(dateTime);
        } catch (IllegalFieldValueException e) {
            CommLogD.error(e.getClass() + ":" + e.getMessage());
        }
        return new DateTime();
    }

    public static void main(String[] args) {


    }

}
