/**
 * 
 */
package com.techsure.autoexecrunner.codehub.api;

import com.alibaba.fastjson.JSONObject;
import com.techsure.autoexecrunner.common.config.Config;
import com.techsure.autoexecrunner.restful.core.privateapi.PrivateApiComponentBase;
import org.springframework.stereotype.Service;


/**
 * @ClassName   ConfigApi
 * @Description 获取 codehub-proxy 相关配置接口
 * @author      zouye
 * @date        2021-05-19   
 *    
 */
@Service
public class ConfigApi extends PrivateApiComponentBase {

	@Override
	public String getToken() {
		return "codehub/config/get";
	}

	@Override
	public String getName() {
		return "获取配置接口";
	}


	@Override
	public Object myDoService(JSONObject jsonObj) throws Exception {
		JSONObject retObj = new JSONObject();

		retObj.put("WORKING_COPY_PATH", Config.WORKING_COPY_PATH);
		retObj.put("MERGE_CONCURRENT_SIZE", Config.MERGE_CONCURRENT_SIZE);

		return retObj;
	}
}
