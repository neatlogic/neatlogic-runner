package com.neatlogic.autoexecrunner.util;

import com.alibaba.fastjson.JSONObject;
import com.neatlogic.autoexecrunner.api.job.node.JobPhaseNodeSubmitWaitInputApi;
import com.neatlogic.autoexecrunner.common.config.Config;
import com.neatlogic.autoexecrunner.exception.core.ApiRuntimeException;
import jnr.constants.platform.OpenFlags;
import jnr.posix.POSIX;
import jnr.posix.POSIXFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/** 覆盖 Runner 接口的原始路径、消息格式及作业边界。 */
class WaitInputPipeApiTest {
    @TempDir Path root;

    /** 区分真实路径和 URL 编码；验证旧请求及带作业标识的请求。 */
    @Test
    void preservesWireFormatAndChecksWorkRoot() throws Exception {
        Path actualRoot = Files.createDirectories(root.resolve("systems/autoexec/data/job")).toRealPath();
        Path alias = Files.createSymbolicLink(root.resolve("autoexec"), root.resolve("systems/autoexec").toAbsolutePath());
        Path configuredRoot = alias.resolve("data/job");
        Path path = Files.createDirectories(actualRoot.resolve("180/743/458/816/000/5/log/interact_mselect")).resolve("a+b%20.pipe");
        assertEquals(0, new ProcessBuilder("mkfifo", path.toString()).start().waitFor());
        POSIX posix = POSIXFactory.getNativePOSIX();
        int reader = posix.open(path.toString(), OpenFlags.O_RDONLY.intValue() | OpenFlags.O_NONBLOCK.intValue(), 0);
        assertTrue(reader >= 0);
        Field home = Config.class.getDeclaredField("AUTOEXEC_HOME");
        home.setAccessible(true);
        Object previous = home.get(null);
        try {
            home.set(null, configuredRoot.toString());
            JobPhaseNodeSubmitWaitInputApi api = new JobPhaseNodeSubmitWaitInputApi();
            JSONObject params = new JSONObject();
            params.put("pipeFile", configuredRoot.resolve(actualRoot.relativize(path)).toString());
            for (String text : new String[]{"[user]# Yes", "[user]# No", "[user]# 中文 % ' &", "[user]# [\"3\"]"}) {
                params.put("option", text);
                assertNull(api.myDoService(params));
                byte[] expected = (text + System.lineSeparator()).getBytes(StandardCharsets.UTF_8);
                byte[] actual = new byte[expected.length];
                assertEquals(expected.length, posix.read(reader, actual, actual.length));
                assertArrayEquals(expected, actual);
                params.put("jobId", 1807434588160005L);
                // 同一作业可通过配置路径和引擎上报的真实路径提交。
                params.put("pipeFile", path.toString());
            }
            params.put("jobId", 43L);
            assertThrows(ApiRuntimeException.class, () -> api.myDoService(params));
            params.remove("jobId");
            params.put("pipeFile", root.resolve("../other/input.pipe").toString());
            assertThrows(ApiRuntimeException.class, () -> api.myDoService(params));
        } finally {
            home.set(null, previous);
            posix.close(reader);
        }
    }
}
