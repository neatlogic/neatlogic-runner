package com.neatlogic.autoexecrunner.api.resourcepool;

import com.alibaba.fastjson.JSONObject;
import com.neatlogic.autoexecrunner.constvalue.ApiParamType;
import com.neatlogic.autoexecrunner.restful.annotation.Description;
import com.neatlogic.autoexecrunner.restful.annotation.Input;
import com.neatlogic.autoexecrunner.restful.annotation.Output;
import com.neatlogic.autoexecrunner.restful.annotation.Param;
import com.neatlogic.autoexecrunner.restful.core.privateapi.PrivateApiComponentBase;
import com.neatlogic.autoexecrunner.service.ResourcepoolPingService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
public class PingIpStatusApi extends PrivateApiComponentBase {

    @Resource
    private ResourcepoolPingService resourcepoolPingService;

    @Override
    public String getName() {
        return "资源池IP ping检测";
    }

    @Override
    public String getToken() {
        return "resourcepool/ip/ping/status";
    }

    @Input({
            @Param(name = "network", type = ApiParamType.STRING, desc = "IPv4网段IP，配合mask使用"),
            @Param(name = "mask", type = ApiParamType.INTEGER, desc = "IPv4掩码，32代表检测单个IPv4"),
            @Param(name = "ip", type = ApiParamType.STRING, desc = "单个IP地址，支持IPv4/IPv6"),
            @Param(name = "ipList", type = ApiParamType.JSONARRAY, desc = "IP地址数组，支持IPv4/IPv6"),
            @Param(name = "async", type = ApiParamType.BOOLEAN, desc = "是否异步执行"),
            @Param(name = "callbackUrl", type = ApiParamType.STRING, desc = "异步执行完成后的NeatLogic回调地址")
    })
    @Output({
            @Param(name = "pingStatusArray", type = ApiParamType.JSONARRAY, desc = "同步执行时返回的IP ping结果数组"),
            @Param(name = "message", type = ApiParamType.STRING, desc = "异步执行提示")
    })
    @Description(desc = "IP资源池，执行IPv4网段或指定IP列表ping检测")
    @Override
    public Object myDoService(JSONObject jsonObj) {
        return resourcepoolPingService.ping(jsonObj);
    }
}
