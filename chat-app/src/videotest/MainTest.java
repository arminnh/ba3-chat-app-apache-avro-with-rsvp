package videotest;

import java.io.File;
import java.io.IOException;
import java.awt.*;
import java.awt.image.BufferedImage;

import javax.swing.JFrame;
import javax.swing.JPanel;

public class MainTest extends JPanel {

	public BufferedImage img = null;
	public Graphics g = null;
	public File a = null, b = null;

	public static void main(String[] args) throws IOException {
		System.out.println("Working Directory = " + System.getProperty("user.dir"));
		MainTest test = new MainTest();

//		test.a = new File("SampleVideo_1080x720_30mb.flv");
//		test.a = new File("SampleVideo_1080x720_50mb.mp4");
		test.a = new File("SampleVideo_1080x720_20mb.mkv");
//		test.a = new File("small.flv");
		test.b = new File("myFile.png");

		JFrame frame = new JFrame();
		frame.getContentPane().add(test);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setSize(800, 600);
		frame.setVisible(true);
		
		new FrameGrabber(test.a, test.b, frame);
	}

	/*public void paint(Graphics g)  {
		System.out.println("paint");
		try {
			img = ImageIO.read(this.b);
		    g.drawString("www.java2s.com", 20,20);
			g.drawImage(img, 0, 0, 800, 600, this);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}*/
}
