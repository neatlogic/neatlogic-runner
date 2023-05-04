package com.techsure.autoexecrunner.codehub.api;

import java.io.OutputStream;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.alibaba.fastjson.JSONObject;
import com.techsure.autoexecrunner.codehub.git.GitWorkingCopy;
import com.techsure.autoexecrunner.codehub.svn.SVNWorkingCopy;
import com.techsure.autoexecrunner.codehub.utils.JSONUtils;
import com.techsure.autoexecrunner.codehub.utils.WorkingCopyUtils;
import com.techsure.autoexecrunner.constvalue.ApiParamType;
import com.techsure.autoexecrunner.restful.annotation.Input;
import com.techsure.autoexecrunner.restful.annotation.Param;
import com.techsure.autoexecrunner.restful.core.privateapi.PrivateBinaryStreamApiComponentBase;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

/**
 * 〈功能概述〉仓库文件下载
 *
 * @className: DownloadApi
 * @author: fengtao
 * @date: 2021-01-06 13:35:24
 */
@Service
public class DownloadApi extends PrivateBinaryStreamApiComponentBase {

    @Override
    public String getToken() {
        return "codehub/download";
    }


    @Override
    public String getName() {
        return "仓库文件下载";
    }

/*    @Override
    public JSONArray help() {
        JSONArray jsonArray = new JSONArray();

        ApiHelpUtils.addRepoAuthJsonObj(jsonArray);

        JSONObject jsonObj = new JSONObject();
        jsonObj.put("name", "branchName");
        jsonObj.put("type", "String");
        jsonObj.put("desc", "分支名称");
        jsonArray.add(jsonObj);

        jsonObj = new JSONObject();
        jsonObj.put("name", "commitId");
        jsonObj.put("type", "String");
        jsonObj.put("desc", "commitId");
        jsonArray.add(jsonObj);

        jsonObj = new JSONObject();
        jsonObj.put("name", "filePath");
        jsonObj.put("type", "String");
        jsonObj.put("desc", "需要下载的文件路径");
        jsonArray.add(jsonObj);

        jsonObj = new JSONObject();
        jsonObj.put("name", "filePathType");
        jsonObj.put("type", "String");
        jsonObj.put("desc", "需要下载的文件类型(file:文件，dir：目录)");
        jsonArray.add(jsonObj);

        ApiHelpUtils.addSVNWorkingCopyPathJsonObj(jsonArray);

        return jsonArray;
    }*/

    @Input({
            @Param(name = "repoType", type = ApiParamType.STRING, desc = "仓库类型"),
            @Param(name = "url", type = ApiParamType.STRING, desc = "url"),
            @Param(name = "username", type = ApiParamType.STRING, desc = "用户"),
            @Param(name = "password", type = ApiParamType.STRING, desc = "密码"),
            @Param(name = "branchName", type = ApiParamType.STRING, desc = "分支名"),
            @Param(name = "commitId", type = ApiParamType.STRING, desc = "commit id"),
            @Param(name = "filePath", type = ApiParamType.STRING, desc = "需要下载的文件"),
            @Param(name = "filePathType", type = ApiParamType.STRING, desc = "需要下载的文件类型(file:文件，dir：目录)"),
            @Param(name = "mainBranch", type = ApiParamType.STRING, desc = "主分支"),
            @Param(name = "branchesPath", type = ApiParamType.STRING, desc = "分支路径"),
            @Param(name = "tagsPath", type = ApiParamType.STRING, desc = "标签路径")
    })
    @Override
    public Object myDoService(JSONObject jsonObj, HttpServletRequest request, HttpServletResponse response) throws Exception {
        String repoType = JSONUtils.optString(jsonObj,"repoType", "").trim().toLowerCase();
        String url = JSONUtils.optString(jsonObj,"url", "").trim();
        String username = JSONUtils.optString(jsonObj,"username", "");
        String pwd = JSONUtils.optString(jsonObj,"password", "");
        String branchName = jsonObj.getString("branchName");
        String commitId = JSONUtils.optString(jsonObj,"commitId");
        String filePath = JSONUtils.optString(jsonObj,"filePath");
        String filePathType = JSONUtils.optString(jsonObj,"filePathType","file");

        String wcPath = WorkingCopyUtils.getWcPath(jsonObj);

        OutputStream os = response.getOutputStream();

        if (repoType.equals("svn")) {
            SVNWorkingCopy wc = null;
            try {
                wc = new SVNWorkingCopy(wcPath, url, username, pwd , JSONUtils.optString(jsonObj,"mainBranch",""), JSONUtils.optString(jsonObj,"branchesPath",""), JSONUtils.optString(jsonObj,"tagsPath",""));
                Long rev = wc.resolveBranch(branchName);
                if(StringUtils.isNotEmpty(commitId)){
                    rev = Long.parseLong(commitId);
                }

                if(!StringUtils.startsWith(filePath,wc.getRealBranchPath(branchName))){
                    filePath = wc.getRealBranchPath(branchName) + "/" + filePath;
                }
                if (StringUtils.equals(filePathType,"file")){
                    wc.downloadFile(rev,filePath,os);
                }
                else{
                    wc.downloadDirArchive(rev,filePath,os);
                }
            } finally {
                if(wc != null){
                    // 需要close 防止内存泄露
                    wc.close();
                    os.close();
                }
            }

        } else if (repoType.equals("gitlab")) {
            GitWorkingCopy wc = null;
            try {
                wc = new GitWorkingCopy(wcPath, url, username, pwd);
                wc.update();
                if(StringUtils.isEmpty(commitId)){
                    commitId = wc.resolveBranch(branchName);
                }

                if(StringUtils.isNotEmpty(filePath)){
                    if (StringUtils.equals(filePathType,"file")){
                        wc.downloadFile(commitId,filePath,os);
                    }
                    else{
                        wc.downloadDir(commitId,filePath,os);
                    }
                }
                else{
                    wc.downloadRepo(commitId,os);
                }
            } finally {
                if (wc != null) {
                    // 需要close 防止内存泄露
                    wc.close();
                    os.close();
                }
            }
        }

        return null;
    }
}
