package com.neatlogic.autoexecrunner.util;

import com.neatlogic.autoexecrunner.common.config.Config;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarConstants;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipFile;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.nio.file.attribute.PosixFilePermission;
import java.util.*;
import java.util.stream.Stream;

/**
 * 在隔离目录解压并完整验证后合并发布文件，拒绝归档中的越界路径和特殊条目。
 */
public final class SafeArchiveUtil {
    private SafeArchiveUtil() {
    }

    /**
     * 按原文件后缀选择 ZIP、TAR 或 TGZ；成功后由调用方处理原压缩包。
     */
    public static void unpack(File archive, File destination, String charset) throws IOException {
        Path target = destination.toPath().toAbsolutePath().normalize();
        Path staging = Files.createTempDirectory("runner-unpack-");
        List<EntryMetadata> metadata = new ArrayList<>();
        try {
            String name = archive.getName().toLowerCase(Locale.ROOT);
            if (name.endsWith(".tar") || name.endsWith(".tgz") || name.endsWith(".tar.gz")) {
                extractTar(archive, staging, name.endsWith(".tar"), metadata);
            } else {
                extractZip(archive, staging, charset, metadata);
            }
            // 全部条目和发布目标先验证，恶意归档不能覆盖已有文件或留下部分解压结果。
            File safetyRoot = destination;
            if (Config.DATA_HOME() != null && FileSafeUtil.isSubPath(new File(Config.DATA_HOME()), destination)) {
                safetyRoot = new File(Config.DATA_HOME());
            }
            List<Path> paths;
            try (Stream<Path> stream = Files.walk(staging)) {
                paths = stream.sorted().toList();
            }
            for (Path source : paths) {
                Path output = target.resolve(staging.relativize(source));
                validateDestination(target, safetyRoot, output);
                if (Files.exists(output) && Files.isDirectory(source) != Files.isDirectory(output)) {
                    throw new IOException("归档条目与已有文件类型冲突：" + output);
                }
            }
            for (Path source : paths) {
                Path output = target.resolve(staging.relativize(source));
                validateDestination(target, safetyRoot, output);
                if (Files.isDirectory(source)) {
                    Files.createDirectories(output);
                } else {
                    // 使用输出流保留目录内合法软链接的既有覆盖行为。
                    try (OutputStream out = Files.newOutputStream(output)) {
                        Files.copy(source, out);
                    }
                }
            }
            // 先恢复文件，再按目录深度倒序恢复权限；根目录最后处理，兼容父目录条目晚于子文件。
            Map<Path, EntryMetadata> latestMetadata = new LinkedHashMap<>();
            for (EntryMetadata entry : metadata) {
                latestMetadata.put(entry.name, entry);
            }
            List<EntryMetadata> orderedMetadata = new ArrayList<>(latestMetadata.values());
            orderedMetadata.sort(Comparator.comparing((EntryMetadata entry) -> entry.directory)
                    .thenComparing(SafeArchiveUtil::entryDepth, Comparator.reverseOrder()));
            for (EntryMetadata entry : orderedMetadata) {
                Path output = target.resolve(entry.name);
                validateDestination(target, safetyRoot, output);
                restoreMetadata(output, entry);
            }
        } finally {
            deleteStaging(staging);
        }
    }

    /**
     * ZIP 仅允许普通文件及目录，保留中文编码和 Unicode 文件名兼容能力。
     */
    private static void extractZip(File archive, Path staging, String charset, List<EntryMetadata> metadata) throws IOException {
        try (ZipFile zip = ZipFile.builder().setFile(archive).setCharset(charset).get()) {
            Enumeration<ZipArchiveEntry> entries = zip.getEntries();
            while (entries.hasMoreElements()) {
                ZipArchiveEntry entry = entries.nextElement();
                int type = entry.getUnixMode() & 0170000;
                if (entry.isUnixSymlink() || (type != 0 && type != 0100000 && type != 0040000)
                        || !zip.canReadEntryData(entry)) {
                    throw new IOException("归档包含不支持的特殊条目：" + entry.getName());
                }
                Path output = entryPath(staging, entry.getName());
                if (entry.isDirectory()) {
                    Files.createDirectories(output);
                } else {
                    Files.createDirectories(output.getParent());
                    try (InputStream in = zip.getInputStream(entry); OutputStream out = Files.newOutputStream(output)) {
                        in.transferTo(out);
                    }
                }
                int mode = -1;
                if (entry.getUnixMode() != 0) {
                    mode = entry.getUnixMode();
                }
                metadata.add(new EntryMetadata(staging.relativize(output), mode, entry.getLastModifiedTime(), entry.isDirectory()));
            }
        }
    }

    /**
     * TAR/TGZ 共用逐条目解压，拒绝软硬链接、设备和其他非普通文件条目。
     */
    private static void extractTar(File archive, Path staging, boolean plainTar, List<EntryMetadata> metadata) throws IOException {
        try (InputStream raw = Files.newInputStream(archive.toPath());
             InputStream decoded = tarInput(raw, plainTar);
             TarArchiveInputStream tar = new TarArchiveInputStream(decoded)) {
            TarArchiveEntry entry;
            while ((entry = tar.getNextEntry()) != null) {
                byte type = entry.getLinkFlag();
                boolean regular = type == TarConstants.LF_NORMAL || type == TarConstants.LF_OLDNORM
                        || type == TarConstants.LF_CONTIG || type == TarConstants.LF_GNUTYPE_SPARSE;
                // isDirectory() 会把末尾为 / 的特殊条目也当作目录，必须按真实类型白名单判断。
                if ((type != TarConstants.LF_DIR && !regular) || !tar.canReadEntryData(entry)) {
                    throw new IOException("归档包含不支持的特殊条目：" + entry.getName());
                }
                Path output = entryPath(staging, entry.getName());
                if (entry.isDirectory()) {
                    Files.createDirectories(output);
                } else {
                    Files.createDirectories(output.getParent());
                    try (OutputStream out = Files.newOutputStream(output)) {
                        tar.transferTo(out);
                    }
                }
                metadata.add(new EntryMetadata(staging.relativize(output), entry.getMode(), entry.getLastModifiedTime(), entry.isDirectory()));
            }
        }
    }

    /**
     * 根据 TAR 压缩方式包装输入流。
     */
    private static InputStream tarInput(InputStream input, boolean plainTar) throws IOException {
        if (plainTar) {
            return input;
        }
        return new GzipCompressorInputStream(input);
    }

    /**
     * 归档路径只按文件路径解释，不作 URL 解码，拒绝绝对路径和向上穿越。
     */
    private static Path entryPath(Path root, String name) throws IOException {
        String normalizedName = name.replace('\\', '/');
        if (normalizedName.startsWith("/") || normalizedName.matches("^[A-Za-z]:.*") || normalizedName.indexOf('\0') >= 0) {
            throw new IOException("归档条目路径不合法：" + name);
        }
        Path relative = Path.of(normalizedName);
        for (Path part : relative) {
            if ("..".equals(part.toString())) {
                throw new IOException("归档条目路径不合法：" + name);
            }
        }
        Path result = root.resolve(relative).normalize();
        if (!result.startsWith(root)) {
            throw new IOException("归档条目越过目标目录：" + name);
        }
        return result;
    }

    /**
     * 同时约束解压目录和业务根目录，检查已有目标或父目录软链接与禁止路径。
     */
    private static void validateDestination(Path target, File safetyRoot, Path output) throws IOException {
        FileSafeUtil.validateFilePathAllowed(target.toFile(), output.toFile());
        FileSafeUtil.validateFilePathAllowed(safetyRoot, output.toFile());
    }

    /**
     * 空相对路径表示发布根目录，排序时必须放在所有子目录之后。
     */
    private static int entryDepth(EntryMetadata entry) {
        if (entry.name.toString().isEmpty()) {
            return 0;
        }
        return entry.name.getNameCount();
    }

    /**
     * 沿用归档中的执行权限与修改时间，非 POSIX 文件系统只恢复修改时间。
     */
    private static void restoreMetadata(Path output, EntryMetadata entry) throws IOException {
        Set<PosixFilePermission> permissions = EnumSet.noneOf(PosixFilePermission.class);
        PosixFilePermission[] values = PosixFilePermission.values();
        for (int i = 0; i < values.length; i++) {
            if ((entry.mode & (1 << (8 - i))) != 0) {
                permissions.add(values[i]);
            }
        }
        // 修改时间先恢复，部分系统在目录设为 0000 后会拒绝更新其时间。
        if (entry.modified != null) {
            Files.setLastModifiedTime(output, entry.modified);
        }
        if (entry.mode >= 0) {
            try {
                Files.setPosixFilePermissions(output, permissions);
            } catch (UnsupportedOperationException ignored) {
                // 非 POSIX 平台不设置 Unix 权限。
            }
        }
    }

    /**
     * 不跟随软链接清理临时目录，失败时也不遗留解压内容。
     */
    private static void deleteStaging(Path staging) throws IOException {
        try (Stream<Path> stream = Files.walk(staging)) {
            for (Path path : stream.sorted(Comparator.reverseOrder()).toList()) {
                Files.deleteIfExists(path);
            }
        }
    }

    private static final class EntryMetadata {
        private final Path name;
        private final int mode;
        private final FileTime modified;
        private final boolean directory;

        /**
         * 保存 TAR 条目的发布后属性。
         */
        private EntryMetadata(Path name, int mode, FileTime modified, boolean directory) {
            this.name = name;
            this.mode = mode;
            this.modified = modified;
            this.directory = directory;
        }
    }
}
