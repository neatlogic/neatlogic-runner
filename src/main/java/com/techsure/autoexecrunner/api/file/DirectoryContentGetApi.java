/*
 * Copyright(c) 2021 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */
package com.techsure.autoexecrunner.api.file;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.techsure.autoexecrunner.constvalue.ApiParamType;
import com.techsure.autoexecrunner.constvalue.FileType;
import com.techsure.autoexecrunner.exception.file.FileIsNotDirectoryException;
import com.techsure.autoexecrunner.restful.annotation.Description;
import com.techsure.autoexecrunner.restful.annotation.Input;
import com.techsure.autoexecrunner.restful.annotation.Output;
import com.techsure.autoexecrunner.restful.annotation.Param;
import com.techsure.autoexecrunner.restful.core.privateapi.PrivateApiComponentBase;
import com.techsure.autoexecrunner.util.FileUtil;
import com.techsure.autoexecrunner.util.TimeUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Set;

/**
 * @author laiwt
 * @since 2022/5/27 10:54 上午
 **/
@Component
public class DirectoryContentGetApi extends PrivateApiComponentBase {

    static Logger logger = LoggerFactory.getLogger(DirectoryContentGetApi.class);

    @Override
    public String getToken() {
        return "/file/directory/content/get";
    }

    @Override
    public String getName() {
        return "获取目录内容";
    }

    @Input({
            @Param(name = "home", type = ApiParamType.STRING, desc = "虚拟根目录路径", isRequired = true),
            @Param(name = "path", type = ApiParamType.STRING, desc = "目录路径", isRequired = true),
            @Param(name = "type", type = ApiParamType.ENUM, rule = "f,d,l", desc = "文件类型过滤(f:文件;d:目录;l:链接)")
    })
    @Output({
            @Param(name = "name", type = ApiParamType.STRING, desc = "文件名"),
            @Param(name = "type", type = ApiParamType.STRING, desc = "文件类型"),
            @Param(name = "size", type = ApiParamType.LONG, desc = "文件大小"),
            @Param(name = "fcd", type = ApiParamType.LONG, desc = "最后修改时间"),
            @Param(name = "fcdText", type = ApiParamType.STRING, desc = "最后修改时间(格式化为yyyy-MM-dd HH:mm:ss)"),
            @Param(name = "permission", type = ApiParamType.STRING, desc = "文件权限"),
            @Param(name = "hasItems", type = ApiParamType.INTEGER, desc = "目录是否有内容")
    })
    @Description(desc = "获取目录内容")
    @Override
    public Object myDoService(JSONObject jsonObj) throws IOException {
        String home = jsonObj.getString("home");
        String path = jsonObj.getString("path");
        String type = jsonObj.getString("type");
        String homePath = FileUtil.getFullAbsolutePath(home);
        File destFile;
        File homeFile = new File(homePath).getCanonicalFile();
        // 如果home尚不存在，则创建home并返回home目录
        if (!homeFile.exists()) {
            Files.createDirectories(Paths.get(homePath));
            destFile = homeFile;
        } else {
            path = homePath + (path.startsWith(File.separator) ? path : File.separator + path);
            destFile = new File(path).getCanonicalFile();
        }
        if (!destFile.isDirectory()) {
            throw new FileIsNotDirectoryException(path);
        }
        JSONArray jsonArray = new JSONArray();
        String[] dirItems = destFile.list();
        if (dirItems != null && dirItems.length > 0) {
            JSONObject object = new JSONObject();
            for (String name : dirItems) {
                if (name.equals(".") || name.equals("..")) {
                    continue;
                }
                try {
                    String fType = FileType.FILE.getValue();
                    Path filePath = Paths.get(destFile.getPath() + File.separator + name);
                    if (type == null) {
                        if (Files.isDirectory(filePath)) {
                            fType = FileType.DIRECTORY.getValue();
                        } else if (Files.isSymbolicLink(filePath)) {
                            fType = FileType.LINK.getValue();
                            object.put("target", Files.readSymbolicLink(filePath).toString());
                        }
                    } else if (FileType.FILE.getValue().equals(type)) {
                        if (!Files.isRegularFile(filePath)) {
                            continue;
                        }
                    } else if (FileType.DIRECTORY.getValue().equals(type)) {
                        fType = FileType.DIRECTORY.getValue();
                        if (!Files.isDirectory(filePath)) {
                            continue;
                        }
                    } else if (FileType.LINK.getValue().equals(type)) {
                        fType = FileType.LINK.getValue();
                        if (!Files.isSymbolicLink(filePath)) {
                            continue;
                        }
                    }
                    object = new JSONObject();
                    object.put("name", name);
                    object.put("type", fType);
                    object.put("size", Files.size(filePath));
                    object.put("fcd", Files.getLastModifiedTime(filePath).toMillis());
                    object.put("fcdText", TimeUtil.getTimeToDateString(Files.getLastModifiedTime(filePath).toMillis(), TimeUtil.YYYY_MM_DD_HH_MM_SS));
                    object.put("permission", getPermission(filePath));
                    object.put("hasItems", 0);
                    File file = new File(destFile.getPath() + File.separator + name);
                    if (file.isDirectory() && file.list() != null && file.list().length > 0) {
                        object.put("hasItems", 1);
                    }
                    jsonArray.add(object);
                } catch (IOException ex) {
                    logger.error(ex.getMessage(), ex);
                }
            }
        }
        return jsonArray;
    }

    /**
     * 获取文件权限信息
     *
     * @param filePath 文件路径
     * @return
     * @throws IOException
     */
    private String getPermission(Path filePath) throws IOException {
        Set<PosixFilePermission> perms = Files.getPosixFilePermissions(filePath);
        StringBuilder permsStr = new StringBuilder();

        if (perms.contains(PosixFilePermission.OWNER_READ))
            permsStr.append("r");
        else
            permsStr.append("-");

        if (perms.contains(PosixFilePermission.OWNER_WRITE))
            permsStr.append("w");
        else
            permsStr.append("-");

        if (perms.contains(PosixFilePermission.OWNER_EXECUTE))
            permsStr.append("x");
        else
            permsStr.append("-");

        if (perms.contains(PosixFilePermission.GROUP_READ))
            permsStr.append("r");
        else
            permsStr.append("-");

        if (perms.contains(PosixFilePermission.GROUP_WRITE))
            permsStr.append("w");
        else
            permsStr.append("-");

        if (perms.contains(PosixFilePermission.GROUP_EXECUTE))
            permsStr.append("x");
        else
            permsStr.append("-");

        if (perms.contains(PosixFilePermission.OTHERS_READ))
            permsStr.append("r");
        else
            permsStr.append("-");

        if (perms.contains(PosixFilePermission.OTHERS_WRITE))
            permsStr.append("w");
        else
            permsStr.append("-");

        if (perms.contains(PosixFilePermission.OTHERS_EXECUTE))
            permsStr.append("x");
        else
            permsStr.append("-");

        return permsStr.toString();
    }

}
