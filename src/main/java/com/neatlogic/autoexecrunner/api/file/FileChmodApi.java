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
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.util.HashSet;
import java.util.Set;

/**
 * @author laiwt
 * @since 2022/5/27 10:54 上午
 **/
@Component
public class FileChmodApi extends PrivateApiComponentBase {

    static Logger logger = LoggerFactory.getLogger(FileChmodApi.class);

    @Override
    public String getToken() {
        return "/file/chmod";
    }

    @Override
    public String getName() {
        return "文件权限修改";
    }

    @Input({
            @Param(name = "path", type = ApiParamType.STRING, desc = "文件路径", isRequired = true),
            @Param(name = "mode", type = ApiParamType.STRING, desc = "权限(e.g:rwxr-xr-x)", isRequired = true)
    })
    @Output({
            @Param(name = "success", type = ApiParamType.INTEGER, desc = "是否成功(1:是;0:否)")
    })
    @Description(desc = "文件权限修改")
    @Override
    public Object myDoService(JSONObject jsonObj) throws IOException {
        String path = jsonObj.getString("path");
        String mode = jsonObj.getString("mode");
        path = FileUtil.getFullAbsolutePath(path);
        Path filePath = Paths.get(path);
        Set<PosixFilePermission> perms = new HashSet<>();
        char[] chars = mode.toCharArray();

        if (chars[0] == 'r') {
            perms.add(PosixFilePermission.OWNER_READ);
        }
        if (chars[1] == 'w') {
            perms.add(PosixFilePermission.OWNER_WRITE);
        }
        if (chars[2] == 'x') {
            perms.add(PosixFilePermission.OWNER_EXECUTE);
        }

        if (chars[3] == 'r') {
            perms.add(PosixFilePermission.GROUP_READ);
        }
        if (chars[4] == 'w') {
            perms.add(PosixFilePermission.GROUP_WRITE);
        }
        if (chars[5] == 'x') {
            perms.add(PosixFilePermission.GROUP_EXECUTE);
        }

        if (chars[6] == 'r') {
            perms.add(PosixFilePermission.OTHERS_READ);
        }
        if (chars[7] == 'w') {
            perms.add(PosixFilePermission.OTHERS_WRITE);
        }
        if (chars[8] == 'x') {
            perms.add(PosixFilePermission.OTHERS_EXECUTE);
        }

        if (Files.exists(filePath))
            Files.setPosixFilePermissions(filePath, perms);

        return null;
    }

}
