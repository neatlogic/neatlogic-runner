package com.techsure.autoexecrunner.deploy;

import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;

import java.io.IOException;
import java.io.OutputStream;

public class ConcatableTarOutputStream extends TarArchiveOutputStream {
	private OutputStream os;
	private boolean isLastPart = false;

	public ConcatableTarOutputStream(OutputStream os, String charSet, boolean isLastPart) {
		super(os, charSet);
		this.setLongFileMode(TarArchiveOutputStream.LONGFILE_POSIX);
		this.os = os;
		this.isLastPart = isLastPart;
	}

	/**
	 * Ends the TAR archive without closing the underlying OutputStream.
	 *
	 * An archive consists of a series of file entries terminated by an
	 * end-of-archive entry, which consists of two 512 blocks of zero bytes. POSIX.1
	 * requires two EOF records, like some other implementations.
	 *
	 * @throws IOException on error
	 */
	@Override
	public void finish() throws IOException {
		if (this.isLastPart) {
			super.finish();
		}
	}

	/**
	 * Closes the underlying OutputStream.
	 *
	 * @throws IOException on error
	 */
	@Override
	public void close() throws IOException {
		if (this.isLastPart) {
			super.close();
			this.os.close();
		} else {
			this.os.close();
		}
	}
}
