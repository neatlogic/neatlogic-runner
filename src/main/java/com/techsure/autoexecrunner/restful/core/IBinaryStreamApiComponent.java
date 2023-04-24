package com.techsure.autoexecrunner.restful.core;

import com.alibaba.fastjson.JSONObject;
import com.techsure.autoexecrunner.constvalue.ApiAnonymousAccessSupportEnum;
import com.techsure.autoexecrunner.dto.ApiVo;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public interface IBinaryStreamApiComponent {
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    String getId();
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    String getName();

    // true时返回格式不再包裹固定格式
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    default boolean isRaw() {
        return false;
    }


    Object doService(ApiVo interfaceVo, JSONObject paramObj, HttpServletRequest request, HttpServletResponse response) throws Exception;

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    JSONObject help();

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    default ApiAnonymousAccessSupportEnum supportAnonymousAccess() {
        return ApiAnonymousAccessSupportEnum.ANONYMOUS_ACCESS_FORBIDDEN;
    }
}
