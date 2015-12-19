package videotest;

import java.io.File;
import java.io.IOException;

import javax.swing.JFrame;
import javax.swing.JPanel;

public class MainTest extends JPanel {

	public File input = null;
	public File output = null;
	private static final long serialVersionUID = 1L;

    //====================================================================================================================
	
	public static void main(String[] args) throws IOException {
		System.out.println("Working Directory = " + System.getProperty("user.dir"));
		MainTest test = new MainTest();

//		test.input = new File("SampleVideo_1080x720_30mb.flv");
//		test.input = new File("SampleVideo_1080x720_50mb.mp4");
//		test.input = new File("SampleVideo_1080x720_20mb.mkv");
		test.input = new File("small.flv");
		test.output = new File("myFile.png");

		JFrame frame = new JFrame();
		frame.getContentPane().add(test);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setSize(800, 600);
		frame.setVisible(true);
		
		new FrameGrabber(test.input, frame);
	}
}
