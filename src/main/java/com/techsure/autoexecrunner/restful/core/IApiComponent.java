package com.techsure.autoexecrunner.restful.core;

import com.alibaba.fastjson.JSONObject;
import com.techsure.autoexecrunner.dto.ApiVo;
import org.springframework.util.ClassUtils;

public interface IApiComponent {

    /*
     * @Description:
     * @Author: chenqiwei
     * @Date: 2021/2/9 10:30 下午
     * @Params: []
     * @Returns: java.lang.String
     **/
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
    JSONObject help();
}
