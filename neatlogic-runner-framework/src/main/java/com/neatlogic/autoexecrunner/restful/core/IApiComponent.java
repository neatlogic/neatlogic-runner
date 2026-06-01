package com.neatlogic.autoexecrunner.restful.core;

import com.alibaba.fastjson.JSONObject;
import com.neatlogic.autoexecrunner.constvalue.ApiAnonymousAccessSupportEnum;
import com.neatlogic.autoexecrunner.dto.ApiVo;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ClassUtils;

public interface IApiComponent {

    /*
     * @Description:
     * @Author: chenqiwei
     * @Date: 2021/2/9 10:30 下午
     * @Params: []
     * @Returns: java.lang.String
     **/
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    default String getClassName() {
        return ClassUtils.getUserClass(this.getClass()).getName();
    }

    /*
     * @Description:是否返回原始JSON格式
     * @Author: chenqiwei
     * @Date: 2021/2/9 10:30 下午
     * @Params: []
     * @Returns: boolean
     **/
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    default boolean isRaw() {
        return false;
    }

    /*
     * @Description:
     * @Author: chenqiwei
     * @Date: 2021/2/9 10:31 下午
     * @Params: []
     * @Returns: java.lang.String
     **/
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    String getName();


    /*
     * @Description:
     * @Author: chenqiwei
     * @Date: 2021/2/9 10:31 下午
     * @Params: [apiVo, jsonObj]
     * @Returns: java.lang.Object
     **/
    Object doService(ApiVo apiVo, JSONObject jsonObj) throws Exception;

    /*
     * @Description:
     * @Author: chenqiwei
     * @Date: 2021/2/9 10:31 下午
     * @Params: []
     * @Returns: com.alibaba.fastjson.JSONObject
     **/
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    JSONObject help();

    /**
     * 是否支持匿名访问
     *
     * @return true false
     */
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    default ApiAnonymousAccessSupportEnum supportAnonymousAccess() {
        return ApiAnonymousAccessSupportEnum.ANONYMOUS_ACCESS_FORBIDDEN;
    }
}
