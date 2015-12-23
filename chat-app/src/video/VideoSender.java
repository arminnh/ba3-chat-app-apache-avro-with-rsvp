package video;

import java.io.File;
import java.io.IOException;
import javax.swing.JFrame;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.Queue;
import java.io.ByteArrayOutputStream;

import com.xuggle.mediatool.IMediaReader;
import com.xuggle.mediatool.MediaListenerAdapter;
import com.xuggle.mediatool.ToolFactory;
import com.xuggle.mediatool.event.IVideoPictureEvent;
import com.xuggle.xuggler.Global;
import com.xuggle.xuggler.ICodec;
import com.xuggle.xuggler.IContainer;
import com.xuggle.xuggler.IStream;

import client.AppClientInterface;

/**
 * Using IMediaReader, takes a media container, finds the first video  stream, decodes that stream, 
 * and then writes video frames out to a PNG image file, based on the video presentation timestamps.
 */
public class VideoSender extends MediaListenerAdapter implements Runnable {
	private long MICRO_SECONDS_PER_FRAME = -1;
    
    // The video stream index, used to ensure we display frames from one and only one video stream from the media container.
    private int videoStreamIndex = -1;

    private JFrame frame;
    private Graphics g;
    private AppClientInterface proxy;
    private IMediaReader reader = null;
    private Queue<BufferedImage> imgBuffer = new LinkedList<BufferedImage>();
    private boolean videoDone;
    
    //====================================================================================================================

    public VideoSender(File input, JFrame frame, AppClientInterface proxy) {
        this.frame = frame;
        this.proxy = proxy;
        this.g = this.frame.getGraphics();
        
        // create a media reader for processing video
        this.reader = ToolFactory.makeReader(input.getAbsolutePath());

        // stipulate that we want BufferedImages created in BGR 24bit color space
        this.reader.setBufferedImageTypeToGenerate(BufferedImage.TYPE_3BYTE_BGR);

        // note that DecodeAndCaptureFrames is derived from MediaReader.ListenerAdapter 
        // and thus may be added as a listener to the MediaReader. DecodeAndCaptureFrames implements onVideoPicture().
        this.reader.addListener(this);
        
        IContainer container = IContainer.make();
        if (container.open(input.getAbsolutePath(), IContainer.Type.READ, null) < 0)
        	throw new IllegalArgumentException("could not open file: " + input);
        
		for (int i = 0; i < container.getNumStreams(); i++) {
			// Find the stream object
			System.out.println(i);
			IStream stream = container.getStream(i);
			
			// Get the pre-configured decoder that can decode this stream;
			if (stream.getStreamCoder().getCodecType() == ICodec.Type.CODEC_TYPE_VIDEO) {
				videoStreamIndex = i;
				break;
			}
		}
		if (videoStreamIndex == -1)
			throw new RuntimeException("could not find video stream in container: " + input);
        
        double fps = container.getStream(videoStreamIndex).getFrameRate().getDouble();
        MICRO_SECONDS_PER_FRAME = (long) (1000000.0 / fps);
        System.out.println("FPS: " + fps + ", microseconds per frame: " + MICRO_SECONDS_PER_FRAME);
    }
    
    public void send() {
        // read out the contents of the media file, note that nothing else happens here. 
        // action happens in the onVideoPicture() method which is called when complete video pictures are extracted from the media source
        while (this.reader.readPacket() == null && frame.isVisible()) {
            do { } while (false);
        }
        videoDone = true;
        /*System.out.println("Seconds between frames: " + SECONDS_BETWEEN_FRAMES);
        System.out.println("Microseconds between frames: " + MICRO_SECONDS_BETWEEN_FRAMES);
        System.out.println("Time of last frame write: " + mLastPtsWrite);
        System.out.println("DEFAULT_PTS_PER_SECOND: " + Global.DEFAULT_PTS_PER_SECOND);
        System.out.println("Video stream index: " + mVideoStreamIndex);*/
    }
    

    /**
     * Called after a video frame has been decoded from a media stream. Optionally a BufferedImage version of the frame may be passed
     * if the calling IMediaReader instance was configured to create BufferedImages. This method blocks, so return quickly.
     */
    public void onVideoPicture(IVideoPictureEvent event) {
        if (event.getStreamIndex() != videoStreamIndex) {
            return;
        }
    	imgBuffer.add(event.getImage());
    }
    
    public void sendVideo() {
    	long lastFrameWrite = System.nanoTime()/1000 - MICRO_SECONDS_PER_FRAME;
    	long time_passed = 0;
    	
    	try {
    		while (!videoDone || !imgBuffer.isEmpty()) {
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
			frame.setVisible(false);
        }
    }

	@Override
	public void run() {
		this.send();

		frame.setVisible(false);
		try {
			proxy.setFrameVisible(false);
		} catch (IOException e) {
			System.err.println("The other user has disconnected, stopping video.");
		}
	}
}
