/*
 * Copyright(c) 2021 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */
package com.techsure.autoexecrunner.api.deploy.ci;

import com.alibaba.fastjson.JSONObject;
import com.techsure.autoexecrunner.constvalue.ApiParamType;
import com.techsure.autoexecrunner.exception.core.ApiRuntimeException;
import com.techsure.autoexecrunner.exception.deploy.DeployCiGitlabTokenLostException;
import com.techsure.autoexecrunner.restful.annotation.Input;
import com.techsure.autoexecrunner.restful.annotation.Output;
import com.techsure.autoexecrunner.restful.annotation.Param;
import com.techsure.autoexecrunner.restful.core.privateapi.PrivateApiComponentBase;
import com.techsure.autoexecrunner.service.DeployCiService;
import com.techsure.autoexecrunner.util.HttpRequestUtil;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Component
public class DeleteGitlabCiHookApi extends PrivateApiComponentBase {

    static final Logger logger = LoggerFactory.getLogger(DeleteGitlabCiHookApi.class);

    @Resource
    DeployCiService deployCiService;

    @Override
    public String getToken() {
        return "/deploy/ci/gitlabwebhook/delete";
    }

    @Override
    public String getName() {
        return "删除gitlab ci webhook";
    }

    @Input({
            @Param(name = "hookId", desc = "webhook id", type = ApiParamType.LONG, isRequired = true),
            @Param(name = "repoServerAddress", desc = "仓库服务器地址", type = ApiParamType.STRING, isRequired = true),
            @Param(name = "repoName", desc = "仓库名称", type = ApiParamType.STRING, isRequired = true),
            @Param(name = "authMode", desc = "认证方式", type = ApiParamType.STRING, isRequired = true),
            @Param(name = "username", desc = "gitlab用户", type = ApiParamType.STRING),
            @Param(name = "password", desc = "gitlab密码", type = ApiParamType.STRING),
    })
    @Output({
    })
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        String hookId = paramObj.getString("hookId");
        String repoServerAddress = paramObj.getString("repoServerAddress");
        String repoName = paramObj.getString("repoName");
        String authMode = paramObj.getString("authMode");
        String username = paramObj.getString("username");
        String password = paramObj.getString("password");
        String token = deployCiService.getGitlabToken(repoServerAddress, authMode, username, password);
        if (StringUtils.isBlank(token)) {
            throw new DeployCiGitlabTokenLostException();
        }
        repoName = URLEncoder.encode(repoName, StandardCharsets.UTF_8.name());
        JSONObject param = new JSONObject();
        param.put("id", repoName);
        param.put("hook_id", hookId);
        String url = deployCiService.getGitlabHookApiUrl(hookId, repoServerAddress, repoName, authMode, token);
        HttpRequestUtil request = HttpRequestUtil.delete(url).setPayload(param.toJSONString()).sendRequest();
        String error = request.getError();
        if (StringUtils.isNotBlank(error)) {
            logger.error("Gitlab webhook delete failed. Request url: {}; params: {}; error: {}", url, param.toJSONString(), error);
            throw new ApiRuntimeException(error);
        }
        return null;
    }


}
