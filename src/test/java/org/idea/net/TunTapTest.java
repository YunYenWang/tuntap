package org.idea.net;

import java.nio.ByteBuffer;

import org.junit.jupiter.api.Test;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TunTapTest {

	@Test
	void test() {
		System.loadLibrary("tuntap");
		
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
			}
		}
		
//		TunTap.close(fd);
	}
}
