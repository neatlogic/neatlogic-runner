package com.neatlogic.autoexecrunner.filter.core;


import com.neatlogic.autoexecrunner.dto.UserVo;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public interface ILoginAuthHandler {

    /**
    * @Author 89770
    * @Time 2020年11月19日  
    * @Description: 认证类型
    * @Param 
    * @return
     */
    String getType();
    
    /**
    * @Author 89770
    * @Time 2020年11月19日  
    * @Description: 认证逻辑
    * @Param 
    * @return
     */
    UserVo auth(HttpServletRequest request, HttpServletResponse response) throws Exception;

}
