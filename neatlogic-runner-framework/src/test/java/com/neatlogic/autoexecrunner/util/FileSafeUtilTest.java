package com.neatlogic.autoexecrunner.util;

import com.neatlogic.autoexecrunner.common.config.Config;
import com.neatlogic.autoexecrunner.exception.core.ApiRuntimeException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FileSafeUtilTest {
    private static final String FORBIDDEN_PATH_CONFIG_KEY = "file.safe.forbidden.path";

    @TempDir
    Path tempDirectory;

    private String originalForbiddenPath;

    @BeforeEach
    void saveFileSafeConfig() {
        originalForbiddenPath = Config.properties.getProperty(FORBIDDEN_PATH_CONFIG_KEY);
        Config.properties.remove(FORBIDDEN_PATH_CONFIG_KEY);
    }

    @AfterEach
    void restoreFileSafeConfig() {
        restoreProperty(FORBIDDEN_PATH_CONFIG_KEY, originalForbiddenPath);
    }

    @Test
    void shouldKeepNormalizedPathInsideRoot() throws Exception {
        Path root = Files.createDirectory(tempDirectory.resolve("root"));
        Path expectedFile = Files.createFile(root.resolve("file.txt"));

        File relativeFile = FileSafeUtil.getPath("folder/../file.txt", root.toString());
        File absoluteFile = FileSafeUtil.getPath(expectedFile.toString(), root.toString());

        assertEquals(expectedFile, relativeFile.toPath());
        assertEquals(expectedFile, absoluteFile.toPath());
    }

    @Test
    void shouldRejectNormalizedPathOutsideRoot() throws Exception {
        Path root = Files.createDirectory(tempDirectory.resolve("root"));

        assertThrows(ApiRuntimeException.class,
                () -> FileSafeUtil.getPath("../outside.txt", root.toString()));
        assertThrows(ApiRuntimeException.class,
                () -> FileSafeUtil.getPath("../../outside.txt", root.toString()));
        assertThrows(ApiRuntimeException.class,
                () -> FileSafeUtil.getPath("%2e%2e/%2e%2e/outside.txt", root.toString()));
    }

    @Test
    void shouldKeepBusinessPathWhenRootIsSymbolicLink() throws Exception {
        Path realRoot = Files.createDirectory(tempDirectory.resolve("real-root"));
        Files.createFile(realRoot.resolve("file.txt"));
        Path linkRoot = Files.createSymbolicLink(tempDirectory.resolve("link-root"), realRoot);

        File file = FileSafeUtil.getPath("file.txt", linkRoot.toString());

        assertEquals(linkRoot.resolve("file.txt"), file.toPath());
    }

    @Test
    void shouldAllowSymbolicLinkWhoseTargetIsInsideRoot() throws Exception {
        Path root = Files.createDirectory(tempDirectory.resolve("root"));
        Path target = Files.createDirectory(root.resolve("target"));
        Files.createFile(target.resolve("file.txt"));
        Path link = Files.createSymbolicLink(root.resolve("link"), target);

        File file = FileSafeUtil.getPath("link/file.txt", root.toString());

        assertEquals(link.resolve("file.txt"), file.toPath());
    }

    /** 即使链接位于业务目录内，也要拒绝指向目录外的真实文件。 */
    @Test
    void shouldRejectExternalSymbolicLinkTarget() throws Exception {
        Path root = Files.createDirectory(tempDirectory.resolve("root"));
        Path externalRoot = Files.createDirectory(tempDirectory.resolve("external"));
        Files.createFile(externalRoot.resolve("file.txt"));
        Files.createSymbolicLink(root.resolve("link"), externalRoot);

        assertThrows(ApiRuntimeException.class, () -> FileSafeUtil.getPath("link/file.txt", root.toString()));
    }

    /** 使用根目录内的目标，单独验证禁止目录对软链接别名也生效。 */
    @Test
    void shouldRejectSymbolicLinkToForbiddenPathInsideRoot() throws Exception {
        Path root = Files.createDirectory(tempDirectory.resolve("root"));
        Path forbiddenRoot = Files.createDirectory(root.resolve("forbidden"));
        Files.createFile(forbiddenRoot.resolve("file.txt"));
        Files.createSymbolicLink(root.resolve("link"), forbiddenRoot);
        Config.properties.setProperty(FORBIDDEN_PATH_CONFIG_KEY, forbiddenRoot.toString());

        assertThrows(ApiRuntimeException.class, () -> FileSafeUtil.getPath("link/file.txt", root.toString()));
    }

    @Test
    void shouldRejectLogicalForbiddenPath() throws Exception {
        Path root = Files.createDirectory(tempDirectory.resolve("root"));
        Path forbiddenRoot = Files.createDirectory(root.resolve("forbidden"));
        Files.createFile(forbiddenRoot.resolve("file.txt"));
        Config.properties.setProperty(FORBIDDEN_PATH_CONFIG_KEY, forbiddenRoot.toString());

        assertThrows(ApiRuntimeException.class,
                () -> FileSafeUtil.getPath("forbidden/file.txt", root.toString()));
    }

    private void restoreProperty(String key, String value) {
        if (value == null) {
            Config.properties.remove(key);
        } else {
            Config.properties.setProperty(key, value);
        }
    }
}
