/*
 *
 * Copyright (C) 2025  TechSure Co., Ltd.  All Rights Reserved.
 * This file is part of the NeatLogic software.
 * Licensed under the NeatLogic Sustainable Use License (NSUL), Version 4.x – 2025.
 * You may use this file only in compliance with the License.
 * See the LICENSE file distributed with this work for the full license text.
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 */
package com.neatlogic.autoexecrunner.api.deploy.ci;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.JSONArray;
import com.neatlogic.autoexecrunner.exception.deploy.DeployCiGitlabHookException;
import com.neatlogic.autoexecrunner.constvalue.ApiParamType;
import com.neatlogic.autoexecrunner.constvalue.DeployCiRepoEvent;
import com.neatlogic.autoexecrunner.exception.deploy.DeployCiGitlabTokenLostException;
import com.neatlogic.autoexecrunner.restful.annotation.Input;
import com.neatlogic.autoexecrunner.restful.annotation.Output;
import com.neatlogic.autoexecrunner.restful.annotation.Param;
import com.neatlogic.autoexecrunner.restful.core.privateapi.PrivateApiComponentBase;
import com.neatlogic.autoexecrunner.service.DeployCiService;
import com.neatlogic.autoexecrunner.util.HttpRequestUtil;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

@Component
public class SaveGitlabCiHookApi extends PrivateApiComponentBase {

    static final Logger logger = LoggerFactory.getLogger(SaveGitlabCiHookApi.class);

    @Resource
    DeployCiService deployCiService;

    @Override
    public String getToken() {
        return "/deploy/ci/gitlabwebhook/save";
    }

    @Override
    public String getName() {
        return "保存gitlab ci webhook";
    }

    @Input({
            @Param(name = "ciId", desc = "持续集成配置id", type = ApiParamType.LONG, isRequired = true),
            @Param(name = "reconcile", desc = "按回调地址核对已存在的 Webhook", type = ApiParamType.BOOLEAN),
            @Param(name = "hookId", desc = "webhook id", type = ApiParamType.LONG),
            @Param(name = "callbackHost", desc = "gitlab webhook回调主机地址", type = ApiParamType.STRING, isRequired = true),
            @Param(name = "repoServerAddress", desc = "仓库服务器地址", type = ApiParamType.STRING, isRequired = true),
            @Param(name = "repoName", desc = "仓库名称", type = ApiParamType.STRING, isRequired = true),
            @Param(name = "branchFilter", desc = "分支", type = ApiParamType.STRING),
            @Param(name = "event", desc = "事件", type = ApiParamType.STRING, isRequired = true),
            @Param(name = "authMode", desc = "认证方式", type = ApiParamType.STRING, isRequired = true),
            @Param(name = "username", desc = "gitlab用户", type = ApiParamType.STRING),
            @Param(name = "password", desc = "gitlab密码", type = ApiParamType.STRING),
    })
    @Output({
    })
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        Long ciId = paramObj.getLong("ciId");
        String hookId = paramObj.getString("hookId");
        String callbackHost = paramObj.getString("callbackHost");
        String repoServerAddress = paramObj.getString("repoServerAddress");
        String repoName = paramObj.getString("repoName");
        String branchFilter = paramObj.getString("branchFilter");
        String event = paramObj.getString("event");
        String authMode = paramObj.getString("authMode");
        String username = paramObj.getString("username");
        String password = paramObj.getString("password");
        String token = deployCiService.getGitlabToken(repoServerAddress, authMode, username, password);
        if (StringUtils.isBlank(token)) {
            throw new DeployCiGitlabTokenLostException();
        }
        String url;
        JSONObject param = new JSONObject();
        repoName = URLEncoder.encode(repoName, StandardCharsets.UTF_8.name());
        param.put("id", repoName);
        if (StringUtils.isNotBlank(hookId)) {
            url = deployCiService.getGitlabHookApiUrl(hookId, repoServerAddress, repoName, authMode, token);
            // 先验证hookId是否存在
            HttpRequestUtil request = HttpRequestUtil.get(url).sendRequest();
            JSONObject resultJson = request.getResultJson();
            if (MapUtils.isNotEmpty(resultJson) && Objects.equals(hookId, resultJson.getString("id"))) {
                param.put("hook_id", hookId);
            } else {
                hookId = null;
            }
        }
        String callback = String.format("%s/anonymous/api/rest/deploy/ci/gitlab/event/callback/%s?ciId=%s", callbackHost, paramObj.getString("tenant"), ciId);
        if (StringUtils.isBlank(hookId) && paramObj.getBooleanValue("reconcile")) {
            // 上次创建可能已经到达 GitLab，但 Runner 未能将结果返回给平台。
            String collection = deployCiService.getGitlabHookApiUrl(null, repoServerAddress, repoName, authMode, token);
            hookId = findExistingHook(collection, callback, ciId);
        }
        url = deployCiService.getGitlabHookApiUrl(hookId, repoServerAddress, repoName, authMode, token);
        param.put("url", callback);
        param.put("enable_ssl_verification", false);
        if (DeployCiRepoEvent.POSTRECEIVE.getValue().equals(event)) {
            param.put("push_events", true);
            param.put("push_events_branch_filter", branchFilter);
        }
        HttpRequestUtil request;
        if (StringUtils.isNotBlank(hookId)) {
            request = HttpRequestUtil.put(url);
        } else {
            request = HttpRequestUtil.post(url);
        }
        request.setPayload(param.toJSONString()).sendRequest();
        String error = request.getError();
        JSONObject resultJson = request.getResultJson();
        if (StringUtils.isNotBlank(error)) {
            logger.error("GitLab Webhook 保存失败，CI ID={}，HTTP 状态={}", ciId, request.getResponseCode());
            throw new DeployCiGitlabHookException(ciId, request.getResponseCode());
        }
        if (resultJson == null || resultJson.getLong("id") == null) throw new DeployCiGitlabHookException(ciId, request.getResponseCode());
        return resultJson.getLong("id");
    }

    /** 仅复用同一租户同一 CI 的回调，在列表读取失败时保留待重试状态。 */
    private String findExistingHook(String collection, String callback, Long ciId) {
        for (int page = 1; ; page++) {
            HttpRequestUtil request = HttpRequestUtil.get(collection + "&per_page=100&page=" + page).sendRequest();
            if (request.getResponseCode() != 200) throw new DeployCiGitlabHookException(ciId, request.getResponseCode());
            JSONArray hooks = request.getResultJsonArray();
            if (hooks == null) throw new DeployCiGitlabHookException(ciId, request.getResponseCode());
            for (int index = 0; index < hooks.size(); index++) {
                JSONObject hook = hooks.getJSONObject(index);
                if (callback.equals(hook.getString("url"))) return hook.getString("id");
            }
            if (hooks.size() < 100) return null;
        }
    }
}
