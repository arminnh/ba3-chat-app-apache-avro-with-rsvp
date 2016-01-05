package video;

import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.LinkedBlockingQueue;

import javax.imageio.ImageIO;
import javax.swing.JFrame;

import client.AppClientInterface;

public class VideoSender implements Runnable {
	public JFrame frame;
	public Graphics g;
	public AppClientInterface proxy;
	
	public File video;
	public boolean videoDecoded = false;
	//use nanoseconds so we can compare easier with system.nanoTime()
	public long NANOSECONDS_PER_FRAME = -1;
	public LinkedBlockingQueue<BufferedImage> imgBuffer = new LinkedBlockingQueue<BufferedImage>();
	
	
    public VideoSender(File input, JFrame frame, AppClientInterface proxy) {
    	this.video = input;
        this.frame = frame;
        this.proxy = proxy;
        this.g = this.frame.getGraphics();
	}

	@Override
	public void run() {
		Thread videoDecoder = new Thread(new VideoDecoder(this));
		videoDecoder.start();
    	long lastFrameWrite = System.nanoTime() - NANOSECONDS_PER_FRAME;
    	long timeNow = 0;
    	System.out.println("NANO_SECONDS_PER_FRAME: " + this.NANOSECONDS_PER_FRAME);
    	
    	try {
    		while ((!videoDecoded || !imgBuffer.isEmpty()) && frame.isVisible()) {
    			timeNow = System.nanoTime();
    			if (timeNow - lastFrameWrite >= NANOSECONDS_PER_FRAME) {
    				lastFrameWrite = timeNow;
    				BufferedImage img = imgBuffer.take();
    				g.drawImage(img, 0, 0, frame.getWidth(), frame.getHeight(), null);

    				ByteArrayOutputStream bos = new ByteArrayOutputStream();
    				ImageIO.write(img,  "jpg",  bos);
    				proxy.receiveImage(ByteBuffer.wrap(bos.toByteArray()));
    			}
    		}
    	} catch (IOException | InterruptedException e) {
			// other user has disconnected, stop sending video
        }

    	imgBuffer.clear();
		frame.setVisible(false);
		try {
			proxy.setFrameVisible(true, false);
		} catch (IOException e) {
			System.err.println("The other user has disconnected, stopping video.");
		}
	}
}
