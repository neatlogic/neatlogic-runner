package com.neatlogic.autoexecrunner.util;

import com.neatlogic.autoexecrunner.exception.job.FifoWriteException;
import jnr.constants.platform.Errno;
import jnr.constants.platform.OpenFlags;
import jnr.posix.FileStat;
import jnr.posix.POSIX;
import jnr.posix.POSIXFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

/** 将一次交互输入写入现有 FIFO，所有等待均在当前线程内有界完成。 */
public final class FifoWriter {
    private static final Logger logger = LoggerFactory.getLogger(FifoWriter.class);
    private static final long TIMEOUT_NANOS = TimeUnit.SECONDS.toNanos(5);
    private static final long RETRY_NANOS = TimeUnit.MILLISECONDS.toNanos(10);

    private FifoWriter() {
    }

    /** 按需初始化原生绑定，让其他 Runner 接口独立于 FIFO 能力。 */
    private static class NativeAccess {
        private static final POSIX POSIX_API = POSIXFactory.getNativePOSIX();
    }

    /** 不创建文件；读端已经离开时立即失败，只在全部字节写入后返回。 */
    public static void write(Path path, byte[] contents) {
        long deadline = System.nanoTime() + TIMEOUT_NANOS;
        POSIX posix;
        try {
            posix = NativeAccess.POSIX_API;
            if (posix == null || !posix.isNative()) {
                throw new FifoWriteException("NATIVE_UNAVAILABLE");
            }
            writeExistingPipe(posix, path.toString(), contents, deadline);
        } catch (LinkageError ex) {
            logger.error("加载或调用 FIFO 原生接口失败", ex);
            throw new FifoWriteException("NATIVE_UNAVAILABLE", ex);
        }
    }

    /** 打开前检查路径，打开后核对 inode 和类型，避免将交互内容写入被替换的文件。 */
    private static void writeExistingPipe(POSIX posix, String path, byte[] contents, long deadline) {
        FileStat expected = posix.allocateStat();
        if (posix.lstat(path, expected) < 0) {
            throw failure(posix.errno());
        }
        if (!expected.isFifo()) {
            throw new FifoWriteException("NOT_FIFO");
        }
        int flags = OpenFlags.O_WRONLY.intValue() | OpenFlags.O_NONBLOCK.intValue()
                | OpenFlags.O_NOFOLLOW.intValue() | OpenFlags.O_CLOEXEC.intValue();
        int fd = posix.open(path, flags, 0);
        if (fd < 0) {
            throw failure(posix.errno());
        }
        try {
            FileStat actual = posix.allocateStat();
            if (posix.fstat(fd, actual) < 0) {
                throw failure(posix.errno());
            }
            if (!actual.isFifo() || expected.dev() != actual.dev() || expected.ino() != actual.ino()) {
                throw new FifoWriteException("NOT_FIFO");
            }
            writeFully(posix, fd, contents, deadline);
        } finally {
            posix.close(fd);
        }
    }

    /** 管道满时只等待剩余期限；短写只推进实际写入的字节，不重发整条输入。 */
    private static void writeFully(POSIX posix, int fd, byte[] contents, long deadline) {
        // 使用直接缓冲区，避免短写或管道暂满时反复将剩余内容复制到原生内存。
        ByteBuffer buffer = ByteBuffer.allocateDirect(contents.length);
        buffer.put(contents).flip();
        while (buffer.hasRemaining()) {
            if (Thread.currentThread().isInterrupted()) {
                throw new FifoWriteException("INTERRUPTED");
            }
            if (deadline - System.nanoTime() <= 0) {
                throw new FifoWriteException("TIMEOUT");
            }
            int count = posix.write(fd, buffer.slice(), buffer.remaining());
            if (count > 0) {
                buffer.position(buffer.position() + count);
            } else if (count < 0) {
                int errno = posix.errno();
                if (errno == Errno.EINTR.intValue()) {
                    continue;
                }
                if (errno != Errno.EAGAIN.intValue() && errno != Errno.EWOULDBLOCK.intValue()) {
                    throw failure(errno);
                }
                LockSupport.parkNanos(Math.min(RETRY_NANOS, deadline - System.nanoTime()));
            } else {
                throw new FifoWriteException("IO_ERROR");
            }
        }
    }

    /** 保留失败系统调用的 errno，便于区分无读取进程、文件缺失及写入失败。 */
    private static FifoWriteException failure(int errno) {
        return new FifoWriteException(Errno.valueOf(errno).name(), errno);
    }
}
