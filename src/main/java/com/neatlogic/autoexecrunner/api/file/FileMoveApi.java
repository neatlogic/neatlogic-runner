/*Copyright (C) 2024  深圳极向量科技有限公司 All Rights Reserved.

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.*/
package com.neatlogic.autoexecrunner.api.file;

import com.alibaba.fastjson.JSONObject;
import com.neatlogic.autoexecrunner.restful.annotation.Description;
import com.neatlogic.autoexecrunner.restful.annotation.Input;
import com.neatlogic.autoexecrunner.restful.annotation.Output;
import com.neatlogic.autoexecrunner.restful.annotation.Param;
import com.neatlogic.autoexecrunner.constvalue.ApiParamType;
import com.neatlogic.autoexecrunner.restful.core.privateapi.PrivateApiComponentBase;
import com.neatlogic.autoexecrunner.util.FileUtil;
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
