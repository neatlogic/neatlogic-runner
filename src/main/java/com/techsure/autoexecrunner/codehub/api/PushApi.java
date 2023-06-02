package com.techsure.autoexecrunner.codehub.api;

import com.alibaba.fastjson.JSONObject;
import com.techsure.autoexecrunner.codehub.git.GitWorkingCopy;
import com.techsure.autoexecrunner.codehub.utils.JSONUtils;
import com.techsure.autoexecrunner.codehub.utils.WorkingCopyUtils;
import com.techsure.autoexecrunner.constvalue.ApiParamType;
import com.techsure.autoexecrunner.exception.core.ApiRuntimeException;
import com.techsure.autoexecrunner.restful.annotation.Description;
import com.techsure.autoexecrunner.restful.annotation.Input;
import com.techsure.autoexecrunner.restful.annotation.Param;
import com.techsure.autoexecrunner.restful.core.privateapi.PrivateApiComponentBase;
import org.springframework.stereotype.Service;

/**
 * 
 * @author fengt
 * @date   2020年8月14日 下午7:44:07
 * @Description 执行代码push
 *
 */
@Service
public class PushApi extends PrivateApiComponentBase {

	@Override
	public String getToken() {
		return "/codehub/push";
	}

	@Override
	public String getName() {
		return "推送接口";
	}

	@Input({
			@Param(name = "repoType", type = ApiParamType.STRING, desc = "仓库类型"),
			@Param(name = "url", type = ApiParamType.STRING, desc = "url"),
			@Param(name = "username", type = ApiParamType.STRING, desc = "用户"),
			@Param(name = "password", type = ApiParamType.STRING, desc = "密码"),
			@Param(name = "targetBranch", type = ApiParamType.STRING, desc = "目标分支")
	})
	@Description(desc = "推送接口")
	@Override
	public Object myDoService(JSONObject jsonObj) throws Exception {
		String repoType = JSONUtils.optString(jsonObj,"repoType", "").trim().toLowerCase();
		String url = JSONUtils.optString(jsonObj,"url", "").trim();
		String username = JSONUtils.optString(jsonObj,"username", "");
		String pwd = JSONUtils.optString(jsonObj,"password", "");
		
		String targetBranch = JSONUtils.optString(jsonObj,"targetBranch", "").trim();

		String wcPath = WorkingCopyUtils.getWcPath(jsonObj);
		JSONObject ret = new JSONObject();
		
		if (repoType.equals("svn")) {
			throw new ApiRuntimeException("此功能暂未实现");
		} else if (repoType.equals("gitlab")) {
			GitWorkingCopy wc = new GitWorkingCopy(wcPath, url, username, pwd);
            wc.checkout(targetBranch, true);
			try {
                wc.push();
                ret.put("status", "finish");
            }
			catch(Exception e){
                ret.put("status", "failed");
                ret.put("message", e.getMessage());
            }
			finally {
			    wc.close();
            }
		}
		
		return ret;
	}
	

/*	@Override
	public JSONArray help() {
        JSONArray jsonArray = new JSONArray();

        ApiHelpUtils.addRepoAuthJsonObj(jsonArray);

		JSONObject jsonObj = new JSONObject();
        jsonObj.put("name", "targetBranch");
        jsonObj.put("type", "String");
        jsonObj.put("desc", "目标分支");
        jsonArray.add(jsonObj);

        ApiHelpUtils.addSVNWorkingCopyPathJsonObj(jsonArray);

        return jsonArray;
	}*/
	


}
