package com.neatlogic.autoexecrunner.util;

import com.neatlogic.autoexecrunner.common.config.Config;
import com.neatlogic.autoexecrunner.exception.core.ApiRuntimeException;
import com.neatlogic.autoexecrunner.exception.file.FileNotFoundException;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
        File root = getNormalizedAbsoluteFile(rootPath);
        File directory = StringUtils.isBlank(relativePath) ? root : resolveNormalizedFile(root, relativePath);
        File file = resolveNormalizedFile(directory, fileName);
        validateLexicalSubPath(root, directory);
        validateLexicalSubPath(root, file);
        validateFilePathAllowed(root, directory);
        validateFilePathAllowed(root, file);
        return file;
    }

    public static File getChildPath(String rootPath, String childPath) throws IOException {
        File root = getNormalizedAbsoluteFile(rootPath);
        File file = resolveNormalizedFile(root, StringUtils.defaultString(childPath));
        validateLexicalSubPath(root, file);
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
        File root = getNormalizedAbsoluteFile(rootPath);
        File file = resolveNormalizedFile(root, decode(path));
        // 先按词法路径清理../../，软链接真实目标再由允许根目录单独控制。
        validateLexicalSubPath(root, file);
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
        File root = getNormalizedAbsoluteFile(rootPath);
        String normalizedChildPath = decode(StringUtils.defaultIfBlank(childPath, ".")).replace("\\", "/");
        if (".".equals(normalizedChildPath)) {
            return ".";
        }
        File child = new File(normalizedChildPath);
        if (child.isAbsolute()) {
            throw new ApiRuntimeException("文件路径不合法");
        }
        File file = resolveNormalizedFile(root, normalizedChildPath);
        // tar子目录参数必须保持为rootPath内的相对路径，避免绝对路径或../把打包范围带出制品目录。
        validateLexicalSubPath(root, file);
        validateFilePathAllowed(root, file);
        String relativePath = root.toPath().relativize(file.toPath()).toString();
        return StringUtils.isBlank(relativePath) ? "." : relativePath.replace(File.separatorChar, '/');
    }

    public static File getValidatedChildFile(File directory, String fileName, String rootPath) throws IOException {
        File root = getNormalizedAbsoluteFile(rootPath);
        File normalizedDirectory = getNormalizedAbsoluteFile(directory.getPath());
        File file = resolveNormalizedFile(normalizedDirectory, fileName);
        if (!isSubPath(root, normalizedDirectory)
                || !isSubPath(root, file)
                || file.getParentFile() == null
                || !normalizedDirectory.toPath().equals(file.getParentFile().toPath())) {
            throw new ApiRuntimeException("文件路径不合法");
        }
        validateFilePathAllowed(root, normalizedDirectory);
        validateFilePathAllowed(root, file);
        return file;
    }

    /**
     * 递归校验目录内文件是否落入禁止访问路径，避免打包下载绕过单文件路径限制。
     */
    public static void validateNoForbiddenPathInDirectory(File root, File directory) throws IOException {
        validateDirectory(root, directory, new HashSet<>());
    }

    /**
     * 按真实目录跟踪当前递归链，允许目录内软链接，但拒绝循环和越界目标。
     */
    private static void validateDirectory(File root, File file, Set<Path> ancestors) throws IOException {
        if (file == null) {
            return;
        }
        validateFilePathAllowed(root, file);
        if (!file.isDirectory()) {
            return;
        }
        Path realPath = resolveRealPath(file.toPath());
        if (!ancestors.add(realPath)) {
            throw new IOException("目录软链接形成循环");
        }
        try {
            File[] children = file.listFiles();
            if (children == null) {
                throw new IOException("无法读取目录：" + file);
            }
            for (File child : children) {
                validateDirectory(root, child, ancestors);
            }
        } finally {
            ancestors.remove(realPath);
        }
    }

    /**
     * 同时校验业务访问路径和真实目标；不存在的写入目标通过最近已存在的父目录解析。
     */
    public static void validateFilePathAllowed(File root, File file) throws IOException {
        if (root == null || file == null) {
            return;
        }
        File normalizedRoot = getNormalizedAbsoluteFile(root.getPath());
        File normalizedFile = getNormalizedAbsoluteFile(file.getPath());
        validateLexicalSubPath(normalizedRoot, normalizedFile);
        Path realRoot = resolveRealPath(normalizedRoot.toPath());
        Path realFile = resolveRealPath(normalizedFile.toPath());
        if (!realFile.startsWith(realRoot)) {
            throw new ApiRuntimeException("文件真实路径不允许访问");
        }
        for (File forbiddenPath : getForbiddenPathList(normalizedRoot)) {
            if (isSubPath(forbiddenPath, normalizedFile)
                    || realFile.startsWith(resolveRealPath(forbiddenPath.toPath()))) {
                throw new ApiRuntimeException("文件路径不允许访问");
            }
        }
    }

    /**
     * 保留尚未创建的路径后缀，避免新文件通过父目录软链接写到业务根目录之外。
     */
    private static Path resolveRealPath(Path path) throws IOException {
        Path absolute = path.toAbsolutePath().normalize();
        Path existing = absolute;
        while (!Files.exists(existing, LinkOption.NOFOLLOW_LINKS)) {
            existing = existing.getParent();
            if (existing == null) {
                throw new IOException("文件路径无法解析");
            }
        }
        return existing.toRealPath().resolve(existing.relativize(absolute)).normalize();
    }

    /**
     * 从runner配置读取禁止访问的本地路径，支持相对业务根目录或绝对路径，默认空列表保持兼容。
     */
    private static List<File> getForbiddenPathList(File root) throws IOException {
        String pathConfig = Config.getConfigProperty(FORBIDDEN_PATH_CONFIG_KEY, StringUtils.EMPTY);
        List<File> pathList = new ArrayList<>();
        addConfiguredPathList(pathList, root, pathConfig);
        return pathList;
    }

    private static void addConfiguredPathList(List<File> pathList, File root, String pathConfig) throws IOException {
        if (StringUtils.isBlank(pathConfig)) {
            return;
        }
        String[] pathArray = pathConfig.split(",");
        for (String path : pathArray) {
            String normalizedPath = StringUtils.trimToEmpty(path);
            if (StringUtils.isBlank(normalizedPath)) {
                continue;
            }
            File configuredPath = new File(normalizedPath);
            if (!configuredPath.isAbsolute()) {
                configuredPath = new File(root, normalizedPath);
            }
            pathList.add(getNormalizedAbsoluteFile(configuredPath.getPath()));
        }
    }

    /**
     * 仅规范化绝对路径中的.和..，不解析软链接，保留业务访问路径。
     */
    private static File getNormalizedAbsoluteFile(String path) {
        return new File(path).toPath().toAbsolutePath().normalize().toFile();
    }

    private static File resolveNormalizedFile(File root, String childPath) {
        Path child = new File(StringUtils.defaultString(childPath)).toPath();
        Path resolvedPath = child.isAbsolute() ? child : root.toPath().resolve(child);
        return resolvedPath.toAbsolutePath().normalize().toFile();
    }

    private static void validateLexicalSubPath(File root, File file) {
        if (!isSubPath(root, file)) {
            throw new ApiRuntimeException("文件路径不合法");
        }
    }

    public static boolean isSubPath(File root, File file){
        Path rootPath = root.toPath().toAbsolutePath().normalize();
        Path filePath = file.toPath().toAbsolutePath().normalize();
        return filePath.startsWith(rootPath);
    }

    public static String decode(String path) throws IOException {
        String decoded = StringUtils.defaultString(path);
        for (int i = 0; i < 3; i++) {
            String next = URLDecoder.decode(decoded, StandardCharsets.UTF_8);
            if (next.equals(decoded)) {
                break;
            }
            decoded = next;
        }
        return decoded;
    }
}
