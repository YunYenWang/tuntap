package org.idea;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.idea.net.TunTap;

import gnu.io.CommPortIdentifier;
import gnu.io.SerialPort;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Main {
	
	static void dump(byte[] bytes, int s) {
		for (int i = 0;i < s;i++) {
			System.out.printf("%02X ", bytes[i] & 0x0FF);
		}
		
		System.out.println();
	}
	
	static int checksum(byte[] bytes, int len) {
		int cs = 0;
		
		for (int i = 0;i < len;i++) {
			cs += (bytes[i] & 0x0FF);
		}
		
		return cs & 0x0FF;
	}
	
	static byte[] readPdu(DataInputStream dis) throws IOException {
		for (;;) {
			int c = dis.read();
			if (c < 0) {
				throw new IOException("Serial communication is broken");
			}
			
			if (c == 0x0A) { // seek the head of magic
				break;
			}
		}
		
		if (dis.read() != 0x0B) {
			log.error("Magic head error");
			return null;
		}
		
		int len = dis.readUnsignedShort();
		
		byte[] bytes = new byte[len];
		dis.readFully(bytes);
		
		int cs = dis.read();
		if (cs != checksum(bytes, len)) {
			log.error("Checksum error - len: " + len);			
			return null;
		}
		
		return bytes;
	}
	
	static void writePdu(DataOutputStream dos, byte[] bytes, int len) throws IOException {
		dos.write(0x0A); dos.write(0x0B);
		dos.writeShort((short) len);
		dos.write(bytes, 0, len);
		dos.write(checksum(bytes, len));
		dos.flush();
	}
	
	static void fromSerialToTap(InputStream is, OutputStream os) throws IOException {
		DataInputStream dis = new DataInputStream(new BufferedInputStream(is));
		
		for (;;) {
			byte[] bytes = readPdu(dis);
			if ((bytes == null) || (bytes.length == 0)) {
				continue;
			}
			
			System.out.println("From serial port - " + bytes.length);
//			dump(bytes, bytes.length);
			
			os.write(bytes);
			os.flush();
		}
	}
	
	static void fromTapToSerial(InputStream is, OutputStream os) throws IOException {
		DataOutputStream dos = new DataOutputStream(new BufferedOutputStream(os, 4096));
		
		byte[] bytes = new byte[4096];
		int s;
		while ((s = is.read(bytes)) >= 0) {
			if (s == 0) {
				continue;
			}
			
			System.out.println("From tap0 - " + s);
//			dump(bytes, s);
			
			writePdu(dos, bytes, s);
		}
	}
	
	public static void main(String[] args) throws Exception {
		String dev = "tap0";
		String tty = "/dev/ttyO1";
		int baudrate = 9600;
		int timeout = 10000;
		
		for (int i = 0; i < args.length; i++) {
			String arg = args[i];
							
			if ("-d".equals(arg)) {
				dev = args[++i];
				
			} else if ("-i".equals(arg)) {
				tty = args[++i];
				
			} else if ("-b".equals(arg)) {
				baudrate = Integer.parseInt(args[++i]);
				
			} else {
				System.out.println("usage: tuntap -d tap0 -i /dev/ttyO1 -b 9600");
				System.exit(0);
			}
		}
		
		try (TunTap tap = new TunTap(dev)) {
			tap.setTimeout(timeout);		
			
			SerialPort serial = (SerialPort) CommPortIdentifier.getPortIdentifier(tty).open(tty, timeout);
			try {
				serial.setSerialPortParams(
						baudrate,
						SerialPort.DATABITS_8,
						SerialPort.STOPBITS_1,
						SerialPort.PARITY_NONE);	
				
				new Thread(() -> {
					try {
						fromTapToSerial(tap.getInputStream(), serial.getOutputStream());
						
					} catch (Exception e) {
						log.error("Error", e);
					}
					
				}).start();
				
				
				fromSerialToTap(serial.getInputStream(), tap.getOutputStream());
				
			} finally {
				serial.close();
			}
		}
	}	
}
