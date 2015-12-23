package video;

import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.Queue;

import javax.imageio.ImageIO;
import javax.swing.JFrame;

import client.AppClientInterface;

public class VideoSender implements Runnable {
	public JFrame frame;
	public Graphics g;
	public AppClientInterface proxy;
	
	public File video;
	public boolean videoDecoded;
	public long MICRO_SECONDS_PER_FRAME = -1;
	public Queue<BufferedImage> imgBuffer = new LinkedList<BufferedImage>();
	
	
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
    	long lastFrameWrite = System.nanoTime()/1000 - MICRO_SECONDS_PER_FRAME;
    	long time_passed = 0;
    	
    	try {
    		while ((!videoDecoded || !imgBuffer.isEmpty()) && frame.isVisible()) {
    			time_passed = System.nanoTime()/1000 - lastFrameWrite;
    			if (!imgBuffer.isEmpty() && time_passed >= MICRO_SECONDS_PER_FRAME) {
    				lastFrameWrite = System.nanoTime()/1000;
    				BufferedImage img = imgBuffer.remove();
    				g.drawImage(img, 0, 0, frame.getWidth(), frame.getHeight(), null);

    				ByteArrayOutputStream bos = new ByteArrayOutputStream();
    				ImageIO.write(img,  "jpg",  bos);
    				proxy.receiveImage(ByteBuffer.wrap(bos.toByteArray()));
    			}
    		}
    	} catch (IOException e) {
			// other user has disconnected, stop sending video
        }

		frame.setVisible(false);
		try {
			proxy.setFrameVisible(false);
		} catch (IOException e) {
			System.err.println("The other user has disconnected, stopping video.");
		}
	}
}
