package kr.co.kworks.socket_server_test;

import android.os.SystemClock;

import java.time.Instant;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

public class CalendarHandler {

    /**
     * yyyyMMddHHmmss from calendar
     * @param calendar
     * @return
     */
    public String convertToStringDatetime(Calendar calendar) {
        StringBuilder sb = new StringBuilder();
        sb.append(calendar.get(Calendar.YEAR));
        int month = calendar.get(Calendar.MONTH)+1; // 0~11
        if(month < 10) sb.append("0");
        sb.append(month);

        int date = calendar.get(Calendar.DATE);
        if(date < 10) sb.append("0");
        sb.append(date);

        int hour = calendar.get(Calendar.HOUR_OF_DAY);  // 0~23
        if(hour < 10) sb.append("0");
        sb.append(hour);

        int minute = calendar.get(Calendar.MINUTE);
        if(minute < 10) sb.append("0");
        sb.append(minute);

        int second = calendar.get(Calendar.SECOND);
        if(second < 10) sb.append("0");
        sb.append(second);
        return sb.toString();
    }

    public Calendar convertToCalendar(String sDatetime) {
        Calendar calendar = Calendar.getInstance();

        int year = Integer.parseInt(sDatetime.substring(0, 4));
        int month = Integer.parseInt(sDatetime.substring(4, 6)) - 1;
        int day = Integer.parseInt(sDatetime.substring(6, 8));
        int hour = Integer.parseInt(sDatetime.substring(8, 10));
        int minute = Integer.parseInt(sDatetime.substring(10, 12));
        int second = Integer.parseInt(sDatetime.substring(12, 14));

        calendar.set(year, month, day, hour, minute, second);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar;
    }

    /**
     * add seconds to specific calendar
     * @param calendar
     * @param seconds
     * @return
     */
    public Calendar getCalendarAfter(Calendar calendar, int seconds) {
        calendar.add(Calendar.SECOND, seconds);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar;
    }

    /**
     * add seconds to Calendar.getInstance()
     * @param seconds
     * @return
     */
    public Calendar getCalendarAfter(int seconds) {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.SECOND, seconds);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar;
    }

    public long getSecondsCal1MinusCal2(Calendar cal1, Calendar cal2) {
        long millis1 = cal1.getTimeInMillis();
        long millis2 = cal2.getTimeInMillis();
        long differenceMillis = millis1 - millis2;
        return Math.abs(differenceMillis) / 1000;
    }

    public String getCurrentDatetimeString() {
        return convertToStringDatetime(Calendar.getInstance());
    }

    public long elapsedTimeToCurrentTimeMillis(long elapsedTimeNanos) {
        long locationElapsedNanos = elapsedTimeNanos;
        long nowElapsedNanos = SystemClock.elapsedRealtimeNanos();
        long nowWallTimeMillis = System.currentTimeMillis();

        // 경과 시간 차이를 ms로 변환
        long deltaMillis = (nowElapsedNanos - locationElapsedNanos) / 1_000_000L;

        // 위치 측정 시각 (UTC 기준)
        long locationTimeMillis = nowWallTimeMillis - deltaMillis;

        return locationTimeMillis;
    }

    public String getDatetimeStringFromTimeMillis(long timeMillis) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(timeMillis);
        return convertToStringDatetime(calendar);
    }

    public int getYear(Calendar calendar) {
        return calendar.get(Calendar.YEAR);
    }

    public int getMonth(Calendar calendar) {
        return calendar.get(Calendar.MONTH)+1;
    }

    public int getDay(Calendar calendar) {
        return calendar.get(Calendar.DATE);
    }

    public int getHour(Calendar calendar) {
        return calendar.get(Calendar.HOUR);
    }

    public int getHourOf24(Calendar calendar) {
        return calendar.get(Calendar.HOUR_OF_DAY);  // 0~23
    }

    public String getAmPmKorean(Calendar calendar) {
        int amPm = calendar.get(Calendar.AM_PM);
        return amPm == 0 ? "오전":"오후";
    }

    public int getMinute(Calendar calendar) {
        return calendar.get(Calendar.MINUTE);
    }

    public int getSecond(Calendar calendar) {
        return calendar.get(Calendar.SECOND);
    }

    public Calendar getUtcCalendar() {
        return Calendar.getInstance(TimeZone.getTimeZone("UTC"));
    }

    public Calendar getKstCalendar() {
        return Calendar.getInstance(TimeZone.getTimeZone("Asia/Seoul"));
    }

    public Calendar convertUtcCalendarFromKstCalendar(Calendar kstCalendar) {
        Calendar utcCalendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        utcCalendar.setTimeInMillis(kstCalendar.getTimeInMillis());
        return utcCalendar;
    }

    public Calendar convertKstCalendarFromUtcCalendar(Calendar utcCalendar) {
        Calendar kstCalendar = Calendar.getInstance(TimeZone.getTimeZone("Asia/Seoul"));
        kstCalendar.setTimeInMillis(utcCalendar.getTimeInMillis());
        return kstCalendar;
    }

    /**
     * ISO-8601 형식의 String을 UTC TimeZone Calendar로 변환
     * @param utcTimeString ex) 2026-01-05T01:56:54Z
     * @return
     */
    public Calendar convertCalendarFromUtcTimeString(String utcTimeString) {
        Instant instant = Instant.parse(utcTimeString);
        Date date = Date.from(instant);
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        calendar.setTime(date);
        return calendar;
    }
}
