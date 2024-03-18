/*Copyright (C) 2024  深圳极向量科技有限公司 All Rights Reserved.

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.*/
package com.neatlogic.autoexecrunner.api.deploy.ci;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.neatlogic.autoexecrunner.constvalue.ApiParamType;
import com.neatlogic.autoexecrunner.constvalue.DeployCiRepoEvent;
import com.neatlogic.autoexecrunner.exception.core.ApiRuntimeException;
import com.neatlogic.autoexecrunner.exception.deploy.DeployCiGitlabTokenLostException;
import com.neatlogic.autoexecrunner.restful.annotation.Input;
import com.neatlogic.autoexecrunner.restful.annotation.Output;
import com.neatlogic.autoexecrunner.restful.annotation.Param;
import com.neatlogic.autoexecrunner.restful.core.privateapi.PrivateApiComponentBase;
import com.neatlogic.autoexecrunner.service.DeployCiService;
import com.neatlogic.autoexecrunner.util.HttpRequestUtil;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Component
public class BatchUpdateGitlabHookApi extends PrivateApiComponentBase {

    static final Logger logger = LoggerFactory.getLogger(BatchUpdateGitlabHookApi.class);

    @Resource
    DeployCiService deployCiService;

    @Override
    public String getToken() {
        return "/deploy/ci/gitlabwebhook/batchupdate";
    }

    @Override
    public String getName() {
        return "批量更新gitlab webhook";
    }

    @Input({
            @Param(name = "configList", desc = "hook配置列表", type = ApiParamType.JSONARRAY, isRequired = true),
            @Param(name = "configList.repoServerAddress", desc = "仓库服务器地址"),
            @Param(name = "configList.repoName", desc = "仓库名称"),
            @Param(name = "configList.branchFilter", desc = "分支", type = ApiParamType.STRING),
            @Param(name = "configList.callbackUrl", desc = "回调url", type = ApiParamType.JSONARRAY),
            @Param(name = "configList.username", desc = "gitlab用户", type = ApiParamType.STRING),
            @Param(name = "configList.password", desc = "gitlab密码", type = ApiParamType.STRING),
            @Param(name = "configList.event", desc = "事件", type = ApiParamType.STRING),
            @Param(name = "configList.authMode", desc = "认证方式", type = ApiParamType.STRING),
            @Param(name = "configList.action", desc = "动作类型", type = ApiParamType.ENUM, rule = "insert,delete"),
    })
    @Output({
    })
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        List<String> errorList = new ArrayList<>();
        JSONArray configList = paramObj.getJSONArray("configList");
        for (int i = 0; i < configList.size(); i++) {
            JSONObject config = configList.getJSONObject(i);
            String repoServerAddress = config.getString("repoServerAddress");
            String repoName = config.getString("repoName");
            List<String> callbackUrl = config.getJSONArray("callbackUrl").toJavaList(String.class);
            String branchFilter = config.getString("branchFilter");
            String event = config.getString("event");
            String authMode = config.getString("authMode");
            String username = config.getString("username");
            String password = config.getString("password");
            String action = config.getString("action");
            String repoNameEncode = URLEncoder.encode(repoName, StandardCharsets.UTF_8.name());
            try {
                String token = deployCiService.getGitlabToken(repoServerAddress, authMode, username, password);
                if (StringUtils.isBlank(token)) {
                    throw new DeployCiGitlabTokenLostException();
                }
                if ("insert".equals(action)) {
                    String url = deployCiService.getGitlabHookApiUrl(null, repoServerAddress, repoNameEncode, authMode, token);
                    JSONObject param = new JSONObject();
                    param.put("url", callbackUrl.get(0));
                    param.put("enable_ssl_verification", false);
                    if (DeployCiRepoEvent.POSTRECEIVE.getValue().equals(event)) {
                        param.put("push_events", true);
                        param.put("push_events_branch_filter", branchFilter);
                    }
                    HttpRequestUtil request = HttpRequestUtil.post(url);
                    request.setPayload(param.toJSONString()).sendRequest();
                    String error = request.getError();
                    if (StringUtils.isNotBlank(error)) {
                        logger.error("Gitlab webhook save failed. Request url: {}; params: {}; error: {}", url, param.toJSONString(), error);
                        throw new ApiRuntimeException(error);
                    }
                } else if ("delete".equals(action)) {
                    HttpRequestUtil request = HttpRequestUtil.get(deployCiService.getGitlabHookApiUrl(null, repoServerAddress, repoNameEncode, authMode, token)).sendRequest();
                    JSONArray hookList = request.getResultJsonArray();
                    if (CollectionUtils.isNotEmpty(hookList)) {
                        for (int j = 0; j < hookList.size(); j++) {
                            String hookId = hookList.getJSONObject(j).getString("id");
                            String url = hookList.getJSONObject(j).getString("url");
                            if (callbackUrl.contains(url)) {
                                JSONObject param = new JSONObject();
                                param.put("id", repoNameEncode);
                                param.put("hook_id", hookId);
                                HttpRequestUtil.delete(deployCiService.getGitlabHookApiUrl(hookId, repoServerAddress, repoNameEncode, authMode, token)).setPayload(param.toJSONString()).sendRequest();
                            }
                        }
                    }
                }
            } catch (Exception ex) {
                errorList.add(repoServerAddress + "/" + repoName + ": " + ex.getMessage());
            }
        }
        return errorList;
    }

}
