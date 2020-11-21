package org.idea.net;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;

public class TunTap implements Closeable {
	
	static native int open(String dev);	
	static native void close(int fd);	
	static native int write(int fd, ByteBuffer d, int len);	
	static native boolean await(int fd, long timeout);	
	static native int read(int fd, ByteBuffer d);
	
	final int fd;
	
	long timeout = 1000L;
	
	public TunTap(String dev) throws IOException {
		System.loadLibrary("tuntap");
		
		fd = open(dev);
		if (fd < 0) {
			throw new IOException("Failed to open device: " + fd);
		}
	}
	
	public void setTimeout(long timeout) {
		this.timeout = timeout;
	}
	
	@Override
	public void close() throws IOException {
		close(fd);		
	}
	
	public OutputStream getOutputStream() {
		return new OutputStream() {
			
			@Override
			public void write(byte[] b, int off, int len) throws IOException {
				ByteBuffer bb = ByteBuffer.allocateDirect(len);
				bb.put(b, off, len);
				bb.flip();
				
				TunTap.write(fd, bb, len);
			}
			
			@Override
			public void write(int b) throws IOException {
				ByteBuffer bb = ByteBuffer.allocateDirect(1);
				bb.put((byte) b);
				bb.flip();
				
				TunTap.write(fd, bb, 1);												
			}
			
			@Override
			public void close() throws IOException {
				TunTap.this.close();
			}
		};
	}
	
	public InputStream getInputStream() {
		return new InputStream() {
			
			@Override
			public int read(byte[] b, int off, int len) throws IOException {
				if (TunTap.await(fd, timeout) == false) {
					return 0;
				}
				
				ByteBuffer bb = ByteBuffer.allocateDirect(len);
				int s = TunTap.read(fd, bb);
				if (s < 0) {
					throw new IOException("Failed to read: " + s);
				}
				
				bb.limit(s);
				
				for (int i = 0;i < s;i++) {
					b[off + i] = bb.get(i);
				}
				
				return s;
			}

			@Override
			public int read() throws IOException {
				if (TunTap.await(fd, timeout) == false) {
					return 0;
				}
				
				ByteBuffer bb = ByteBuffer.allocateDirect(1);
				int s = TunTap.read(fd, bb);
				if (s < 0) {
					throw new IOException("Failed to read: " + s);
				}
				
				bb.limit(s);
				
				return bb.get(0);
			}
			
			@Override
			public int available() throws IOException {
				return TunTap.await(fd, timeout)? 1 : 0; // XXX - could be better
			}
			
			@Override
			public long skip(long n) throws IOException {
				throw new IOException("Not yet implemented");
			}
			
			@Override
			public void close() throws IOException {
				TunTap.this.close();
			}
		};
	}
}
