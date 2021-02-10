package com.techsure.autoexecproxy.restful.core.privateapi;


import com.techsure.autoexecproxy.restful.core.IApiComponent;

/**
 * @Title: IPrivateApiComponent
 * @Package: com.techsure.autoexecproxy.restful.core.IApiComponent
 * @Description: 系统公告下发接口
 * @Author: chenqiwei
 * @Date: 2021/1/13 18:01
 * Copyright(c) 2021 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 **/
public interface IPrivateApiComponent extends IApiComponent {
    /*
     * @Description:
     * @Author: chenqiwei
     * @Date: 2021/2/10 9:41 上午
     * @Params: []
     * @Returns: java.lang.String
     **/
    public String getToken();

}
