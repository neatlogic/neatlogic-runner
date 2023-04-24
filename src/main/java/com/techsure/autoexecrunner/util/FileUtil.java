package com.techsure.autoexecrunner.util;

import com.techsure.autoexecrunner.common.config.Config;
import com.techsure.autoexecrunner.constvalue.FileLogType;
import com.techsure.autoexecrunner.dto.FileLineVo;
import com.techsure.autoexecrunner.dto.FileTailerVo;
import com.techsure.autoexecrunner.dto.FileVo;
import com.techsure.autoexecrunner.exception.MkdirPermissionDeniedException;
import com.techsure.autoexecrunner.exception.job.ExecuteJobFileNotFoundException;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * @author lvzk
 * @since 2021/5/12 9:45
 **/
public class FileUtil {

    private static int BUF_SIZE = 16 * 1024;

    private static final Logger logger = LoggerFactory.getLogger(FileUtil.class);

    public static void saveFile(String content, String path) throws Exception {
        InputStream inputStream = IOUtils.toInputStream(content + System.getProperty("line.separator"), StandardCharsets.UTF_8.toString());
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
    @Deprecated
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
                    if (StringUtils.isNotBlank(line) && line.length() > 8) {
                        String time = line.substring(0, 8);
                        String infoClass = StringUtils.EMPTY;
                        String info = StringUtils.EMPTY;
                        if (line.length() > 9) {
                            info = line.substring(9);
                            if (info.startsWith("ERROR")) {
                                infoClass = "text-danger";
                            } else if (info.startsWith("WARN")) {
                                infoClass = "text-warning";
                            } else if (info.startsWith("FINEST")) {
                                infoClass = "text-success";
                            }
                        }
                        content.append(String.format("<div><span class='text-tip'>%s</span> <span class='%s'>%s</span></div>", time, infoClass, info));
                    }
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
     * logPos = -1 && direction =  down 则读取最新内容
     *
     * @param logPath   文件地址
     * @param logPos    读取点
     * @param direction 方向
     * @param encoding  字符编码
     * @param status    状态，如果是中止状态则读取剩下的日志，不受 LOGTAIL_BUFLEN 长度限制
     * @return 读取的内容
     */
    public static FileTailerVo tailLogWithoutHtml(String logPath, Long logPos, String direction, String encoding, String status) {
        if (logPos == null) {
            logPos = 0L;
        }
        List<FileLineVo> lineList = new ArrayList<>();
        FileTailerVo fileTailer = new FileTailerVo(logPos);
        File logFile = new File(logPath);
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
                    //如果向下读取 且状态为非终止，则获取剩下所有日志。 解决获取日志不全的问题
                    if (StringUtils.isNotBlank(status) && !Arrays.asList("pending", "running", "aborting", "pausing").contains(status)) {
                        fileTailer.setEndPos(-1L);
                    } else {
                        fileTailer.setEndPos(logPos + Config.LOGTAIL_BUFLEN());
                    }
                }
                fileTailer.setStartPos(logPos);
                fis.seek(logPos);
                String line;
                while ((fileTailer.getEndPos() == -1L || fis.getFilePointer() < fileTailer.getEndPos())
                        && (line = fis.readLine()) != null) {
                    line = new String(line.getBytes(StandardCharsets.ISO_8859_1), encoding);
                    fileTailer.setLastLine(line);
                    if (StringUtils.isNotBlank(line) && line.length() > 8) {
                        String time = line.substring(0, 8);
                        String content = StringUtils.EMPTY;
                        String lineType = StringUtils.EMPTY;
                        if (line.length() > 9) {
                            content = line.substring(9);
                            if (content.startsWith(FileLogType.ERROR.getValue())) {
                                lineType = FileLogType.ERROR.getValue();
                            } else if (content.startsWith(FileLogType.INFO.getValue())) {
                                lineType = FileLogType.INFO.getValue();
                            } else if (content.startsWith(FileLogType.WARN.getValue())) {
                                lineType = FileLogType.WARN.getValue();
                            } else if (content.startsWith(FileLogType.FINE.getValue())) {
                                lineType = FileLogType.FINE.getValue();
                            }
                        }
                        String anchor = null;
                        if (content.startsWith("------START--[")) {
                            StringBuilder anchorSb = new StringBuilder(StringUtils.EMPTY);
                            String contentTmp = content.substring(14);
                            char[] contentCharArray = contentTmp.toCharArray();
                            for (char contentChar : contentCharArray) {
                                if (Objects.equals(contentChar, ']')) {
                                    break;
                                }
                                anchorSb.append(contentChar);
                            }
                            if (StringUtils.isNotBlank(anchorSb.toString())) {
                                anchor = anchorSb.toString();
                            }
                        }
                        lineList.add(new FileLineVo(fis.getFilePointer(), time, lineType, content, anchor));
                    }
                }
                fileTailer.setLogPos(fis.getFilePointer());
                fileTailer.setEndPos(fis.getFilePointer());
            } catch (IOException ex) {
                logger.error("get tail log for file:" + logFile.getAbsolutePath() + "failed", ex);
            }
        } else {
            fileTailer.setLogPos(0L);
            fileTailer.setStartPos(0L);
        }
        fileTailer.setLineList(lineList);
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
     * 读取文件前{wordCountLimit}个字符的内容
     *
     * @param filePath       文件路径
     * @param wordCountLimit 字数限制
     * @return
     */
    public static String getFileContentWithLimit(String filePath, int wordCountLimit) {
        if (filePath == null || "".equals(filePath)) {
            return null;
        }
        String result = null;
        File file = new File(filePath);
        if (file.exists() && file.isFile()) {
            FileInputStream fis = null;
            InputStreamReader isr = null;
            try {
                fis = new FileInputStream(file);
                isr = new InputStreamReader(fis, StandardCharsets.UTF_8);
                int c;
                int index = 0;
                StringBuilder sb = new StringBuilder();
                while ((c = isr.read()) != -1 && index < wordCountLimit) {
                    sb.append((char) c);
                    index++;
                }
                result = sb.toString();
            } catch (Exception ex) {
                logger.error(ex.getMessage(), ex);
            } finally {
                if (isr != null) {
                    try {
                        isr.close();
                    } catch (IOException e) {
                        logger.error(e.getMessage(), e);
                    }
                }
                if (fis != null) {
                    try {
                        fis.close();
                    } catch (IOException e) {
                        logger.error(e.getMessage(), e);
                    }
                }
            }
        }
        return result;
    }

    public static FileTailerVo getSqlFileContent(String filePath, String encoding) {
        List<FileLineVo> lineList = new ArrayList<>();
        FileTailerVo fileTailer = new FileTailerVo();
        File logFile = new File(filePath);
        if (logFile.isFile()) {
            try (RandomAccessFile fis = new RandomAccessFile(logFile, "r")) {
                String line;
                while ((line = fis.readLine()) != null) {
                    line = new String(line.getBytes(StandardCharsets.ISO_8859_1), encoding);
                    FileLineVo lineVo = new FileLineVo();
                    lineVo.setContent(line);
                    lineList.add(lineVo);
                }
            } catch (IOException e) {
                logger.error("get sql file:" + logFile.getAbsolutePath() + "failed", e);
            }
        }
        fileTailer.setLineList(lineList);
        return fileTailer;
    }


    /**
     * 根据文件夹路径读取文件列表
     *
     * @param filepath 文件夹路径
     * @return 文件列表
     */
    public static List<FileVo> readFileList(String filepath) {
        List<FileVo> fileVoList = new ArrayList<>();
        try {
            File file = new File(filepath);
            String[] fileList = file.list();
            if (fileList == null || fileList.length == 0) {
                return fileVoList;
            }
            for (int i = 0; i < Objects.requireNonNull(fileList).length; i++) {
                File readFile = new File(filepath + System.getProperty("file.separator") + fileList[i]);
                FileVo f = new FileVo();
                f.setFileName(readFile.getName());
                f.setFilePath(readFile.getAbsolutePath());
                f.setLastModified(TimeUtil.getTimeToDateString(readFile.lastModified(), TimeUtil.YYYY_MM_DD_HH_MM_SS));
                f.setIsDirectory(0);
                if (readFile.isDirectory()) {
                    f.setIsDirectory(1);
                }
                fileVoList.add(f);
            }
        } catch (Exception e) {
            logger.error("show path : " + filepath + " file list Exception: " + e.getMessage(), e);
        }
        return fileVoList;
    }

    /**
     * 根据文件路径获取输入流
     *
     * @param path 文件路径
     * @throws Exception 异常
     */
    public static void downloadFileByPath(String path, HttpServletResponse response) throws Exception {
        InputStream in = null;
        File file = new File(path);
        if (file.exists() && file.isFile()) {
            in = new FileInputStream(file);
        }
        if (in != null) {
            OutputStream os = response.getOutputStream();
            IOUtils.copyLarge(in, os);
            if (os != null) {
                os.flush();
                os.close();
            }
            in.close();
        } else {
            throw new ExecuteJobFileNotFoundException(path);
        }
    }

    /**
     * 删除文件夹或文件
     *
     * @param path 文件路径
     */
    public static void deleteDirectoryOrFile(String path) {
        File dirFile = new File(path);
        if (!dirFile.isDirectory()) {
            dirFile.getAbsoluteFile().delete();
        } else {
            File[] files = dirFile.listFiles();
            for (int i = 0; i < files.length; i++) {
                if (files[i].isFile()) {
                    File file = new File(files[i].getAbsolutePath());
                    if (file.isFile()) {
                        file.delete();
                    }
                } else {
                    deleteDirectoryOrFile(files[i].getAbsolutePath());
                }
            }
            if (Objects.requireNonNull(dirFile.listFiles()).length == 0)
                dirFile.delete();
        }
    }

    /**
     * chrome、firefox、edge浏览器下载文件时，文件名包含~@#$&+=;这八个英文字符时会变成乱码_%40%23%24%26%2B%3D%3B，
     * 下面是对@#$&+=;这七个字符做特殊处理，
     * 对于~这个字符还是会变成下划线_，暂无法处理
     *
     * @param fileName 文件名
     * @return 返回处理后的文件名
     */
    public static String fileNameSpecialCharacterHandling(String fileName) {
        if (fileName.contains("%40")) {
            fileName = fileName.replace("%40", "@");
        }
        if (fileName.contains("%23")) {
            fileName = fileName.replace("%23", "#");
        }
        if (fileName.contains("%24")) {
            fileName = fileName.replace("%24", "$");
        }
        if (fileName.contains("%26")) {
            fileName = fileName.replace("%26", "&");
        }
        if (fileName.contains("%2B")) {
            fileName = fileName.replace("%2B", "+");
        }
        if (fileName.contains("%3D")) {
            fileName = fileName.replace("%3D", "=");
        }
        if (fileName.contains("%3B")) {
            fileName = fileName.replace("%3B", ";");
        }
        return fileName;
    }

    /**
     * Firefox浏览器userAgent：Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:88.0) Gecko/20100101 Firefox/88.0
     * Chrome浏览器userAgent：Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/90.0.4430.85 Safari/537.36
     * Edg浏览器userAgent：Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/90.0.4430.85 Safari/537.36 Edg/90.0.818.46
     *
     * @param userAgent
     * @param fileName
     * @return
     * @throws UnsupportedEncodingException
     */
    public static String getEncodedFileName(String userAgent, String fileName) throws UnsupportedEncodingException {
        if (userAgent.indexOf("Gecko") > 0) {
            //chrome、firefox、edge浏览器下载文件
            fileName = URLEncoder.encode(fileName, "UTF-8");
            fileName = fileNameSpecialCharacterHandling(fileName);
        } else {
            fileName = new String(fileName.replace(" ", "").getBytes(StandardCharsets.UTF_8), "ISO8859-1");
        }
        return fileName;
    }

    /**
     * 上传文件
     *
     * @param filePath    文件路径
     * @param inputStream
     * @return 保存后的文件绝对路径
     * @throws IOException
     */
    public static String uploadFile(String filePath, InputStream inputStream) throws IOException {
        File file = new File(getFullAbsolutePath(filePath));
        FileOutputStream fos = new FileOutputStream(file);
        IOUtils.copyLarge(inputStream, fos);
        fos.flush();
        fos.close();
        return file.getCanonicalPath();
    }

    /**
     * 解压zip文件
     *
     * @param zipFile 文件
     * @param charset 字符集
     * @throws Exception
     */
    public static void unzipFile(File zipFile, String charset) throws Exception {
        unzipFileToDestDirectory(zipFile, charset, null);
    }

    /**
     * 解压zip文件到指定目录
     *
     * @param zipFile   文件
     * @param charset   字符集
     * @param targetDir 目标目录
     * @throws Exception
     */
    public static void unzipFileToDestDirectory(File zipFile, String charset, String targetDir) throws Exception {
        try (ZipFile zip = new ZipFile(zipFile, Charset.forName(charset))) {//解决中文文件名乱码
            if (targetDir == null || targetDir.equals("")) {
                targetDir = zip.getName().substring(0, zip.getName().lastIndexOf(File.separatorChar));
            }
            for (Enumeration<? extends ZipEntry> entries = zip.entries(); entries.hasMoreElements(); ) {
                ZipEntry entry = entries.nextElement();
                String zipEntryName = entry.getName();
                String outPath = (targetDir + File.separator + zipEntryName).replaceAll("\\*", File.separator);
                // 判断路径是否存在,不存在则创建文件路径
                File file = new File(outPath.substring(0, outPath.lastIndexOf(File.separatorChar)));
                if (!file.exists()) {
                    file.mkdirs();
                }
                // 判断文件全路径是否为文件夹,如果是上面已经上传,不需要解压
                if (new File(outPath).isDirectory()) {
                    continue;
                }
                try (InputStream in = zip.getInputStream(entry); FileOutputStream out = new FileOutputStream(outPath)) {
                    byte[] buf = new byte[1024];
                    int len;
                    while ((len = in.read(buf)) > 0) {
                        out.write(buf, 0, len);
                    }
                }
            }
        }
    }

    /**
     * 压缩目录为zip文件
     *
     * @param rootDir 需要压缩的目录
     * @param out
     * @throws IOException
     */
    public static void zipDirectory(String rootDir, OutputStream out) throws IOException {
        Path rootPath = Paths.get(rootDir).normalize().toAbsolutePath();
        rootDir = rootPath.toString();
        int bufLen = BUF_SIZE;
        byte[] buf = new byte[bufLen];
        try {
            // ProcessBuilder builder = new ProcessBuilder("zip", "-qr", "-", ".");
            // .git/config里有密码信息，屏蔽掉
            ProcessBuilder builder = null;
            if (".git".equals(rootPath.getFileName().toString())) {
                builder = new ProcessBuilder("zip", "-qr", "-x", "config", "-", ".");
            } else {
                builder = new ProcessBuilder("zip", "-qr", "-x", "/.git/config", "-", ".");
            }

            builder.directory(new File(rootDir));
            Process proc = builder.start();

            InputStream in = proc.getInputStream();
            InputStream err = proc.getErrorStream();
            int len;
            while ((len = in.read(buf, 0, bufLen)) >= 0) {
                out.write(buf, 0, len);
            }
            proc.waitFor();
            int exitValue = proc.exitValue();
            if (exitValue != 0) {
                String errMsg = "";
                try {
                    len = err.read(buf, 0, bufLen);
                    errMsg = new String(buf, 0, bufLen);
                } catch (Exception ex) {
                }
                throw new IOException("zip dir failed:" + rootDir + ":" + errMsg);
            }
        } catch (InterruptedException e) {
            throw new IOException("zip dir failed:" + rootDir, e);
        } finally {

        }
    }

    /**
     * 补全文件路径
     *
     * @param path
     * @return
     */
    public static String getFullAbsolutePath(String path) {
        if (!path.startsWith(File.separator)) {
            path = File.separator + path;
        }
        path = Config.DATA_HOME() + path;
        return path;
    }
}
