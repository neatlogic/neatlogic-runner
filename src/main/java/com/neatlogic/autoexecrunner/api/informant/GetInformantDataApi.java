package com.neatlogic.autoexecrunner.api.informant;

import com.alibaba.fastjson.JSONObject;
import com.neatlogic.autoexecrunner.constvalue.ApiParamType;
import com.neatlogic.autoexecrunner.restful.annotation.Description;
import com.neatlogic.autoexecrunner.restful.annotation.Input;
import com.neatlogic.autoexecrunner.restful.annotation.Output;
import com.neatlogic.autoexecrunner.restful.annotation.Param;
import com.neatlogic.autoexecrunner.restful.core.privateapi.PrivateApiComponentBase;
import com.neatlogic.autoexecrunner.util.HttpRequestUtil;
import org.apache.commons.collections4.MapUtils;
import org.springframework.stereotype.Service;

@Service
public class GetInformantDataApi extends PrivateApiComponentBase {

    @Override
    public String getName() {
        return "获取tagent配置";
    }


    @Input({
            @Param(name = "url", type = ApiParamType.STRING, desc = "url", isRequired = true),
            @Param(name = "ip", type = ApiParamType.STRING, desc = "informant ip", isRequired = true),
            @Param(name = "port", type = ApiParamType.INTEGER, desc = "informant 端口", isRequired = true),
            @Param(name = "method", type = ApiParamType.STRING, desc = "请求方法", isRequired = true),
            @Param(name = "param", type = ApiParamType.JSONOBJECT, desc = "参数"),
            @Param(name = "header", type = ApiParamType.JSONOBJECT, desc = "请求头", isRequired = true)
    })
    @Output({
            @Param(name = "port", type = ApiParamType.JSONOBJECT, desc = "tagent 配置", isRequired = true)
    })
    @Description(desc = "tagent 获取配置")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {

        String url = jsonObj.getString("url");
        String ip = jsonObj.getString("ip");
        Integer port = jsonObj.getInteger("port");
        String method = jsonObj.getString("method");
        JSONObject param = jsonObj.getJSONObject("param");
        JSONObject header = jsonObj.getJSONObject("header");
        HttpRequestUtil httpUtil;
        String finalUrl = "http://" + ip + ":" + port + "/" + url;
        if (method.equals("get")) {
            httpUtil = HttpRequestUtil.get(finalUrl);
            if (MapUtils.isNotEmpty(param)) {
                httpUtil.setQueryString(param);
            }
        } else {
            httpUtil = HttpRequestUtil.post(finalUrl);
            if (MapUtils.isNotEmpty(param)) {
                httpUtil.setPayload(param.toJSONString());
            }
        }
        if (MapUtils.isNotEmpty(header)) {
            for (String headerKey : header.keySet()) {
                httpUtil.addHeader(headerKey, header.get(headerKey).toString());
            }
        }
        System.out.println("发送请求到：" + finalUrl + " 数据：" + param + " 头部：" + header);
        httpUtil.sendRequest();
        if (httpUtil.getError() != null) {
            System.out.println("请求异常：" + httpUtil.getError());
        }
        String returnStr = httpUtil.getResult();
        System.out.println("返回值：" + returnStr);
        return returnStr;
    }

    @Override
    public String getToken() {
        return "informant/data/get";
    }

}
