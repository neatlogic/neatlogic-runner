package com.techsure.autoexecrunner.codehub.utils;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

/**
 * 〈功能概述〉接口帮助工具类
 *
 * @className: HelpUtils
 * @author: fengtao
 * @date: 2020-08-28 11:15:03
 */
public class ApiHelpUtils {

    /**
     * 添加仓库认证相关信息
     * @param jsonArray
     */
    public static void addRepoAuthJsonObj(JSONArray jsonArray) {
        JSONObject jsonObj = new JSONObject();
        jsonObj.put("name", "repoType");
        jsonObj.put("type", "String");
        jsonObj.put("desc", "repository type, 'svn' or 'gitlab'");
        jsonArray.add(jsonObj);

        jsonObj = new JSONObject();
        jsonObj.put("name", "url");
        jsonObj.put("type", "String");
        jsonObj.put("desc", "remote repository url");
        jsonArray.add(jsonObj);

        jsonObj = new JSONObject();
        jsonObj.put("name", "username");
        jsonObj.put("type", "String");
        jsonObj.put("desc", "repository username");
        jsonArray.add(jsonObj);

        jsonObj = new JSONObject();
        jsonObj.put("name", "password");
        jsonObj.put("type", "String");
        jsonObj.put("desc", "repository username password");
        jsonArray.add(jsonObj);
    }

    /**
     * 添加SVNWorkingCopy路径相关帮助信息
     * @param jsonArray
     */
    public static void addSVNWorkingCopyPathJsonObj(JSONArray jsonArray) {
        JSONObject jsonObj = new JSONObject();
        jsonObj.put("name", "mainBranch");
        jsonObj.put("type", "String");
        jsonObj.put("desc", "主干分支，git仓库默认master，svn仓库默认trunk");
        jsonArray.add(jsonObj);

        jsonObj = new JSONObject();
        jsonObj.put("name", "branchesPath");
        jsonObj.put("type", "String");
        jsonObj.put("desc", "SVN分支路径");
        jsonArray.add(jsonObj);

        jsonObj = new JSONObject();
        jsonObj.put("name", "tagsPath");
        jsonObj.put("type", "String");
        jsonObj.put("desc", "SVN标签路径");
        jsonArray.add(jsonObj);
    }


}
