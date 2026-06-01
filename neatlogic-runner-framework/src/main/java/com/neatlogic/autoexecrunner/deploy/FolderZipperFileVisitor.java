package com.neatlogic.autoexecrunner.deploy;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class FolderZipperFileVisitor extends SimpleFileVisitor<Path> {
	String rootPath = null;
	int rootLen = 0;
	ZipOutputStream zip = null;

	public FolderZipperFileVisitor(Path rootPath, ZipOutputStream zip) {
		super();
		this.rootPath = rootPath.toAbsolutePath().toString();
		this.rootLen = this.rootPath.length() + 1;
		this.zip = zip;
	}

	@Override
	public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
		String abPath = dir.toAbsolutePath().toString();
		if (abPath.length() > rootLen) {
			String relPath = abPath.substring(rootLen);

			zip.putNextEntry(new ZipEntry(relPath + "/"));
			zip.closeEntry();
		}

		return FileVisitResult.CONTINUE;
	}

	@Override
	public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
		String abPath = file.toAbsolutePath().toString();
		String relPath = abPath.substring(rootLen);

		if (attrs.isRegularFile()) {
			byte[] buf = new byte[4096];
			int len;
			FileInputStream in = new FileInputStream(abPath);

			try {
				zip.putNextEntry(new ZipEntry(relPath));
				while ((len = in.read(buf)) > 0) {
					zip.write(buf, 0, len);
				}
				zip.closeEntry();
			} finally {
				if (in != null)
					in.close();
			}
		}

		return FileVisitResult.CONTINUE;
	}
}
