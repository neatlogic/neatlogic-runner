/*
 * Copyright(c) 2021 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */
package com.techsure.autoexecrunner.api.deploy.ci;

import com.alibaba.fastjson.JSONObject;
import com.techsure.autoexecrunner.constvalue.ApiParamType;
import com.techsure.autoexecrunner.constvalue.DeployCiRepoEvent;
import com.techsure.autoexecrunner.exception.core.ApiRuntimeException;
import com.techsure.autoexecrunner.exception.deploy.DeployCiGitlabTokenLostException;
import com.techsure.autoexecrunner.restful.annotation.Input;
import com.techsure.autoexecrunner.restful.annotation.Output;
import com.techsure.autoexecrunner.restful.annotation.Param;
import com.techsure.autoexecrunner.restful.core.privateapi.PrivateApiComponentBase;
import com.techsure.autoexecrunner.service.DeployCiService;
import com.techsure.autoexecrunner.util.HttpRequestUtil;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
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
        url = deployCiService.getGitlabHookApiUrl(hookId, repoServerAddress, repoName, authMode, token);
        param.put("url", String.format("%s/anonymous/api/rest/deploy/ci/gitlab/event/callback/%s?ciId=%s", callbackHost, paramObj.getString("tenant"), ciId));
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
            logger.error("Gitlab webhook save failed. Request url: {}; params: {}; error: {}", url, param.toJSONString(), error);
            throw new ApiRuntimeException(error);
        }
        return resultJson.getLong("id");
    }

}
