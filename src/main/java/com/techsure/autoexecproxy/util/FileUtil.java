package com.techsure.autoexecproxy.util;

import com.techsure.autoexecproxy.common.config.Config;
import com.techsure.autoexecproxy.dto.FileTailerVo;
import com.techsure.autoexecproxy.dto.FileVo;
import com.techsure.autoexecproxy.exception.MkdirPermissionDeniedException;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.sql.Time;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

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
            if (!file.getParentFile().mkdirs()) {
                throw new MkdirPermissionDeniedException();
            }

        }
        FileOutputStream fos = new FileOutputStream(file);
        IOUtils.copyLarge(inputStream, fos);
        fos.flush();
        fos.close();
    }

    /**
     * logPos = -1 && direction =  down 则读取最新内容
     *
     * @param logPath   文件地址
     * @param logPos    读取点
     * @param direction 方向
     * @return 读取的内容
     */
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
                    String time = line.substring(0,8);
                    String info = line.substring(9);
                    content.append(String.format("<div><span class='text-tip'>%s</span> %s</div>",time,info));
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


    /**
     * 读取文件内容
     *
     * @param filePath 文件路径
     * @return 文件内容
     */
    public static String getReadFileContent(String filePath) {
        if (filePath == null || "".equals(filePath)) {
            return null;
        } else {
            String result = "";
            FileInputStream fr = null;
            BufferedReader filebr = null;
            InputStreamReader in = null;
            File desFile = new File(filePath);
            try {
                if (desFile.isFile() && desFile.exists()) {
                    StringBuilder str = new StringBuilder();
                    fr = new FileInputStream(desFile);
                    in = new InputStreamReader(fr, StandardCharsets.UTF_8);
                    filebr = new BufferedReader(in);
                    String inLine = "";
                    while ((inLine = filebr.readLine()) != null) {
                        str.append(inLine);
                    }
                    result = str.toString();
                } else {
                    result = "";
                }
            } catch (Exception ex) {
                result = ex.getMessage();
            } finally {
                if (fr != null) {
                    try {
                        fr.close();
                    } catch (IOException ignored) {
                    }
                }
                if (filebr != null) {
                    try {
                        filebr.close();
                    } catch (IOException ignored) {
                    }
                }
                if (in != null) {
                    try {
                        in.close();
                    } catch (IOException ignored) {

                    }
                }
            }
            return result;
        }
    }


    /**
     * 根据文件夹路径读取文件列表
     * @param filepath 文件夹路径
     * @return 文件列表
     */
    public static List<FileVo> readFileList(String filepath) {
        List<FileVo> fileVoList = new ArrayList<>();
        try {
            File file = new File(filepath);
            String[] fileList = file.list();
            for (int i = 0; i < Objects.requireNonNull(fileList).length; i++) {
                File readFile = new File(filepath + System.getProperty("file.separator") + fileList[i]);
                FileVo f = new FileVo();
                f.setFileName(readFile.getName());
                f.setFilePath(readFile.getAbsolutePath());
                f.setLastModified(TimeUtil.getTimeToDateString(file.lastModified(), TimeUtil.YYYY_MM_DD_HH_MM_SS));
                f.setIsDirectory(1);
                fileVoList.add(f);
            }
        } catch (Exception e) {
            logger.error("read file Exception:" + e.getMessage());
        }
        return fileVoList;
    }

}
