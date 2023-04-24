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

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;

/**
 * @author laiwt
 * @since 2022/5/27 10:54 上午
 **/
@Component
public class FileDeleteApi extends PrivateApiComponentBase {

    static Logger logger = LoggerFactory.getLogger(FileDeleteApi.class);

    @Override
    public String getToken() {
        return "/file/delete";
    }

    @Override
    public String getName() {
        return "删除文件";
    }

    @Input({
            @Param(name = "path", type = ApiParamType.STRING, desc = "文件路径", isRequired = true)
    })
    @Output({})
    @Description(desc = "删除文件")
    @Override
    public Object myDoService(JSONObject jsonObj) throws IOException {
        String path = jsonObj.getString("path");
        path = FileUtil.getFullAbsolutePath(path);
        File opFile = new File(path);
        if (opFile.isFile()) {
            opFile.delete();
        } else {
            Path directory = Paths.get(path);
            Files.walkFileTree(directory, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    Files.delete(file);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                    Files.delete(dir);
                    return FileVisitResult.CONTINUE;
                }
            });
        }
        return null;
    }

}
