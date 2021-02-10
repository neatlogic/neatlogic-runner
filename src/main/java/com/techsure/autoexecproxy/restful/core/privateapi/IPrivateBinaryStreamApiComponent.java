package com.techsure.autoexecproxy.restful.core.privateapi;


import com.techsure.autoexecproxy.restful.core.IBinaryStreamApiComponent;

/*
 * @Description: 内部二进制接口定义
 * @Author: chenqiwei
 * @Date: 2021/2/10 9:39 上午
 * @Params: * @param null:
 * @Returns: * @return: null
 **/
public interface IPrivateBinaryStreamApiComponent extends IBinaryStreamApiComponent {
    /*
     * @Description:
     * @Author: chenqiwei
     * @Date: 2021/2/10 9:39 上午
     * @Params: []
     * @Returns: java.lang.String
     **/
    public String getToken();

}
