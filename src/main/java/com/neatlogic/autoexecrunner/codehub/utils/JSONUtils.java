package com.neatlogic.autoexecrunner.codehub.utils;

import com.alibaba.fastjson.JSONObject;

/**
 * @Title: JSONUtils
 * @Description: 模仿net.sf.json中opt默认值用法
 * @Author: yangy
 * @Date: 2023/4/25 18:20
 **/
public class JSONUtils {

    public static String optString(JSONObject jsonObject, String key, String defaultValue) {
        return jsonObject.containsKey(key) ? jsonObject.getString(key) : defaultValue;
    }

    public static String optString(JSONObject jsonObject, String key) {
        return jsonObject.getString(key);
    }

    public static int optInt(JSONObject jsonObject, String key, int defaultValue) {
        return jsonObject.containsKey(key) ? jsonObject.getIntValue(key) : defaultValue;
    }

    public static int optInt(JSONObject jsonObject, String key) {
        return jsonObject.getIntValue(key);
    }


    public static Long optLong(JSONObject jsonObject, String key, Long defaultValue) {
        return jsonObject.containsKey(key) ? jsonObject.getLongValue(key) : defaultValue;
    }

    public static Long optLong(JSONObject jsonObject, String key) {
        return jsonObject.getLongValue(key);
    }

    public static Boolean optBoolean(JSONObject jsonObject, String key, Boolean defaultValue) {
        return jsonObject.containsKey(key) ? jsonObject.getBooleanValue(key) : defaultValue;
    }

    public static Boolean optBoolean(JSONObject jsonObject, String key) {
        return jsonObject.getBooleanValue(key);
    }

/*    public static <T> T opt(JSONObject jsonObject, String key, T defaultValue){
        return jsonObject.containsKey(key) ? (T)jsonObject.get(key) : defaultValue;
    }

    public static void main(String[] args) {
        JSONObject jsonObject = new JSONObject();
        System.out.println(opt(jsonObject,"aa",""));
        System.out.println(opt(jsonObject,"aa",22));
    }*/

}
