package com.neatlogic.autoexecrunner.util;

import com.neatlogic.autoexecrunner.deploy.ConcatableTarOutputStream;
import com.neatlogic.autoexecrunner.deploy.FolderZipperFileVisitor;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.archivers.tar.TarConstants;
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;

import javax.servlet.ServletOutputStream;
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
	 * throws IOException { Path rootPath = Paths.get(rootDir).toAbsolutePath();
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

		Path rootPath = Paths.get(rootDir).toAbsolutePath();
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
				Path subPath = rootPath.resolve(subDir);

				try {
					if (Files.exists(subPath)) {
						// 遍历文件目录树
						Files.walkFileTree(subPath, options, Integer.MAX_VALUE, new SimpleFileVisitor<Path>() {

							// 当成功访问到一个文件
							@Override
							public FileVisitResult visitFile(Path file, BasicFileAttributes attributes) throws IOException {
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

	public static void tgzFolderToResponse(String resRootFullDir, String rootDir, List<String> subDirs, ServletOutputStream out) throws IOException {
		resRootFullDir = Paths.get(resRootFullDir).toAbsolutePath().toString();
		String lockFilePath = resRootFullDir + ".lock";

		Path rootPath = Paths.get(rootDir).toAbsolutePath();
		rootDir = rootPath.toString();

		File lockFile = new File(lockFilePath);

		RandomAccessFile randomAccessFile = null;
		FileChannel lockChannel = null;
		FileLock buildLock = null;

		Process proc = null;
		InputStream in = null;
		InputStream err = null;

		int bufLen = BUF_SIZE;
		byte[] buf = new byte[bufLen];
		try {
			randomAccessFile = new RandomAccessFile(lockFile, "rw");
			lockChannel = randomAccessFile.getChannel();
			buildLock = lockChannel.lock(0, 4096, true);

			List<String> cmdList = new ArrayList<String>();
			cmdList.add("tar");
			cmdList.add("-czf");
			cmdList.add("-");
			if (subDirs != null && subDirs.size() > 0) {
				boolean isAllValid = false;
				for (int i = 0; i < subDirs.size(); i++) {
					if (Files.exists(rootPath.resolve(subDirs.get(i)))) {
						cmdList.add(subDirs.get(i));
						isAllValid = true;
					}
				}
				
				// 这里判断如果所有目录都不存在, 那么就报错抛出, 否则tar会出现 create an empty archive 错误
				if (!isAllValid) {
					throw new IOException("tar dir failed:" + rootDir + ": 所有要打包的文件夹都不存在");
				}
				
			} else {
				cmdList.add(".");
			}

			ProcessBuilder builder = new ProcessBuilder(cmdList);
			builder.directory(new File(rootDir));
			proc = builder.start();

			in = proc.getInputStream();
			err = proc.getErrorStream();
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
				throw new IOException("tar dir failed:" + rootDir + ":" + errMsg);
			}
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
			if (in != null) {
				try {
					in.close();
				} catch (IOException e) {
				}
			}
			if (err != null) {
				try {
					err.close();
				} catch (IOException e) {
				}
			}
			if (proc != null && proc.isAlive()) {
				proc.destroyForcibly();
			}
		}

	}

	public static void zipFolderToResponse(String resRootFullDir, String rootDir, ServletOutputStream out) throws IOException {
		resRootFullDir = Paths.get(resRootFullDir).toAbsolutePath().toString();
		String lockFilePath = resRootFullDir + ".lock";

		Path rootPath = Paths.get(rootDir).normalize().toAbsolutePath();
		rootDir = rootPath.toString();

		File lockFile = new File(lockFilePath);
		RandomAccessFile randomAccessFile = new RandomAccessFile(lockFile, "rw");
		FileChannel lockChannel = randomAccessFile.getChannel();
		FileLock buildLock = lockChannel.lock(0, 4096, true);

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
			if (buildLock != null) {
				buildLock.release();
			}
			if (lockChannel != null) {
				lockChannel.close();
			}
			if (randomAccessFile != null) {
				randomAccessFile.close();
			}
		}

	}

	public static void main(String[] args) throws IOException {
		ZipOutputStream zipStream = null;
		FileOutputStream fileWriter = null;
		fileWriter = new FileOutputStream("/tmp/test.zip");
		zipStream = new ZipOutputStream(fileWriter);

		String rootDir = "/app/ezdeploy/data/version/1/1/1.0.10/appbuild";
		final Path rootPath = Paths.get(rootDir).toAbsolutePath();

		Files.walkFileTree(rootPath, EnumSet.noneOf(FileVisitOption.class), Integer.MAX_VALUE, new FolderZipperFileVisitor(rootPath, zipStream));
		zipStream.finish();
		zipStream.flush();
		zipStream.close();
	}
}
