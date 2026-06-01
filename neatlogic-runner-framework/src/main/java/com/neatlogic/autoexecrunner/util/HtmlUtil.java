package com.neatlogic.autoexecrunner.util;

/**
 * @author lvzk
 * @since 2021/5/13 15:38
 **/
public class HtmlUtil {
    public static String encodeHtml(String str) {
        if (str != null && !"".equals(str)) {
            str = str.replace("&", "&amp;");
            str = str.replace("<", "&lt;");
            str = str.replace(">", "&gt;");
            str = str.replace("'", "&#39;");
            str = str.replace("\"", "&quot;");
            return str;
        }
        return "";
    }
}
