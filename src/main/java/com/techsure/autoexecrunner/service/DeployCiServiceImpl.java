package com.techsure.autoexecrunner.service;

import com.alibaba.fastjson.JSONObject;
import com.techsure.autoexecrunner.common.config.Config;
import com.techsure.autoexecrunner.constvalue.DeployCiGitlabAuthMode;
import com.techsure.autoexecrunner.exception.deploy.DeployCiGitlabAuthException;
import com.techsure.autoexecrunner.util.HttpRequestUtil;
import com.techsure.autoexecrunner.util.RC4Util;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class DeployCiServiceImpl implements DeployCiService {

    static final Logger logger = LoggerFactory.getLogger(DeployCiServiceImpl.class);

    @Override
    public String getGitlabToken(String repoServerAddress, String authMode, String username, String password) {
        String token = Config.GITLAB_PASSWORD();
        if (DeployCiGitlabAuthMode.ACCESS_TOKEN.getValue().equals(authMode)) {
            String oauthUrl = repoServerAddress + "/oauth/token";
            JSONObject authObj = new JSONObject();
            authObj.put("grant_type", "password");
            authObj.put("username", username);
            authObj.put("password", RC4Util.decrypt(password));
            HttpRequestUtil authReq = HttpRequestUtil.post(oauthUrl).setPayload(authObj.toJSONString()).sendRequest();
            String authError = authReq.getError();
            if (StringUtils.isNotBlank(authError)) {
                logger.error(authError);
                throw new DeployCiGitlabAuthException();
            } else {
                token = authReq.getResultJson().getString("access_token");
            }
        }
        return token;
    }

    @Override
    public String getGitlabHookApiUrl(String hookId, String repoServerAddress, String repoName, String authMode, String token) {
        // /api/v4/projects/:id/hooks 中的id指项目id或项目名称的url编码字符串
        String url = repoServerAddress + "/api/v4/projects/" + repoName + "/hooks";
        if (StringUtils.isNotBlank(hookId)) {
            url += ("/" + hookId);
        }
        if (DeployCiGitlabAuthMode.ACCESS_TOKEN.getValue().equals(authMode)) {
            url += ("?" + DeployCiGitlabAuthMode.ACCESS_TOKEN.getValue() + "=" + token);
        } else {
            url += ("?" + DeployCiGitlabAuthMode.PRIVATE_TOKEN.getValue() + "=" + token);
        }
        return url;
    }
}
