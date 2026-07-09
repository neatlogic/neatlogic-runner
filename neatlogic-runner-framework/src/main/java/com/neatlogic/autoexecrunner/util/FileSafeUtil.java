package com.neatlogic.autoexecrunner.util;

import com.neatlogic.autoexecrunner.common.config.Config;
import com.neatlogic.autoexecrunner.exception.core.ApiRuntimeException;
import com.neatlogic.autoexecrunner.exception.file.FileNotFoundException;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public final class FileSafeUtil {
    private static final String FORBIDDEN_PATH_CONFIG_KEY = "file.safe.forbidden.path";

    private FileSafeUtil() {
    }

    public static String getUploadFileName(String fileName) {
        if (StringUtils.isBlank(fileName)) {
            throw new ApiRuntimeException("上传文件名不能为空");
        }
        String normalizedFileName = fileName.replace("\\", "/");
        return new File(normalizedFileName).getName();
    }

    public static File getChildFile(String rootPath, String relativePath, String fileName) throws IOException {
        File root = new File(rootPath).getCanonicalFile();
        File directory = StringUtils.isBlank(relativePath) ? root : new File(root, relativePath).getCanonicalFile();
        File file = new File(directory, fileName).getCanonicalFile();
        if (!isSubPath(root, directory) || !isSubPath(root, file)) {
            throw new ApiRuntimeException("文件路径不合法");
        }
        validateFilePathAllowed(root, directory);
        validateFilePathAllowed(root, file);
        return file;
    }

    public static File getChildPath(String rootPath, String childPath) throws IOException {
        File root = new File(rootPath).getCanonicalFile();
        File file = new File(root, StringUtils.defaultString(childPath)).getCanonicalFile();
        if (!isSubPath(root, file)) {
            throw new ApiRuntimeException("文件路径不合法");
        }
        validateFilePathAllowed(root, file);
        return file;
    }

    public static File getDownloadFile(String path, String rootPath) throws IOException {
        File file = getPath(path, rootPath);
        if (!file.exists() || !file.isFile()) {
            throw new FileNotFoundException(path);
        }
        return file;
    }

    public static File getPath(String path, String rootPath) throws IOException {
        if (StringUtils.isBlank(path)) {
            throw new ApiRuntimeException("文件路径不能为空");
        }
        File root = new File(rootPath).getCanonicalFile();
        File file = new File(decode(path));
        if (!file.isAbsolute()) {
            file = new File(root, file.getPath());
        }
        file = file.getCanonicalFile();
        // runner文件接口只允许访问DATA_HOME内的真实路径，避免../../和软链接逃逸到宿主机其他目录。
        if (!isSubPath(root, file)) {
            throw new ApiRuntimeException("文件路径不合法");
        }
        validateFilePathAllowed(root, file);
        return file;
    }

    public static File getValidatedDirectory(String path, String rootPath) throws IOException {
        File directory = getPath(path, rootPath);
        if (!directory.exists() || !directory.isDirectory()) {
            throw new FileNotFoundException(path);
        }
        return directory;
    }

    public static File getValidatedPath(String path, String rootPath) throws IOException {
        File file = getPath(path, rootPath);
        if (!file.exists()) {
            throw new FileNotFoundException(path);
        }
        return file;
    }

    public static String getValidatedRelativePath(String rootPath, String childPath) throws IOException {
        File root = new File(rootPath).getCanonicalFile();
        String normalizedChildPath = decode(StringUtils.defaultIfBlank(childPath, ".")).replace("\\", "/");
        if (".".equals(normalizedChildPath)) {
            return ".";
        }
        File child = new File(normalizedChildPath);
        if (child.isAbsolute()) {
            throw new ApiRuntimeException("文件路径不合法");
        }
        File file = new File(root, normalizedChildPath).getCanonicalFile();
        // tar子目录参数必须保持为rootPath内的相对路径，避免绝对路径或../把打包范围带出制品目录。
        if (!isSubPath(root, file)) {
            throw new ApiRuntimeException("文件路径不合法");
        }
        validateFilePathAllowed(root, file);
        String relativePath = root.toPath().relativize(file.toPath()).toString();
        return StringUtils.isBlank(relativePath) ? "." : relativePath.replace(File.separatorChar, '/');
    }

    public static File getValidatedChildFile(File directory, String fileName, String rootPath) throws IOException {
        File root = new File(rootPath).getCanonicalFile();
        File canonicalDirectory = directory.getCanonicalFile();
        File file = new File(canonicalDirectory, fileName).getCanonicalFile();
        if (!isSubPath(root, canonicalDirectory)
                || !isSubPath(root, file)
                || !canonicalDirectory.getCanonicalPath().equals(file.getParentFile().getCanonicalPath())) {
            throw new ApiRuntimeException("文件路径不合法");
        }
        validateFilePathAllowed(root, canonicalDirectory);
        validateFilePathAllowed(root, file);
        return file;
    }

    /**
     * 递归校验目录内文件是否落入禁止访问路径，避免打包下载绕过单文件路径限制。
     */
    public static void validateNoForbiddenPathInDirectory(File root, File directory) throws IOException {
        if (directory == null) {
            return;
        }
        File canonicalRoot = root.getCanonicalFile();
        File canonicalFile = directory.getCanonicalFile();
        if (!isSubPath(canonicalRoot, canonicalFile)) {
            throw new ApiRuntimeException("文件路径不合法");
        }
        validateFilePathAllowed(canonicalRoot, canonicalFile);
        if (!canonicalFile.isDirectory()) {
            return;
        }
        File[] childList = canonicalFile.listFiles();
        if (childList == null) {
            return;
        }
        for (File child : childList) {
            validateNoForbiddenPathInDirectory(canonicalRoot, child);
        }
    }

    /**
     * 校验本地文件是否落入file.safe.forbidden.path配置的禁止访问路径。
     */
    public static void validateFilePathAllowed(File root, File file) throws IOException {
        if (root == null || file == null) {
            return;
        }
        File canonicalRoot = root.getCanonicalFile();
        File canonicalFile = file.getCanonicalFile();
        for (File forbiddenPath : getForbiddenPathList(canonicalRoot)) {
            if (isSubPath(forbiddenPath, canonicalFile)) {
                throw new ApiRuntimeException("文件路径不允许访问");
            }
        }
    }

    /**
     * 从runner配置读取禁止访问的本地路径，支持相对业务根目录或绝对路径，默认空列表保持兼容。
     */
    private static List<File> getForbiddenPathList(File root) throws IOException {
        String pathConfig = Config.getConfigProperty(FORBIDDEN_PATH_CONFIG_KEY, StringUtils.EMPTY);
        List<File> pathList = new ArrayList<>();
        if (StringUtils.isBlank(pathConfig)) {
            return pathList;
        }
        String[] pathArray = pathConfig.split(",");
        for (String path : pathArray) {
            String normalizedPath = StringUtils.trimToEmpty(path);
            if (StringUtils.isBlank(normalizedPath)) {
                continue;
            }
            File forbiddenPath = new File(normalizedPath);
            if (!forbiddenPath.isAbsolute()) {
                forbiddenPath = new File(root, normalizedPath);
            }
            pathList.add(forbiddenPath.getCanonicalFile());
        }
        return pathList;
    }

    public static boolean isSubPath(File root, File file) throws IOException {
        String rootPath = root.getCanonicalPath();
        String filePath = file.getCanonicalPath();
        return filePath.equals(rootPath) || filePath.startsWith(rootPath + File.separator);
    }

    public static String decode(String path) throws IOException {
        String decoded = StringUtils.defaultString(path);
        for (int i = 0; i < 3; i++) {
            String next = URLDecoder.decode(decoded, StandardCharsets.UTF_8.name());
            if (next.equals(decoded)) {
                break;
            }
            decoded = next;
        }
        return decoded;
    }
}
