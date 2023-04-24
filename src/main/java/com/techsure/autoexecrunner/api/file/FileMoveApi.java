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
import java.nio.file.StandardCopyOption;

/**
 * @author laiwt
 * @since 2022/5/27 10:54 上午
 **/
@Component
public class FileMoveApi extends PrivateApiComponentBase {

    static Logger logger = LoggerFactory.getLogger(FileMoveApi.class);

    @Override
    public String getToken() {
        return "/file/move";
    }

    @Override
    public String getName() {
        return "文件移动或重命名";
    }

    @Input({
            @Param(name = "src", type = ApiParamType.STRING, desc = "源文件路径", isRequired = true),
            @Param(name = "dest", type = ApiParamType.STRING, desc = "目标文件路径(移动文件时，dest末尾需要带上文件名；移动目录时，若不指定目录名，则把src中的文件移动到dest根路径)", isRequired = true)
    })
    @Output({
            @Param(name = "success", type = ApiParamType.INTEGER, desc = "是否成功(1:是;0:否)")
    })
    @Description(desc = "文件移动或重命名")
    @Override
    public Object myDoService(JSONObject jsonObj) throws IOException {
        String src = jsonObj.getString("src");
        String dest = jsonObj.getString("dest");
        src = FileUtil.getFullAbsolutePath(src);
        dest = FileUtil.getFullAbsolutePath(dest);
        Files.move(Paths.get(src), Paths.get(dest), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        return null;
    }

}
