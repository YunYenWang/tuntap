package org.idea.net;

import java.nio.ByteBuffer;

public class TunTap {

	static native int open(String dev);
	
	static native void close(int fd);
	
	static native int write(int fd, ByteBuffer d, int len);
	
	static native boolean await(int fd, long timeout);
	
	static native int read(int fd, ByteBuffer d);

}
