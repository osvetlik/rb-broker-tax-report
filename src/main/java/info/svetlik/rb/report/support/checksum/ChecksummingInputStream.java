package info.svetlik.rb.report.support.checksum;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;

public class ChecksummingInputStream extends FilterInputStream implements ChecksummingStream {

	private final MessageDigest digest;

	public ChecksummingInputStream(InputStream in, DigestAlgorithm algorithm) {
		super(in);
		this.digest = algorithm.digest();
	}

	@Override
	public byte[] rawDigest() {
		return digest.digest();
	}

	@Override
	public int read() throws IOException {
		final var data = in.read();
		if (data != -1) {
			digest.update((byte) data);
		}
		return data;
	}

	@Override
	public int read(byte[] b) throws IOException {
		final var length = super.read(b);
		if (length != -1) {
			digest.update(b, 0, length);
		}
		return length;
	}

	@Override
	public int read(byte[] b, int off, int len) throws IOException {
		final var length = super.read(b, off, len);
		if (length != -1) {
			digest.update(b, off, length);
		}
		return length;
	}

}
