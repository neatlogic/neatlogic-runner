package com.techsure.autoexecrunner.restful.core.privateapi;


import com.techsure.autoexecrunner.restful.core.IJsonStreamApiComponent;

/*
 * @Description:  系统json接口定义
 * @Author: chenqiwei
 * @Date: 2021/2/10 9:40 上午
 * @Params: * @param null:
 * @Returns: * @return: null
 **/
public interface IPrivateJsonStreamApiComponent extends IJsonStreamApiComponent {

    /*
     * @Description:
     * @Author: chenqiwei
     * @Date: 2021/2/10 9:40 上午
     * @Params: []
     * @Returns: java.lang.String
     **/
    String getToken();
}
