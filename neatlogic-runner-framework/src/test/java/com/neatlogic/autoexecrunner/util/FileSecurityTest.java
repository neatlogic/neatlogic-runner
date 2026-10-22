package com.neatlogic.autoexecrunner.util;

import com.alibaba.fastjson.JSONObject;
import com.neatlogic.autoexecrunner.api.file.FileUploadApi;
import com.neatlogic.autoexecrunner.common.config.Config;
import com.neatlogic.autoexecrunner.exception.core.ApiRuntimeException;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.archivers.tar.TarConstants;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.mock.web.MockMultipartHttpServletRequest;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipInputStream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 上传、打包及解压使用隔离目录验证边界，并覆盖正常制品兼容行为。
 */
class FileSecurityTest {
    @TempDir
    Path temp;
    private Path root;
    private String originalHome;
    private String originalForbidden;

    /**
     * 将文件操作根目录隔离到测试临时目录。
     */
    @BeforeEach
    void setUp() throws Exception {
        root = Files.createDirectory(temp.resolve("data"));
        Field field = Config.class.getDeclaredField("DATA_HOME");
        field.setAccessible(true);
        originalHome = (String) field.get(null);
        field.set(null, root.toString());
        originalForbidden = Config.properties.getProperty("file.safe.forbidden.path");
        Config.properties.remove("file.safe.forbidden.path");
    }

    /**
     * 恢复全局配置，避免影响其他 Runner 测试。
     */
    @AfterEach
    void tearDown() throws Exception {
        Field field = Config.class.getDeclaredField("DATA_HOME");
        field.setAccessible(true);
        field.set(null, originalHome);
        if (originalForbidden == null) {
            Config.properties.remove("file.safe.forbidden.path");
        } else {
            Config.properties.setProperty("file.safe.forbidden.path", originalForbidden);
        }
    }

    /**
     * 上传保留相对路径及虚拟根路径，也保留文件名中的加号和百分号。
     */
    @Test
    void uploadNormalPathsAndRejectEscapes() throws Exception {
        assertEquals(root.resolve("a.txt").toFile().getCanonicalPath(), FileUtil.uploadFile("a.txt", input("a")));
        FileUtil.uploadFile("/plus+100%.txt", input("b"));
        assertEquals("b", Files.readString(root.resolve("plus+100%.txt")));
        assertThrows(ApiRuntimeException.class, () -> FileUtil.uploadFile("../escape.txt", input("bad")));
        assertFalse(Files.exists(temp.resolve("escape.txt")));
        Files.createSymbolicLink(root.resolve("outside"), temp);
        assertThrows(ApiRuntimeException.class, () -> FileUtil.uploadFile("outside/escape.txt", input("bad")));
        Path real = Files.createDirectory(root.resolve("real"));
        Files.createSymbolicLink(root.resolve("inside"), real);
        FileUtil.uploadFile("inside/safe.txt", input("safe"));
        assertEquals("safe", Files.readString(real.resolve("safe.txt")));
    }

    /**
     * 下载拒绝多重编码、同名前缀、软链接越界及禁止目录别名。
     */
    @Test
    void downloadValidatesRealPathsAndForbiddenAliases() throws Exception {
        Path file = Files.writeString(root.resolve("normal.txt"), "ok");
        assertEquals(file.toFile(), FileSafeUtil.getDownloadFile(file.toString(), root.toString()));
        assertThrows(ApiRuntimeException.class, () -> FileSafeUtil.getPath("%252e%252e/escape", root.toString()));
        assertThrows(ApiRuntimeException.class, () -> FileSafeUtil.getPath(temp.resolve("data-other/a").toString(), root.toString()));
        Files.createSymbolicLink(root.resolve("outside"), temp);
        assertThrows(ApiRuntimeException.class, () -> FileSafeUtil.getPath("outside/new/file", root.toString()));
        Path secret = Files.createDirectory(root.resolve("secret"));
        Path secretFile = Files.writeString(secret.resolve("s.txt"), "secret");
        Files.createSymbolicLink(root.resolve("alias"), secret);
        Config.properties.setProperty("file.safe.forbidden.path", "secret");
        assertThrows(ApiRuntimeException.class, () -> FileSafeUtil.getDownloadFile("alias/s.txt", root.toString()));
        Config.properties.setProperty("file.safe.forbidden.path", "alias");
        assertThrows(ApiRuntimeException.class, () -> FileSafeUtil.getDownloadFile(secretFile.toString(), root.toString()));
    }

    /**
     * ZIP 保留中文、隐藏文件、空目录以及目录内合法链接，拒绝循环和越界链接。
     */
    @Test
    void zipNormalDirectoryAndSafeLinks() throws Exception {
        Files.writeString(root.resolve("中文.txt"), "normal");
        Files.setPosixFilePermissions(root.resolve("中文.txt"), java.nio.file.attribute.PosixFilePermissions.fromString("rwxr-xr-x"));
        Files.writeString(root.resolve(".hidden"), "hidden");
        Files.createDirectory(root.resolve("empty"));
        Files.createSymbolicLink(root.resolve("link.txt"), root.resolve("中文.txt"));
        Files.createDirectory(root.resolve(".git"));
        Files.writeString(root.resolve(".git/config"), "password");
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        FileUtil.zipDirectory(root.toString(), out);
        List<String> names = new ArrayList<>();
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(out.toByteArray()))) {
            java.util.zip.ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                names.add(entry.getName());
            }
        }
        assertTrue(names.containsAll(List.of("中文.txt", ".hidden", "empty/", "link.txt")));
        assertFalse(names.contains(".git/config"));
        Path packed = Files.write(temp.resolve("packed.zip"), out.toByteArray());
        Path unpacked = Files.createDirectory(temp.resolve("unpacked"));
        SafeArchiveUtil.unpack(packed.toFile(), unpacked.toFile(), "GBK");
        assertTrue(Files.getPosixFilePermissions(unpacked.resolve("中文.txt")).contains(PosixFilePermission.OWNER_EXECUTE));
        Files.createSymbolicLink(root.resolve("loop"), root);
        assertThrows(IOException.class, () -> FileUtil.zipDirectory(root.toString(), new ByteArrayOutputStream()));
        Files.delete(root.resolve("loop"));
        Files.createSymbolicLink(root.resolve("escape"), temp);
        assertThrows(ApiRuntimeException.class, () -> FileUtil.zipDirectory(root.toString(), new ByteArrayOutputStream()));
    }

    /**
     * ZIP、TAR、TGZ 都可合并覆盖，保留未涉及文件、空目录和脚本执行权限。
     */
    @Test
    void archivesMergeWithoutDeletingExistingFiles() throws Exception {
        for (String format : List.of("zip", "tar", "tgz")) {
            Path destination = Files.createDirectory(root.resolve(format));
            Files.writeString(destination.resolve("keep.txt"), "keep");
            Files.writeString(destination.resolve("script.sh"), "old");
            Path archive = archive(format, "script.sh", false, false);
            SafeArchiveUtil.unpack(archive.toFile(), destination.toFile(), "GBK");
            assertEquals("content", Files.readString(destination.resolve("script.sh")));
            assertEquals("keep", Files.readString(destination.resolve("keep.txt")));
            assertTrue(Files.isDirectory(destination.resolve("empty")));
            assertTrue(Files.getPosixFilePermissions(destination.resolve("script.sh")).contains(PosixFilePermission.OWNER_EXECUTE));
        }
    }

    /**
     * 恶意条目出现在普通条目之后时，已有文件仍不能被部分覆盖。
     */
    @Test
    void archivesRejectTraversalAndLinksBeforePublishing() throws Exception {
        for (String format : List.of("zip", "tar", "tgz")) {
            for (String unsafe : List.of("../escape.txt", "a/../../escape.txt", "/absolute.txt", "C:/escape.txt")) {
                Path destination = Files.createTempDirectory(root, "dest");
                Files.writeString(destination.resolve("first.txt"), "original");
                Path archive = archive(format, unsafe, false, true);
                assertThrows(IOException.class, () -> SafeArchiveUtil.unpack(archive.toFile(), destination.toFile(), "GBK"));
                assertEquals("original", Files.readString(destination.resolve("first.txt")));
                assertEquals(1, countFiles(destination));
            }
            Path destination = Files.createTempDirectory(root, "dest");
            Path linked = archive(format, "link", true, true);
            assertThrows(IOException.class, () -> SafeArchiveUtil.unpack(linked.toFile(), destination.toFile(), "GBK"));
            assertEquals(0, countFiles(destination));
        }
    }

    /**
     * 已有越界链接及禁止目录在发布前拒绝，合法目录内链接继续按原方式覆盖。
     */
    @Test
    void archiveValidatesAllDestinationsBeforePublishing() throws Exception {
        Path destination = Files.createDirectory(root.resolve("dest"));
        Path archive = archive("zip", "nested/file", false, true);
        Files.createSymbolicLink(destination.resolve("nested"), temp);
        assertThrows(ApiRuntimeException.class, () -> SafeArchiveUtil.unpack(archive.toFile(), destination.toFile(), "GBK"));
        assertFalse(Files.exists(destination.resolve("first.txt")));
        Files.delete(destination.resolve("nested"));
        Path real = Files.createDirectory(destination.resolve("real"));
        Files.createSymbolicLink(destination.resolve("nested"), real);
        SafeArchiveUtil.unpack(archive.toFile(), destination.toFile(), "GBK");
        assertEquals("content", Files.readString(real.resolve("file")));
        Config.properties.setProperty("file.safe.forbidden.path", "dest/real");
        Files.writeString(destination.resolve("first.txt"), "original");
        assertThrows(ApiRuntimeException.class, () -> SafeArchiveUtil.unpack(archive.toFile(), destination.toFile(), "GBK"));
        assertEquals("original", Files.readString(destination.resolve("first.txt")));
    }

    /**
     * TAR 的硬链接和设备条目、ZIP 的设备类型都不能在目标目录发布。
     */
    @Test
    void rejectHardLinksAndDevices() throws Exception {
        for (byte type : new byte[]{TarConstants.LF_LINK, TarConstants.LF_CHR, TarConstants.LF_BLK, TarConstants.LF_FIFO}) {
            for (String entryName : List.of("special", "special/")) {
                Path archive = Files.createTempFile(root, "special", ".tar");
                try (TarArchiveOutputStream out = new TarArchiveOutputStream(Files.newOutputStream(archive))) {
                    tarEntry(out, "first.txt", false);
                    TarArchiveEntry special = new TarArchiveEntry(entryName, type);
                    special.setLinkName("first.txt");
                    out.putArchiveEntry(special);
                    out.closeArchiveEntry();
                }
                Path destination = Files.createTempDirectory(root, "dest");
                assertThrows(IOException.class, () -> SafeArchiveUtil.unpack(archive.toFile(), destination.toFile(), "GBK"));
                assertEquals(0, countFiles(destination));
            }
        }
        Path archive = Files.createTempFile(root, "device", ".zip");
        try (ZipArchiveOutputStream out = new ZipArchiveOutputStream(archive)) {
            ZipArchiveEntry device = new ZipArchiveEntry("device");
            device.setUnixMode(0020666);
            out.putArchiveEntry(device);
            out.closeArchiveEntry();
        }
        Path destination = Files.createTempDirectory(root, "dest");
        assertThrows(IOException.class, () -> SafeArchiveUtil.unpack(archive.toFile(), destination.toFile(), "GBK"));
    }

    /**
     * 保留不携带 UTF-8 标记的历史 GBK 中文文件名。
     */
    @Test
    void legacyGbkZipNamesRemainReadable() throws Exception {
        Path archive = Files.createTempFile(root, "gbk", ".zip");
        try (ZipArchiveOutputStream out = new ZipArchiveOutputStream(archive)) {
            out.setEncoding("GBK");
            out.setUseLanguageEncodingFlag(false);
            zipEntry(out, "中文.txt", false);
        }
        Path destination = Files.createDirectory(root.resolve("gbk"));
        FileUtil.unzipFileToDestDirectory(archive.toFile(), "GBK", destination.toString());
        assertEquals("content", Files.readString(destination.resolve("中文.txt")));
    }

    /**
     * TAR 根目录为软链接或父路径别名时，仍按原工作目录语义输出完整内容。
     */
    @Test
    void tarPackingRootAliasesKeepDirectoryContents() throws Exception {
        Path artifact = Files.createDirectory(root.resolve("artifact"));
        Files.writeString(artifact.resolve("top.txt"), "top");
        Path sub = Files.createDirectory(artifact.resolve("sub"));
        Files.writeString(sub.resolve("child.txt"), "child");
        Files.createDirectory(artifact.resolve("empty"));
        Files.createSymbolicLink(artifact.resolve("inner-link"), sub);
        Path alias = Files.createSymbolicLink(root.resolve("alias"), artifact);
        MockHttpServletResponse all = new MockHttpServletResponse();
        FolderZipperUtil.tgzFolderToResponse(alias.toString(), alias.toString(), List.of("."), all.getOutputStream());
        List<String> allNames = tarNames(all.getContentAsByteArray());
        assertTrue(allNames.containsAll(List.of("top.txt", "sub/child.txt", "empty/", "inner-link")));
        MockHttpServletResponse selection = new MockHttpServletResponse();
        FolderZipperUtil.tgzFolderToResponse(alias.toString(), alias.toString(), List.of("sub"), selection.getOutputStream());
        List<String> selectedNames = tarNames(selection.getContentAsByteArray());
        assertTrue(selectedNames.contains("sub/child.txt"));
        assertFalse(selectedNames.contains("top.txt"));
        MockHttpServletResponse linkedSelection = new MockHttpServletResponse();
        FolderZipperUtil.tgzFolderToResponse(alias.toString(), alias.toString(), List.of("inner-link"), linkedSelection.getOutputStream());
        assertEquals(List.of("inner-link"), tarNames(linkedSelection.getContentAsByteArray()));
    }

    /**
     * 归档中的只读父目录晚于子文件时，仍先恢复文件属性，最后按深度恢复目录权限。
     */
    @Test
    void lateReadOnlyDirectoriesRestoreAfterChildren() throws Exception {
        Path archive = Files.createTempFile(root, "readonly", ".tar");
        try (TarArchiveOutputStream out = new TarArchiveOutputStream(Files.newOutputStream(archive))) {
            tarEntry(out, "outer/inner/script.sh", false);
            for (String name : List.of("outer/inner/", "outer/", "./")) {
                TarArchiveEntry directory = new TarArchiveEntry(name, TarConstants.LF_DIR);
                directory.setMode(0);
                out.putArchiveEntry(directory);
                out.closeArchiveEntry();
            }
        }
        Path destination = Files.createDirectory(root.resolve("readonly"));
        Path outer = destination.resolve("outer");
        Path inner = outer.resolve("inner");
        try {
            SafeArchiveUtil.unpack(archive.toFile(), destination.toFile(), "GBK");
            assertTrue(Files.getPosixFilePermissions(destination).isEmpty());
        } finally {
            Files.setPosixFilePermissions(destination, java.nio.file.attribute.PosixFilePermissions.fromString("rwx------"));
            if (Files.exists(outer)) {
                Files.setPosixFilePermissions(outer, java.nio.file.attribute.PosixFilePermissions.fromString("rwx------"));
            }
            if (Files.exists(inner)) {
                Files.setPosixFilePermissions(inner, java.nio.file.attribute.PosixFilePermissions.fromString("rwx------"));
            }
        }
        assertEquals("content", Files.readString(inner.resolve("script.sh")));
        assertTrue(Files.getPosixFilePermissions(inner.resolve("script.sh")).contains(PosixFilePermission.OWNER_EXECUTE));
    }

    /**
     * 读取 TAR.GZ 条目名称，辅助检查目录与链接的兼容性。
     */
    private List<String> tarNames(byte[] archive) throws Exception {
        List<String> names = new ArrayList<>();
        try (TarArchiveInputStream tar = new TarArchiveInputStream(new GzipCompressorInputStream(new ByteArrayInputStream(archive)))) {
            TarArchiveEntry entry;
            while ((entry = tar.getNextEntry()) != null) {
                names.add(entry.getName());
            }
        }
        return names;
    }

    /**
     * 上传即解压时先隔离原包，失败不能覆盖删除历史同名压缩包，也不能部分覆盖业务文件。
     */
    @Test
    void uploadAndUnpackKeepsPreviousArchiveOnFailure() throws Exception {
        Path existing = Files.writeString(root.resolve("upload.zip"), "previous archive");
        Path first = Files.writeString(root.resolve("first.txt"), "previous file");
        Path archive = archive("zip", "../escape.txt", false, true);
        MockMultipartHttpServletRequest request = new MockMultipartHttpServletRequest();
        request.addFile(new MockMultipartFile("file", "upload.zip", "application/zip", Files.readAllBytes(archive)));
        JSONObject params = new JSONObject();
        params.put("path", "/upload.zip");
        params.put("unpack", 1);
        assertThrows(IOException.class, () -> new FileUploadApi().myDoService(params, request, new MockHttpServletResponse()));
        assertEquals("previous archive", Files.readString(existing));
        assertEquals("previous file", Files.readString(first));
        assertFalse(Files.exists(temp.resolve("escape.txt")));
    }

    /**
     * 上传即解压保留虚拟路径以及父目录的加号、百分号，正常制品不残留上传归档。
     */
    @Test
    void uploadAndUnpackNormalArchives() throws Exception {
        for (String format : List.of("zip", "tar", "tgz")) {
            Path directory = Files.createDirectory(root.resolve(format + "+100%"));
            Path archive = archive(format, "script.sh", false, false);
            MockMultipartHttpServletRequest request = new MockMultipartHttpServletRequest();
            request.addFile(new MockMultipartFile("file", "upload." + format, "application/octet-stream", Files.readAllBytes(archive)));
            JSONObject params = new JSONObject();
            params.put("path", "/" + directory.getFileName() + "/upload." + format);
            params.put("unpack", 1);
            new FileUploadApi().myDoService(params, request, new MockHttpServletResponse());
            assertEquals("content", Files.readString(directory.resolve("script.sh")));
            assertFalse(Files.exists(directory.resolve("upload." + format)));
        }
    }

    /**
     * 测试内容输入流。
     */
    private ByteArrayInputStream input(String content) {
        return new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 创建包含正常前置条目、目标条目及空目录的测试归档。
     */
    private Path archive(String format, String name, boolean link, boolean first) throws Exception {
        Path archive = Files.createTempFile(root, "archive", "." + format);
        if ("zip".equals(format)) {
            try (ZipArchiveOutputStream out = new ZipArchiveOutputStream(archive)) {
                if (first) {
                    zipEntry(out, "first.txt", false);
                }
                zipEntry(out, name, link);
                zipEntry(out, "empty/", false);
            }
        } else {
            try (OutputStream raw = Files.newOutputStream(archive);
                 OutputStream compressed = compress(raw, format);
                 TarArchiveOutputStream out = new TarArchiveOutputStream(compressed)) {
                if (first) {
                    tarEntry(out, "first.txt", false);
                }
                tarEntry(out, name, link);
                tarEntry(out, "empty/", false);
            }
        }
        return archive;
    }

    /**
     * 按测试格式包装 TGZ 压缩流。
     */
    private OutputStream compress(OutputStream out, String format) throws Exception {
        if ("tgz".equals(format)) {
            return new GzipCompressorOutputStream(out);
        }
        return out;
    }

    /**
     * 写入支持 Unix 类型的 ZIP 测试条目。
     */
    private void zipEntry(ZipArchiveOutputStream out, String name, boolean link) throws Exception {
        ZipArchiveEntry entry = new ZipArchiveEntry(name);
        if (link) {
            entry.setUnixMode(0120777);
        } else {
            entry.setUnixMode(0100755);
        }
        out.putArchiveEntry(entry);
        if (!name.endsWith("/")) {
            out.write("content".getBytes(StandardCharsets.UTF_8));
        }
        out.closeArchiveEntry();
    }

    /**
     * 保留绝对路径构造恶意 TAR，不让测试库自动清理漏洞输入。
     */
    private void tarEntry(TarArchiveOutputStream out, String name, boolean link) throws Exception {
        byte type = TarConstants.LF_NORMAL;
        if (link) {
            type = TarConstants.LF_SYMLINK;
        } else if (name.endsWith("/")) {
            type = TarConstants.LF_DIR;
        }
        TarArchiveEntry entry = new TarArchiveEntry(name, type, true);
        entry.setMode(0755);
        if (link) {
            entry.setLinkName("../outside");
        } else if (!name.endsWith("/")) {
            entry.setSize(7);
        }
        out.putArchiveEntry(entry);
        if (!link && !name.endsWith("/")) {
            out.write("content".getBytes(StandardCharsets.UTF_8));
        }
        out.closeArchiveEntry();
    }

    /**
     * 统计目标目录的直接文件数，验证失败时未部分发布。
     */
    private long countFiles(Path directory) throws Exception {
        try (java.util.stream.Stream<Path> files = Files.list(directory)) {
            return files.count();
        }
    }
}
