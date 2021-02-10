package com.techsure.autoexecproxy.dto;


import com.techsure.autoexecproxy.constvalue.ApiParamType;
import com.techsure.autoexecproxy.restful.annotation.EntityField;

import java.util.List;

public class ApiHandlerVo {
    @EntityField(name = "处理器", type = ApiParamType.STRING)
    private String handler;
    @EntityField(name = "名称", type = ApiParamType.STRING)
    private String name;
    @EntityField(name = "处理器类型", type = ApiParamType.STRING)
    private String type;
    private List<ApiVo> interfaceList;


    public List<ApiVo> getInterfaceList() {
        return interfaceList;
    }

    public void setInterfaceList(List<ApiVo> interfaceList) {
        this.interfaceList = interfaceList;
    }

    public String getHandler() {
        return handler;
    }

    public void setHandler(String handler) {
        this.handler = handler;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }


    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
