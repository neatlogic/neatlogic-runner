package com.techsure.autoexecrunner.util;

import java.text.SimpleDateFormat;
import java.util.Calendar;

/**
 * @author lvzk
 * @since 2021/5/28 13:14
 **/
public class TimeUtil {
    public static final String YYYY_MM_DD_HH_MM_SS = "yyyy-MM-dd HH:mm:ss";
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
}
