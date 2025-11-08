/*
 *
 * Copyright (C) 2025  TechSure Co., Ltd.  All Rights Reserved.
 * This file is part of the NeatLogic software.
 * Licensed under the NeatLogic Sustainable Use License (NSUL), Version 4.x – 2025.
 * You may use this file only in compliance with the License.
 * See the LICENSE file distributed with this work for the full license text.
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 */
package com.neatlogic.autoexecrunner.api.file;

import com.alibaba.fastjson.JSONObject;
import com.neatlogic.autoexecrunner.constvalue.ApiParamType;
import com.neatlogic.autoexecrunner.restful.annotation.Description;
import com.neatlogic.autoexecrunner.restful.annotation.Input;
import com.neatlogic.autoexecrunner.restful.annotation.Output;
import com.neatlogic.autoexecrunner.restful.annotation.Param;
import com.neatlogic.autoexecrunner.restful.core.privateapi.PrivateApiComponentBase;
import com.neatlogic.autoexecrunner.util.FileUtil;
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
