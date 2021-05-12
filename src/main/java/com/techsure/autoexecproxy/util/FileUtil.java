package com.techsure.autoexecproxy.util;

import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;

/**
 * @author lvzk
 * @since 2021/5/12 9:45
 **/
public class FileUtil {
    public static void saveFile(String content, String path, String contentType, String fileType) throws Exception {
        InputStream inputStream = IOUtils.toInputStream(content, StandardCharsets.UTF_8.toString());
        File file = new File(path);
        if (!file.getParentFile().exists()) {
            file.getParentFile().mkdirs();
        }
        FileOutputStream fos = new FileOutputStream(file);
        IOUtils.copyLarge(inputStream, fos);
        fos.flush();
        fos.close();
    }
}
