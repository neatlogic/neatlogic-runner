package com.neatlogic.autoexecrunner.restful.core.privateapi;


import com.neatlogic.autoexecrunner.restful.core.IApiComponent;

/**
 * @Title: IPrivateApiComponent
 * @Description: 系统公告下发接口
 * @Author: chenqiwei
 * @Date: 2021/1/13 18:01

 **/
public interface IPrivateApiComponent extends IApiComponent {
    /*
     * @Description:
     * @Author: chenqiwei
     * @Date: 2021/2/10 9:41 上午
     * @Params: []
     * @Returns: java.lang.String
     **/
    String getToken();

}
