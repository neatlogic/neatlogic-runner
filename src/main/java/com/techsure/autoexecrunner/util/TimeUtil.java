package com.techsure.autoexecrunner.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 * @author lvzk
 * @since 2021/5/28 13:14
 **/
public class TimeUtil {
    public static final String YYYY_MM_DD_HH_MM_SS = "yyyy-MM-dd HH:mm:ss";
    public static final String YYYYMMDD_HHMMSS = "yyyyMMdd-HHmmss";
    public static String getTimeToDateString(long time, String format) {
        try {
            String dateStr = "";
            if (time > 0) {
                Calendar calendar = Calendar.getInstance();
                calendar.setTimeInMillis(time);
                SimpleDateFormat sf = new SimpleDateFormat(format);
                dateStr = sf.format(calendar.getTime());
            }
            return dateStr;
        } catch (Exception e) {
            return null;
        }
    }

    public static Date convertStringToDate(String dataStr, String format) throws ParseException {
        Date date = null;
        try {
            SimpleDateFormat sdf = new SimpleDateFormat(format);
            date = sdf.parse(dataStr);
        } catch (Exception ex) {
            return null;
        }
        return date;
    }

    public static String convertDateToString(Date date, String format) {
        SimpleDateFormat sdf = new SimpleDateFormat(format);
        return sdf.format(date);
    }
}
