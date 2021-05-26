package com.techsure.autoexecproxy.util;

import com.techsure.autoexecproxy.common.config.Config;
import com.techsure.autoexecproxy.dto.FileTailerVo;
import com.techsure.autoexecproxy.exception.MkdirPermissionDeniedException;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.List;

/**
 * @author lvzk
 * @since 2021/5/12 9:45
 **/
public class FileUtil {
    private static final Logger logger = LoggerFactory.getLogger(FileUtil.class);
    public static void saveFile(String content, String path, String contentType, String fileType) throws Exception {
        InputStream inputStream = IOUtils.toInputStream(content, StandardCharsets.UTF_8.toString());
        File file = new File(path);
        if (!file.getParentFile().exists()) {
            if(!file.getParentFile().mkdirs()){
                throw new MkdirPermissionDeniedException();
            }

        }
        FileOutputStream fos = new FileOutputStream(file);
        IOUtils.copyLarge(inputStream, fos);
        fos.flush();
        fos.close();
    }

    public static FileTailerVo tailLog(String logPath, Long logPos, String direction) {
        if (logPos == null) {
            logPos = 0L;
        }
        FileTailerVo fileTailer = new FileTailerVo(logPos);
        File logFile = new File(logPath);
        StringBuilder content = new StringBuilder();
        if (logFile.isFile()) {
            long fileLen = logFile.length();
            try (RandomAccessFile fis = new RandomAccessFile(logFile, "r")) {
                if (logPos == -1) {
                    if (fileLen > Config.LOGTAIL_BUFLEN()) {
                        logPos = fileLen - Config.LOGTAIL_BUFLEN();
                        fis.seek(logPos);
                        while (fis.readByte() != 10) {
                        }
                        logPos = fis.getFilePointer();
                    } else {
                        logPos = 0L;
                    }
                } else if (logPos == -2 || logPos == Long.MAX_VALUE) {
                    logPos = fileLen;
                }
                if ("up".equals(direction)) {
                    fileTailer.setEndPos(logPos);
                    logPos = logPos - Config.LOGTAIL_BUFLEN();
                    if (logPos > 0) {
                        fis.seek(logPos);
                        while (fis.read() != 10) {
                        }
                        logPos = fis.getFilePointer();
                    } else {
                        logPos = 0L;
                    }
                } else {
                    fileTailer.setEndPos(logPos + Config.LOGTAIL_BUFLEN());
                }
                fileTailer.setStartPos(logPos);
                fis.seek(logPos);
                String line;
                while ((fileTailer.getEndPos() == -1L || fis.getFilePointer() < fileTailer.getEndPos())
                        && (line = fis.readLine()) != null) {
                    line = new String(line.getBytes(StandardCharsets.ISO_8859_1));
                    fileTailer.setLastLine(line);
                    content.append("<div>").append(line).append("</div>");
                }
                fileTailer.setLogPos(fis.getFilePointer());
                fileTailer.setEndPos(fis.getFilePointer());
            } catch (IOException ex) {
                logger.error("ge tail log for file:" + logFile.getAbsolutePath() + "failed", ex);
            }
        } else {
            fileTailer.setLogPos(0L);
            fileTailer.setStartPos(0L);
        }

        fileTailer.setTailContent(content.toString());

        return fileTailer;
    }

}
