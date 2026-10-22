package com.neatlogic.autoexecrunner.util;

import com.neatlogic.autoexecrunner.common.config.Config;
import com.neatlogic.autoexecrunner.deploy.ConcatableTarOutputStream;
import com.neatlogic.autoexecrunner.deploy.FolderZipperFileVisitor;
import jakarta.servlet.ServletOutputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.archivers.tar.TarConstants;
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;

import java.io.*;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipOutputStream;

public class FolderZipperUtil {

	private static int BUF_SIZE = 16 * 1024;

	/*
	 * public static void zipFolderToResponse(String rootDir, ZipOutputStream zip)
	 * throws IOException { Path rootPath = Paths.get(rootDir).toAbsolutePath().normalize();
	 * Files.walkFileTree(rootPath, EnumSet.noneOf(FileVisitOption.class),
	 * Integer.MAX_VALUE, new FolderZipperFileVisitor(rootPath, zip)); zip.finish();
	 * }
	 */

	public static void tgzConcatableFolderToResponse(String resRootFullDir, String rootDir, List<String> subDirs, ServletOutputStream out, final String targetDirPrefix, String charSet, boolean followLinks, boolean isLastPart) throws IOException {
		if ("".equals(charSet)) {
			charSet = null;
		}

		resRootFullDir = Paths.get(resRootFullDir).toAbsolutePath().toString();
		String lockFilePath = resRootFullDir + ".lock";

		Path rootPath = Paths.get(rootDir).toAbsolutePath().normalize();
		rootDir = rootPath.toString();

		File lockFile = new File(lockFilePath);

		Set<FileVisitOption> options = null;
		if (followLinks) {
			options = EnumSet.of(FileVisitOption.FOLLOW_LINKS);
		} else {
			options = EnumSet.noneOf(FileVisitOption.class);
		}

		RandomAccessFile randomAccessFile = null;
		FileChannel lockChannel = null;
		FileLock buildLock = null;

		try {
			randomAccessFile = new RandomAccessFile(lockFile, "rw");
			lockChannel = randomAccessFile.getChannel();
			buildLock = lockChannel.lock(0, 4096, true);

			BufferedOutputStream buffOut = new BufferedOutputStream(out);
			GzipCompressorOutputStream gzOut = new GzipCompressorOutputStream(buffOut);
			TarArchiveOutputStream tarOut = new ConcatableTarOutputStream(gzOut, charSet, isLastPart);

			for (String subDir : subDirs) {
				String safeSubDir = FileSafeUtil.getValidatedRelativePath(rootDir, subDir);
				Path subPath = rootPath.resolve(safeSubDir);
                validatePackedDirectory(rootPath, subPath);

				try {
					if (Files.exists(subPath)) {
						// 遍历文件目录树
						Files.walkFileTree(subPath, options, Integer.MAX_VALUE, new SimpleFileVisitor<Path>() {

							/**
                             * 遍历目录前校验真实目标，避免目录链接越界。
                             */
                            @Override
                            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                                validatePackedPath(rootPath, dir);
                                return FileVisitResult.CONTINUE;
                            }

                            // 当成功访问到一个文件
                            @Override
							public FileVisitResult visitFile(Path file, BasicFileAttributes attributes) throws IOException {
								validatePackedPath(rootPath, file);
                                // 获取当前遍历文件名称
								Path targetFile = rootPath.relativize(file);

								if (attributes.isDirectory()) {
									String fileName = file.getFileName().toString();
									if (fileName.equals(".svn") || fileName.equals(".git")) {
										return FileVisitResult.SKIP_SUBTREE;
									}
								} else if (attributes.isSymbolicLink()) {
									// 如果不followlink，则创建一个symbolic link的entry
									if (!followLinks) {
										TarArchiveEntry tarEntry = new TarArchiveEntry(targetDirPrefix + targetFile.toString(), TarConstants.LF_SYMLINK);
										tarEntry.setLinkName(Files.readSymbolicLink(file).toString());
										tarOut.putArchiveEntry(tarEntry);
										tarOut.closeArchiveEntry();
										// 继续下一个遍历文件处理
										return FileVisitResult.CONTINUE;
									}
								}

								// 将该文件打包压缩
								TarArchiveEntry tarEntry = new TarArchiveEntry(file.toFile(), targetDirPrefix + targetFile.toString());
								tarOut.putArchiveEntry(tarEntry);
								Files.copy(file, tarOut);
								tarOut.closeArchiveEntry();
								// 继续下一个遍历文件处理
								return FileVisitResult.CONTINUE;
							}

							// 当前遍历文件访问失败
							@Override
							public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
								// System.err.printf("无法对该文件压缩打包为tar.gz : %s%n%s%n", file, exc);
								throw new IOException("Can not tar file:" + file.toString(), exc);
								// return FileVisitResult.CONTINUE;
							}

						});
					}
				} finally {

				}
			}
			// for循环完成之后，finish-tar包输出流
			tarOut.close();
		} catch (Exception e) {
			throw new IOException("tar dir failed:" + rootDir, e);
		} finally {
			if (buildLock != null) {
				try {
					buildLock.release();
				} catch (IOException e) {
				}
			}
			if (lockChannel != null) {
				try {
					lockChannel.close();
				} catch (IOException e) {
				}
			}
			if (randomAccessFile != null) {
				try {
					randomAccessFile.close();
				} catch (IOException e) {
				}
			}
		}

	}

    /**
     * 使用逐条目校验的 TAR 输出，保留目录和目录内合法软链接的既有打包行为。
     */
    public static void tgzFolderToResponse(String resRootFullDir, String rootDir, List<String> subDirs, ServletOutputStream out) throws IOException {
        Path root = Paths.get(rootDir).toAbsolutePath().normalize();
        List<Path> selections = new ArrayList<>();
        if (subDirs == null || subDirs.isEmpty()) {
            selections.add(root);
        } else {
            for (String subDir : subDirs) {
                String safePath = FileSafeUtil.getValidatedRelativePath(root.toString(), subDir);
                Path selected = root.resolve(safePath);
                if (Files.exists(selected)) {
                    selections.add(selected);
                }
            }
        }
        if (selections.isEmpty()) {
            throw new IOException("所有要打包的文件夹都不存在");
        }
        try (RandomAccessFile lockFile = new RandomAccessFile(resRootFullDir + ".lock", "rw");
             FileChannel channel = lockFile.getChannel();
             FileLock ignored = channel.lock(0, 4096, true);
             GzipCompressorOutputStream gzip = new GzipCompressorOutputStream(new BufferedOutputStream(out));
             TarArchiveOutputStream tar = new TarArchiveOutputStream(gzip)) {
            tar.setLongFileMode(TarArchiveOutputStream.LONGFILE_POSIX);
            tar.setBigNumberMode(TarArchiveOutputStream.BIGNUMBER_POSIX);
            for (Path selected : selections) {
                validatePackedDirectory(root, selected);
                Path traversal = selected;
                // 原系统 tar 以根目录作为工作目录；根目录本身为软链接时仍应打包其内容。
                if (selected.equals(root) && Files.isSymbolicLink(selected)) {
                    traversal = selected.toRealPath();
                }
                final Path traversalRoot = traversal;
                Files.walkFileTree(traversalRoot, new SimpleFileVisitor<Path>() {
                    /**
                     * 保留空目录并在读取前检查目录边界。
                     */
                    @Override
                    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                        writeTarEntry(root, selected.resolve(traversalRoot.relativize(dir)), tar, true);
                        return FileVisitResult.CONTINUE;
                    }

                    /**
                     * 保留普通文件属性和合法软链接，特殊文件拒绝打包。
                     */
                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                        if (!attrs.isRegularFile() && !attrs.isSymbolicLink()) {
                            throw new IOException("不支持打包特殊文件：" + file);
                        }
                        writeTarEntry(root, selected.resolve(traversalRoot.relativize(file)), tar, false);
                        return FileVisitResult.CONTINUE;
                    }
                });
            }
        }
    }

    /**
     * 在每个 TAR 条目实际写出前校验逻辑路径与真实目标。
     */
    private static void writeTarEntry(Path root, Path file, TarArchiveOutputStream tar, boolean directory) throws IOException {
        validatePackedPath(root, file);
        String name = root.relativize(file).toString().replace(File.separatorChar, '/');
        if (name.isEmpty()) {
            name = ".";
        }
        TarArchiveEntry entry;
        if (!directory && Files.isSymbolicLink(file)) {
            entry = new TarArchiveEntry(name, TarConstants.LF_SYMLINK);
            entry.setLinkName(Files.readSymbolicLink(file).toString());
        } else {
            entry = new TarArchiveEntry(file.toFile(), name);
        }
        tar.putArchiveEntry(entry);
        if (!directory && !Files.isSymbolicLink(file)) {
            Files.copy(file, tar);
        }
        tar.closeArchiveEntry();
    }

    /**
     * 打包边界使用当前制品目录，禁止路径同时按 DATA_HOME 的配置语义匹配。
     */
    private static void validatePackedPath(Path root, Path file) throws IOException {
        FileSafeUtil.validateFilePathAllowed(root.toFile(), file.toFile());
        if (Config.DATA_HOME() != null && FileSafeUtil.isSubPath(new File(Config.DATA_HOME()), root.toFile())) {
            FileSafeUtil.validateFilePathAllowed(new File(Config.DATA_HOME()), file.toFile());
        }
    }

    /**
     * 打包前完整检查所选目录，避免隐藏子目录或软链接绕过禁止访问配置。
     */
    private static void validatePackedDirectory(Path root, Path selected) throws IOException {
        FileSafeUtil.validateNoForbiddenPathInDirectory(root.toFile(), selected.toFile());
        if (Config.DATA_HOME() != null && FileSafeUtil.isSubPath(new File(Config.DATA_HOME()), root.toFile())) {
            FileSafeUtil.validateNoForbiddenPathInDirectory(new File(Config.DATA_HOME()), selected.toFile());
        }
    }

    /**
     * 复用逐条目校验的 ZIP 打包实现，并保留发布目录读锁。
     */
    public static void zipFolderToResponse(String resRootFullDir, String rootDir, ServletOutputStream out) throws IOException {
        try (RandomAccessFile lockFile = new RandomAccessFile(resRootFullDir + ".lock", "rw");
             FileChannel channel = lockFile.getChannel();
             FileLock ignored = channel.lock(0, 4096, true)) {
            FileUtil.zipDirectory(rootDir, out);
        }
    }

	public static void main(String[] args) throws IOException {
		ZipOutputStream zipStream = null;
		FileOutputStream fileWriter = null;
		fileWriter = new FileOutputStream("/tmp/test.zip");
		zipStream = new ZipOutputStream(fileWriter);

		String rootDir = "/app/ezdeploy/data/version/1/1/1.0.10/appbuild";
		final Path rootPath = Paths.get(rootDir).toAbsolutePath().normalize();

		Files.walkFileTree(rootPath, EnumSet.noneOf(FileVisitOption.class), Integer.MAX_VALUE, new FolderZipperFileVisitor(rootPath, zipStream));
		zipStream.finish();
		zipStream.flush();
		zipStream.close();
	}
}
