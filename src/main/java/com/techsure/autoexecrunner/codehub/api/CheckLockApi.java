package com.techsure.autoexecrunner.codehub.api;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.techsure.autoexecrunner.codehub.git.GitWorkingCopy;
import com.techsure.autoexecrunner.codehub.svn.SVNWorkingCopy;
import com.techsure.autoexecrunner.codehub.utils.JSONUtils;
import com.techsure.autoexecrunner.codehub.utils.WorkingCopyUtils;
import com.techsure.autoexecrunner.constvalue.ApiParamType;
import com.techsure.autoexecrunner.exception.core.ApiRuntimeException;
import com.techsure.autoexecrunner.restful.annotation.Description;
import com.techsure.autoexecrunner.restful.annotation.Input;
import com.techsure.autoexecrunner.restful.annotation.Output;
import com.techsure.autoexecrunner.restful.annotation.Param;
import com.techsure.autoexecrunner.restful.core.privateapi.PrivateApiComponentBase;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;


import java.io.File;

@Service
public class CheckLockApi extends PrivateApiComponentBase {
    Logger logger = LoggerFactory.getLogger(CheckLockApi.class);

    @Override
    public String getToken() {
        return "codehub/checklock";
    }

    @Override
    public String getName() {
        return "校验文件是否上锁";
    }

    @Input({
            @Param(name = "repoType", type = ApiParamType.STRING, desc = "仓库类型"),
            @Param(name = "url", type = ApiParamType.STRING, desc = "仓库url"),
            @Param(name = "username", type = ApiParamType.STRING, desc = "用户"),
            @Param(name = "password", type = ApiParamType.STRING, desc = "密码"),
            @Param(name = "targetBranch", type = ApiParamType.STRING, desc = "目标分支"),
            @Param(name = "checkBranchPathExists", type = ApiParamType.BOOLEAN, desc = "分支路径是否已经给checkout过"),
            @Param(name = "targetBranches", type = ApiParamType.JSONARRAY, desc = "目标分支列表"),
    })
    @Output({
    })
    @Description(desc = "校验文件是否上锁接口")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        String repoType = JSONUtils.optString(jsonObj,"repoType", "").trim().toLowerCase();
        String url = JSONUtils.optString(jsonObj,"url", "").trim();
        String username = JSONUtils.optString(jsonObj,"username", "");
        String pwd = JSONUtils.optString(jsonObj,"password", "");

        String targetBranch = JSONUtils.optString(jsonObj,"targetBranch", "").trim();
        // 检查分支路径是否已经checkout出来过
        boolean checkBranchPathExists = JSONUtils.optBoolean(jsonObj,"checkBranchPathExists", true);
        // 多个分支同时检查
        JSONArray targetBranches = jsonObj.getJSONArray("targetBranches");
        String wcPath = WorkingCopyUtils.getWcPath(jsonObj);
        JSONObject ret = new JSONObject();

        if (repoType.equals("svn")) {
            if (CollectionUtils.isEmpty(targetBranches)) {
                targetBranches = new JSONArray();
                targetBranches.add(targetBranch);
            }

            for (int i = 0; i < targetBranches.size(); i++) {
                String checkBranch = targetBranches.getString(i);

                if (StringUtils.isBlank(checkBranch)) {
                    continue;
                }
                SVNWorkingCopy wc = null;
                try {
                    String branchRealPath = WorkingCopyUtils.getBranchRealPath(jsonObj, checkBranch);
                    wc = new SVNWorkingCopy(branchRealPath, url, username, pwd, JSONUtils.optString(jsonObj,"mainBranch", ""), JSONUtils.optString(jsonObj,"branchesPath", ""), JSONUtils.optString(jsonObj,"tagsPath", ""));

                    if (checkBranchPathExists) {
                        if (!new File(branchRealPath).exists()) {
                            throw new ApiRuntimeException(String.format("当前仓库 '%s' 的分支 '%s' 目录不存在，请先同步仓库分支", url, checkBranch));
                        }
                        if (!new File(branchRealPath, ".svn").exists()) {
                            // 虽然分支目录下没有.svn但是可能上级目录存在, 此时单纯判断.svn不准确
                            try {
                                // 仅用于测试目录是否是有效的workingcopy目录
                                wc.getStatus(checkBranch);
                            } catch (Exception e) {
                                if (StringUtils.contains(ExceptionUtils.getRootCauseMessage(e), "is not a working copy")) {
                                    throw new ApiRuntimeException(String.format("当前仓库 '%s' 的分支 '%s' 目录不存在，请先同步仓库分支", url, checkBranch));
                                }
                                logger.error(e.getMessage(), e);
                            }
                        }
                    }

                    wc.tryLock();
                } finally {
                    if (wc != null) {
                        wc.close();
                    }
                }

            }

        } else if (repoType.equals("gitlab")) {
            GitWorkingCopy wc = null;
            try {
                wc = new GitWorkingCopy(wcPath, url, username, pwd);
                wc.tryLock();

            } finally {
                if (wc != null) {
                    wc.close();
                }
            }
        }

        return ret;
    }
}
