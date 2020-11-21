package org.idea;

import java.io.DataInputStream;
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
	
	static byte[] readPdu(DataInputStream dis) throws IOException {
		int s;
		for (;;) {		
			s = dis.read();
			if (s > 0) {
				break;
			}
		}
		
		byte[] bytes = new byte[s];
		dis.readFully(bytes);
		
		return bytes;
	}
	
	static void writePdu(OutputStream os, byte[] bytes, int len) throws IOException {
		os.write(len);
		os.write(bytes, 0, len);
		os.flush();
	}
	
	static void fromSerialToTap(InputStream is, OutputStream os) throws IOException {
		DataInputStream dis = new DataInputStream(is);
		
		for (;;) {
			byte[] bytes = readPdu(dis);			
			os.write(bytes);
			
			dump(bytes, bytes.length);
		}
	}
	
	static void fromTapToSerial(InputStream is, OutputStream os) throws IOException {
		byte[] bytes = new byte[4096];
		int s;
		while ((s = is.read(bytes)) >= 0) {
			writePdu(os, bytes, s);
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
