package com.techsure.autoexecrunner.api.tagent;

import com.alibaba.fastjson.JSONObject;
import com.techsure.autoexecrunner.common.config.Config;
import com.techsure.autoexecrunner.common.tagent.Constant;
import com.techsure.autoexecrunner.restful.core.privateapi.PrivateApiComponentBase;
import com.techsure.autoexecrunner.service.TagentService;
import com.techsure.autoexecrunner.util.RC4Util;
import com.techsure.autoexecrunner.util.tagent.RandomUtils;
import com.techsure.tagent.client.TagentClient;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
public class TagentPasswordResetApi extends PrivateApiComponentBase {

    @Resource
    TagentService tagentService;

    @Override
    public String getName() {
        return null;
    }

    @Override
    public String getToken() {
        return "tagent/password/reset";
    }

    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        boolean status = false;
        JSONObject result = new JSONObject();
        StringBuilder execInfo = new StringBuilder();
        String credential = RC4Util.decrypt(jsonObj.getString("credential").substring(4));
        TagentClient tagentClient = new TagentClient(jsonObj.getString("ip"), Integer.parseInt(jsonObj.getString("port")), credential, 3000, 30000);
        String newPasswd = RandomUtils.getRandomStr(16);
        int execStatus = tagentClient.updateCred(newPasswd);
//        int execStatus = 0;
        if (execStatus != 0) {
            throw new RuntimeException("connect tagent reset password failed.");
        } else {
            try {
                jsonObj.put("credential", newPasswd);
                String url = String.format("%s/public/api/rest/%s", Config.CODEDRIVER_ROOT(), Constant.ACTION_UPDATE_CRED);
                status = tagentService.forwardCodedriverWeb(jsonObj, url, execInfo,false);
                //同步未成功则回滚密码
                if (!status) {
                    tagentClient.updateCred(credential);
                }
            } catch (Exception ex) {
                execInfo.append("runner exec error :").append(ExceptionUtils.getStackTrace(ex));
            }
            //!需reload使重置密码生效
            int reloadStatus = tagentClient.reload();
//            int reloadStatus =1;
            if (reloadStatus != 0) {
                status = false;
                execInfo.append("reset password succeed, but reload failed, may should restart tagent service.");
            }
        }
        if (status) {
            return jsonObj.get("data");
        } else {
            result.put("Message", execInfo);
            return result;
        }
    }
    @Override
    public boolean isRaw() {
        return true;
    }
}
