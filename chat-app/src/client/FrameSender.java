package client;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;

import javax.imageio.ImageIO;

public class FrameSender implements Runnable {
	
	BufferedImage img;
	AppClientInterface proxy;
	
	FrameSender(BufferedImage img, AppClientInterface proxy) {
		this.img = img;
		this.proxy = proxy;
	}
	
	public void run() {
		try {
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
	    	ImageIO.write(this.img,  "jpg",  bos);
			this.proxy.receiveImage(ByteBuffer.wrap(bos.toByteArray()));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
