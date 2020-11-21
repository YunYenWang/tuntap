package org.idea.net;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.StringTokenizer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TunTapTest {

	@BeforeEach
	void init() {
		System.loadLibrary("tuntap");
	}
	
	@Test
	void test() {
		int fd = TunTap.open("tap0");
		log.info("fd: {}", fd);
		
		ByteBuffer bb = ByteBuffer.allocateDirect(1500);

		long timeout = 1000L;		
		for (;;) {
			boolean r = TunTap.await(fd, timeout);
			log.info("await: {}", r);
			
			if (r) {			
				int s = TunTap.read(fd, bb);				
				log.info("bytes: {}", s);
				
				bb.limit(s);
				
				for (int i = 0;i < s;i++) {
					System.out.printf("%02X ", bb.get(i) & 0x0FF);
				}
				
//				TunTap.write(fd, bb, bb.remaining());
			}
			
			System.out.println();
		}
		
//		TunTap.close(fd);
	}

	byte[] toBytes(String s) {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		
		StringTokenizer st = new StringTokenizer(s);
		while (st.hasMoreTokens()) {
			baos.write(Integer.parseInt(st.nextToken(), 16));
		}
		
		return baos.toByteArray();
	}
	
	@Test
	void testWrite() {
		String p = "FF FF FF FF FF FF 02 0E 90 FA 15 5B 08 06 00 01 08 00 06 04 00 01 02 0E 90 FA 15 5B 0A 00 00 01 00 00 00 00 00 00 0A 00 00 02";
		
		int fd = TunTap.open("tap0");
		
		ByteBuffer bb = ByteBuffer.allocateDirect(1500);
		bb.put(toBytes(p));
		bb.flip();
		
		TunTap.write(fd, bb, bb.remaining());
	}
	
	// ======
	
	@Test
	void testInputStream() throws IOException {
		long timeout = 10000L;
		
		try (TunTap tt = new TunTap("tap0")) {
			tt.setTimeout(timeout);
			
			byte[] b = new byte[1500];
			int s;
			InputStream is = tt.getInputStream();
			while ((s = is.read(b)) >= 0) {
				for (int i = 0;i < s;i++) {
					System.out.printf("%02X ", b[i] & 0x0FF);
				}
				
				System.out.println();
			}
		}		
	}
	
	@Test
	void testOutputStream() throws IOException {
		String p = "FF FF FF FF FF FF 02 0E 90 FA 15 5B 08 06 00 01 08 00 06 04 00 01 02 0E 90 FA 15 5B 0A 00 00 01 00 00 00 00 00 00 0A 00 00 02";
		
		long timeout = 10000L;
		
		try (TunTap tt = new TunTap("tap0")) {
			tt.setTimeout(timeout);
			
			OutputStream os = tt.getOutputStream();
			
			byte[] b = toBytes(p);
			os.write(b);
		}
	}
}
