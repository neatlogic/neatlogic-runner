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

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;

/**
 * @author laiwt
 * @since 2022/5/27 10:54 上午
 **/
@Component
public class FileCopyApi extends PrivateApiComponentBase {

    static Logger logger = LoggerFactory.getLogger(FileCopyApi.class);

    @Override
    public String getToken() {
        return "/file/copy";
    }

    @Override
    public String getName() {
        return "文件复制";
    }

    @Input({
            @Param(name = "src", type = ApiParamType.STRING, desc = "源文件路径", isRequired = true),
            @Param(name = "dest", type = ApiParamType.STRING, desc = "目标文件路径(复制文件时，dest末尾需要指定文件名)", isRequired = true)
    })
    @Output({})
    @Description(desc = "文件复制")
    @Override
    public Object myDoService(JSONObject jsonObj) throws IOException {
        String src = jsonObj.getString("src");
        String dest = jsonObj.getString("dest");
        src = FileUtil.getFullAbsolutePath(src);
        dest = FileUtil.getFullAbsolutePath(dest);
        File scrFile = new File(src);
        if (scrFile.isFile()) {
            Files.copy(Paths.get(src), Paths.get(dest), StandardCopyOption.REPLACE_EXISTING);
        } else {
            Path scrPath = Paths.get(src);
            Path destPath = Paths.get(dest).resolve(scrPath.getFileName());
            Files.walkFileTree(scrPath, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                    // 在目标文件夹中创建dir对应的子文件夹
                    Path subDir = (dir.compareTo(scrPath) == 0) ? destPath : destPath.resolve(dir.subpath(scrPath.getNameCount(), dir.getNameCount()));
                    Files.createDirectories(subDir);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    Files.copy(file, destPath.resolve(file.subpath(scrPath.getNameCount(), file.getNameCount())), StandardCopyOption.REPLACE_EXISTING);
                    return FileVisitResult.CONTINUE;
                }
            });
        }
        return null;
    }

}
