package com.neatlogic.autoexecrunner.codehub.api;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.neatlogic.autoexecrunner.codehub.dto.commit.CommitInfo;
import com.neatlogic.autoexecrunner.codehub.git.GitWorkingCopy;
import com.neatlogic.autoexecrunner.codehub.svn.SVNWorkingCopy;
import com.neatlogic.autoexecrunner.codehub.utils.JSONUtils;
import com.neatlogic.autoexecrunner.codehub.utils.WorkingCopyUtils;
import com.neatlogic.autoexecrunner.restful.annotation.Description;
import com.neatlogic.autoexecrunner.restful.annotation.Input;
import com.neatlogic.autoexecrunner.restful.annotation.Param;
import com.neatlogic.autoexecrunner.constvalue.ApiParamType;
import com.neatlogic.autoexecrunner.exception.core.ApiRuntimeException;
import com.neatlogic.autoexecrunner.restful.core.privateapi.PrivateApiComponentBase;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 标签接口
 *
 */
@Service
public class TagApi extends PrivateApiComponentBase {

    @Override
    public String getToken() {
        return "codehub/tag";
    }

    @Override
    public String getName() {
        return "标签接口";
    }


    @Input({
            @Param(name = "repoType", type = ApiParamType.STRING, desc = "仓库类型"),
            @Param(name = "url", type = ApiParamType.STRING, desc = "url"),
            @Param(name = "username", type = ApiParamType.STRING, desc = "用户"),
            @Param(name = "password", type = ApiParamType.STRING, desc = "密码"),
            @Param(name = "tagName", type = ApiParamType.STRING, desc = "标签名"),
            @Param(name = "branchName", type = ApiParamType.STRING, desc = "分支名"),
            @Param(name = "hasCommit", type = ApiParamType.STRING, desc = "是否提交"),
            @Param(name = "method", type = ApiParamType.STRING, desc = "标签操作方法")
    })
    @Description(desc = "标签接口")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        String repoType = JSONUtils.optString(jsonObj, "repoType", "").trim().toLowerCase();
        String url = JSONUtils.optString(jsonObj, "url", "").trim();
        String username = JSONUtils.optString(jsonObj, "username", "");
        String pwd = JSONUtils.optString(jsonObj, "password", "");
        String tagName = JSONUtils.optString(jsonObj, "tagName");
        String branchName = JSONUtils.optString(jsonObj, "branchName");
        String hasCommit = JSONUtils.optString(jsonObj, "hasCommit", "0");

        String wcPath = WorkingCopyUtils.getWcPath(jsonObj);
        String method = jsonObj.getString("method");
        JSON retObj = null;

        if (repoType.equals("svn")) {
            SVNWorkingCopy wc = new SVNWorkingCopy(wcPath, url, username, pwd, JSONUtils.optString(jsonObj, "mainBranch", ""), JSONUtils.optString(jsonObj, "branchesPath", ""), JSONUtils.optString(jsonObj, "tagsPath", ""));

            switch (method) {
                case "delete":
                    if (wc.hasTag(tagName)) {
                        wc.deleteTag(tagName);
                    } else {
                        throw new ApiRuntimeException("标签\"" + tagName + "\"不存在");
                    }
                    break;
                case "save":
                    if (!wc.hasBranch(branchName)) {
                        throw new ApiRuntimeException(String.format("分支\"%s\"不存在", branchName));
                    }
                    if (wc.hasTag(tagName)) {
                        throw new ApiRuntimeException(String.format("标签\"%s\"已经存在", tagName));
                    }
                    wc.createTag(tagName, branchName);
                    break;
                case "search":
                    JSONArray jsonArray = new JSONArray();
                    List<String> tagList = new ArrayList<>();
                    tagList = wc.getRemoteTagList();
                    if (CollectionUtils.isNotEmpty(tagList)) {
                        for (int i = 0; i < tagList.size(); i++) {
                            JSONObject jsonObject = new JSONObject();
                            tagName = tagList.get(i);
                            jsonObject.put("name", tagName);
                            if ("1".equals(hasCommit)) {
                                List<CommitInfo> commitInfoList = wc.getCommitsForTag(tagName, null, 1, 0L, true);
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
        } else if (repoType.equals("gitlab")) {
            GitWorkingCopy wc = new GitWorkingCopy(wcPath, url, username, pwd);
            wc.update();

            switch (method) {
                case "delete":
                    if (wc.hasTag(tagName)) {
                        wc.deleteTag(tagName);
                    } else {
                        throw new ApiRuntimeException("标签\"" + tagName + "\"不存在");
                    }
                    break;
                case "save":
                    if (!wc.hasBranch(branchName)) {
                        throw new ApiRuntimeException(String.format("分支\"%s\"不存在", branchName));
                    }
                    if (wc.hasTag(tagName)) {
                        throw new ApiRuntimeException(String.format("标签\"%s\"已经存在", tagName));
                    }
                    wc.createTag(tagName, branchName);
                    break;
                case "search":
                    JSONArray jsonArray = new JSONArray();
                    List<String> tagList = new ArrayList<>();
                    tagList = wc.getRemoteTagList();
                    if (CollectionUtils.isNotEmpty(tagList)) {
                        for (int i = 0; i < tagList.size(); i++) {
                            JSONObject jsonObject = new JSONObject();
                            tagName = tagList.get(i);
                            jsonObject.put("name", tagName);
                            if ("1".equals(hasCommit)) {
                                List<CommitInfo> commitInfoList = wc.getCommitsForTag(tagName, null, 1, 0, true);
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
}


/*    @Override
    public JSONArray help() {
        JSONArray jsonArray = new JSONArray();

        ApiHelpUtils.addRepoAuthJsonObj(jsonArray);

        JSONObject jsonObj = new JSONObject();
        jsonObj.put("name", "tagName");
        jsonObj.put("type", "String");
        jsonObj.put("desc", "标签名称");
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
