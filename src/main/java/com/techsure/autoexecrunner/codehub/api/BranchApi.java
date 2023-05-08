package com.techsure.autoexecrunner.codehub.api;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.techsure.autoexecrunner.codehub.dto.commit.CommitInfo;
import com.techsure.autoexecrunner.codehub.git.GitWorkingCopy;
import com.techsure.autoexecrunner.codehub.svn.SVNWorkingCopy;
import com.techsure.autoexecrunner.codehub.utils.JSONUtils;
import com.techsure.autoexecrunner.codehub.utils.WorkingCopyUtils;
import com.techsure.autoexecrunner.constvalue.ApiParamType;
import com.techsure.autoexecrunner.restful.annotation.Description;
import com.techsure.autoexecrunner.restful.annotation.Input;
import com.techsure.autoexecrunner.restful.annotation.Output;
import com.techsure.autoexecrunner.restful.annotation.Param;
import com.techsure.autoexecrunner.restful.core.privateapi.PrivateApiComponentBase;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;


import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * 分支接口
 *
 */
@Service
public class BranchApi extends PrivateApiComponentBase {

    @Override
    public String getToken() {
        return "/codehub/branch";
    }

    @Override
    public String getName() {
        return null;
    }


    @Input({
            @Param(name = "repoType", type = ApiParamType.STRING, desc = "仓库类型"),
            @Param(name = "url", type = ApiParamType.STRING, desc = "仓库url"),
            @Param(name = "username", type = ApiParamType.STRING, desc = "用户"),
            @Param(name = "password", type = ApiParamType.STRING, desc = "密码"),
            @Param(name = "startBranchName", type = ApiParamType.STRING, desc = "起始分支名"),
            @Param(name = "branchName", type = ApiParamType.STRING, desc = "分支名称"),
            @Param(name = "hasCommit", type = ApiParamType.STRING, desc = "是否已经提交 1:是;0:否 默认0"),
            @Param(name = "method", type = ApiParamType.STRING, isRequired = true, desc = "方法")
    })
    @Output({
    })
    @Description(desc = "代码仓库分支接口")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        String repoType = JSONUtils.optString(jsonObj,"repoType", "").trim().toLowerCase();
        String url = JSONUtils.optString(jsonObj,"url", "").trim();
        String username = JSONUtils.optString(jsonObj,"username", "");
        String pwd = JSONUtils.optString(jsonObj,"password", "");
        String startBranchName = JSONUtils.optString(jsonObj,"startBranchName");
        String branchName = JSONUtils.optString(jsonObj,"branchName");
        String hasCommit = JSONUtils.optString(jsonObj,"hasCommit", "0");

        String method = jsonObj.getString("method");
        String wcPath = WorkingCopyUtils.getWcPath(jsonObj);
        JSON retObj = null;
        
        if (repoType.equals("svn")) {
            SVNWorkingCopy wc = new SVNWorkingCopy(wcPath, url, username, pwd, JSONUtils.optString(jsonObj,"mainBranch", ""), JSONUtils.optString(jsonObj,"branchesPath", ""), JSONUtils.optString(jsonObj,"tagsPath", ""));

            switch (method) {
                case "save":
                    if (!wc.hasBranch(startBranchName)) {
                        throw new RuntimeException(String.format("源分支\"%s\"不存在", startBranchName));
                    }
                    if (wc.hasBranch(branchName)) {
                        throw new RuntimeException(String.format("分支\"%s\"已经存在", branchName));
                    }
                    wc.createBranch(branchName, startBranchName);
                    break;
                case "delete":
                    if (wc.hasBranch(branchName)) {
                        // 如果本地分支目录存在, 则判断是否正在做合并或者同步, 如果是则禁止删除分支
                        SVNWorkingCopy wcBranch = null;
                        try {
                            String branchRealPath = WorkingCopyUtils.getBranchRealPath(jsonObj, branchName);
                            wcBranch = new SVNWorkingCopy(branchRealPath, url, username, pwd , JSONUtils.optString(jsonObj,"mainBranch",""), JSONUtils.optString(jsonObj,"branchesPath",""), JSONUtils.optString(jsonObj,"tagsPath",""));
                            if (new File(branchRealPath).exists()) {
                                wcBranch.tryLock();
                            }
                        }  finally {
                            if (wcBranch != null) {
                                wcBranch.close();
                            }
                        }
                        wc.deleteBranch(branchName);
                    } else {
                        throw new RuntimeException("分支\"" + branchName + "\"不存在");
                    }
                    break;
                case "search":
                    JSONArray jsonArray = new JSONArray();
                    List<String> branchList = new ArrayList<>();

                    branchList = wc.getRemoteBrancheList();
                    if (CollectionUtils.isNotEmpty(branchList)) {
                        for (String s : branchList) {
                            JSONObject jsonObject = new JSONObject();
                            branchName = s;
                            jsonObject.put("name", branchName);
                            if ("1".equals(hasCommit)) {
                                List<CommitInfo> commitInfoList = wc.getCommitsForBranch(branchName, null, 1, 0, true);
                                if (CollectionUtils.isNotEmpty(commitInfoList)) {
                                    CommitInfo commitInfo = commitInfoList.get(0);
                                    jsonObject.put("commit", commitInfo);
                                }
                            }
                            jsonArray.add(jsonObject);
                        }
                    }
                    retObj = jsonArray;
                    break;
            }


            wc.close();
        } else if (repoType.equals("gitlab")) {
            GitWorkingCopy wc = new GitWorkingCopy(wcPath, url, username, pwd);
            wc.update();

            switch (method) {
                case "save":
                    if (!wc.hasBranch(startBranchName)) {
                        throw new RuntimeException(String.format("源分支\"%s\"不存在", startBranchName));
                    }
                    if (wc.hasBranch(branchName)) {
                        throw new RuntimeException(String.format("分支\"%s\"已经存在", branchName));
                    }
                    wc.createBranch(branchName, startBranchName);
                    break;
                case "delete":
                    if (wc.hasBranch(branchName)) {
                        wc.deleteBranch(branchName);
                    } else {
                        throw new RuntimeException("分支\"" + branchName + "\"不存在");
                    }
                    break;
                case "search":
                    JSONArray jsonArray = new JSONArray();
                    List<String> branchList = new ArrayList<>();

                    branchList = wc.getRemoteBranchList();
                    if (CollectionUtils.isNotEmpty(branchList)) {
                        for (String s : branchList) {
                            JSONObject jsonObject = new JSONObject();
                            branchName = s;
                            jsonObject.put("name", branchName);
                            if ("1".equals(hasCommit)) {
                                List<CommitInfo> commitInfoList = wc.getCommitsForBranch(branchName, null, 1, 0, true);
                                if (CollectionUtils.isNotEmpty(commitInfoList)) {
                                    CommitInfo commitInfo = commitInfoList.get(0);
                                    jsonObject.put("commit", commitInfo);
                                }
                            }
                            jsonArray.add(jsonObject);
                        }
                    }
                    retObj = jsonArray;
                    break;
            }

            // 需要close 防止内存泄露
            wc.close();
        }

        return retObj;
    }



/*    @Override
    public JSONArray help() {
        JSONArray jsonArray = new JSONArray();
        JSONObject jsonObj;

        ApiHelpUtils.addRepoAuthJsonObj(jsonArray);

        jsonObj = new JSONObject();
        jsonObj.put("name", "startBranchName");
        jsonObj.put("type", "String");
        jsonObj.put("desc", "源分支名称");
        jsonArray.add(jsonObj);

        jsonObj = new JSONObject();
        jsonObj.put("name", "branchName");
        jsonObj.put("type", "String");
        jsonObj.put("desc", "分支名称");
        jsonArray.add(jsonObj);
        
        jsonObj = new JSONObject();
        jsonObj.put("name", "hasCommit");
        jsonObj.put("type", "String");
        jsonObj.put("desc", "是否查询关联的提交信息，默认值是0，1：关联查询，0：不关联查询");
        jsonArray.add(jsonObj);

        ApiHelpUtils.addSVNWorkingCopyPathJsonObj(jsonArray);

        return jsonArray;
    }*/
}
