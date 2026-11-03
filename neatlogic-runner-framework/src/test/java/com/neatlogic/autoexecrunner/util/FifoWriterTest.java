package com.neatlogic.autoexecrunner.util;

import com.neatlogic.autoexecrunner.exception.job.FifoWriteException;
import jnr.constants.platform.OpenFlags;
import jnr.posix.POSIX;
import jnr.posix.POSIXFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.Collections;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.locks.LockSupport;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

/** 用真实命名管道覆盖 Runner 与交互进程的边界。 */
@Timeout(15)
class FifoWriterTest {
    @TempDir Path root;
    private final POSIX posix = POSIXFactory.getNativePOSIX();

    /** 临时 FIFO 的生命周期独立于其他测试及真实作业。 */
    private Path pipe() throws Exception {
        Path pipe = root.resolve("input.pipe");
        assertEquals(0, new ProcessBuilder("mkfifo", pipe.toString()).start().waitFor());
        return pipe;
    }

    /** 正常写入只推进实际字节，长输入跨多次系统调用仍保持原顺序。 */
    @Test
    void deliversExactBytes() throws Exception {
        Path path = pipe();
        for (String value : new String[]{"[user]# Yes", "[user]# No", "[user]# 特殊 字符 '%' & ".repeat(9000)}) {
            int reader = posix.open(path.toString(), OpenFlags.O_RDONLY.intValue() | OpenFlags.O_NONBLOCK.intValue(), 0);
            assertTrue(reader >= 0);
            byte[] expected = (value + System.lineSeparator()).getBytes(StandardCharsets.UTF_8);
            try {
                CompletableFuture<byte[]> received = CompletableFuture.supplyAsync(() -> {
                    ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                    byte[] chunk = new byte[2048];
                    long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(8);
                    while (bytes.size() < expected.length && System.nanoTime() < deadline) {
                        int n = posix.read(reader, chunk, chunk.length);
                        if (n > 0) {
                            bytes.write(chunk, 0, n);
                        } else {
                            LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(1));
                        }
                    }
                    return bytes.toByteArray();
                });
                FifoWriter.write(path, expected);
                assertArrayEquals(expected, received.get(9, TimeUnit.SECONDS));
            } finally {
                posix.close(reader);
            }
        }
    }

    /** 读端被强杀后保留 FIFO，连续失败不创建文件、不截断普通文件。 */
    @Test
    void stalePipeFailsImmediately() throws Exception {
        Path path = pipe();
        // 子进程先确认 FIFO 已打开，再强杀，模拟作业退出时未执行清理。
        Process reader = new ProcessBuilder("sh", "-c", "exec 3<>\"$1\"; echo ready; read -r value <&3",
                "test-reader", path.toString()).start();
        try {
            assertEquals("ready", reader.inputReader().readLine());
        } finally {
            reader.destroyForcibly().waitFor();
        }
        assertTimeout(Duration.ofSeconds(2), () -> {
            for (int i = 0; i < 100; i++) {
                FifoWriteException ex = assertThrows(FifoWriteException.class, () -> FifoWriter.write(path, new byte[1]));
                assertTrue(ex.getMessage().contains("ENXIO"), ex.getMessage());
            }
        });
        Path missing = root.resolve("missing");
        assertThrows(FifoWriteException.class, () -> FifoWriter.write(missing, new byte[1]));
        assertFalse(Files.exists(missing));
        Path ordinary = Files.writeString(root.resolve("ordinary"), "preserved");
        assertThrows(FifoWriteException.class, () -> FifoWriter.write(ordinary, new byte[1]));
        assertEquals("preserved", Files.readString(ordinary));
        if (posix.geteuid() != 0) {
            Files.setPosixFilePermissions(path, Collections.emptySet());
            FifoWriteException denied = assertThrows(FifoWriteException.class, () -> FifoWriter.write(path, new byte[1]));
            assertTrue(denied.getMessage().contains("EACCES"), denied.getMessage());
        }
        Path link = Files.createSymbolicLink(root.resolve("link"), ordinary);
        assertThrows(FifoWriteException.class, () -> FifoWriter.write(link, new byte[1]));
    }

    /** 读端不消费时可中断、超时；读端离开时 JVM 存活，所有写端均被关闭。 */
    @Test
    void fullPipeAndReaderExitAreBounded() throws Exception {
        Path path = pipe();
        int reader = posix.open(path.toString(), OpenFlags.O_RDONLY.intValue() | OpenFlags.O_NONBLOCK.intValue(), 0);
        assertTrue(reader >= 0);
        byte[] large = new byte[4 * 1024 * 1024];
        try {
            assertTimeout(Duration.ofSeconds(7), () -> assertTrue(
                    assertThrows(FifoWriteException.class, () -> FifoWriter.write(path, large)).getMessage().contains("TIMEOUT")));
            AtomicBoolean interrupted = new AtomicBoolean();
            FutureTask<Void> write = new FutureTask<>(() -> {
                try {
                    FifoWriter.write(path, large);
                } finally {
                    interrupted.set(Thread.currentThread().isInterrupted());
                }
                return null;
            });
            Thread thread = new Thread(write);
            thread.start();
            thread.interrupt();
            assertTrue(assertThrows(ExecutionException.class, () -> write.get(2, TimeUnit.SECONDS))
                    .getCause().getMessage().contains("INTERRUPTED"));
            assertTrue(interrupted.get());
            // 排空此前成功写入的数据；返回零表示已经没有遗留写端。
            byte[] chunk = new byte[4096];
            int n;
            do {
                n = posix.read(reader, chunk, chunk.length);
            } while (n > 0);
            assertEquals(0, n);
            FutureTask<Void> lastWrite = new FutureTask<>(() -> {
                FifoWriter.write(path, large);
                return null;
            });
            Thread writer = new Thread(lastWrite);
            writer.start();
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(2);
            while ((n = posix.read(reader, chunk, chunk.length)) <= 0 && System.nanoTime() < deadline) {
                Thread.sleep(5);
            }
            assertTrue(n > 0);
            posix.close(reader);
            reader = -1;
            assertTrue(assertThrows(ExecutionException.class, () -> lastWrite.get(2, TimeUnit.SECONDS))
                    .getCause().getMessage().contains("EPIPE"));
        } finally {
            if (reader >= 0) {
                posix.close(reader);
            }
        }
    }
}
