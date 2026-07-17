package com.aidigital.aionboarding.observability;

import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.WriteListener;

import java.io.IOException;

/**
 * Delegates every write to the real response stream while incrementing a running byte
 * counter, so payload-size metrics can be recorded without retaining/buffering body content.
 */
class CountingServletOutputStream extends ServletOutputStream {

	private final ServletOutputStream delegate;
	private long byteCount;

	CountingServletOutputStream(ServletOutputStream delegate) {
		this.delegate = delegate;
	}

	@Override
	public void write(int b) throws IOException {
		delegate.write(b);
		byteCount += 1;
	}

	@Override
	public void write(byte[] b, int off, int len) throws IOException {
		delegate.write(b, off, len);
		byteCount += len;
	}

	@Override
	public boolean isReady() {
		return delegate.isReady();
	}

	@Override
	public void setWriteListener(WriteListener writeListener) {
		delegate.setWriteListener(writeListener);
	}

	/**
	 * Returns the number of bytes written through this stream so far.
	 *
	 * @return running byte count
	 */
	long getByteCount() {
		return byteCount;
	}
}
