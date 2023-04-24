/*
 * Copyright(c) 2021 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */
package com.techsure.autoexecrunner.api.file;

import com.alibaba.fastjson.JSONObject;
import com.techsure.autoexecrunner.constvalue.ApiParamType;
import com.techsure.autoexecrunner.restful.annotation.Description;
import com.techsure.autoexecrunner.restful.annotation.Input;
import com.techsure.autoexecrunner.restful.annotation.Output;
import com.techsure.autoexecrunner.restful.annotation.Param;
import com.techsure.autoexecrunner.restful.core.privateapi.PrivateApiComponentBase;
import com.techsure.autoexecrunner.util.FileUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * @author laiwt
 * @since 2022/5/27 10:54 上午
 **/
@Component
public class DirectoryCreateApi extends PrivateApiComponentBase {

    static Logger logger = LoggerFactory.getLogger(DirectoryCreateApi.class);

    @Override
    public String getToken() {
        return "/file/directory/create";
    }

    @Override
    public String getName() {
        return "新建目录";
    }

    @Input({
            @Param(name = "path", type = ApiParamType.STRING, desc = "目录路径", isRequired = true)
    })
    @Output({})
    @Description(desc = "新建目录")
    @Override
    public Object myDoService(JSONObject jsonObj) throws IOException {
        String path = jsonObj.getString("path");
        path = FileUtil.getFullAbsolutePath(path);
        Files.createDirectory(Paths.get(path));
        return null;
    }

}
