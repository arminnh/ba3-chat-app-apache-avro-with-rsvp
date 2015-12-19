package videotest;

import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;

import com.xuggle.mediatool.IMediaReader;
import com.xuggle.mediatool.MediaListenerAdapter;
import com.xuggle.mediatool.ToolFactory;
import com.xuggle.mediatool.event.IVideoPictureEvent;
import com.xuggle.xuggler.Global;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JFrame;

/**
 * Using IMediaReader, takes a media container, finds the first video  stream, decodes that stream, 
 * and then writes video frames out to a PNG image file, based on the video presentation timestamps.
 */
public class FrameGrabber extends MediaListenerAdapter {
    private int CAPTURED_FRAMES = 0; // count of the number of frames captures
    
    // The number of seconds between frames.
    public static double SECONDS_BETWEEN_FRAMES = 1/25;

    // The number of micro-seconds between frames.
    public static long MICRO_SECONDS_BETWEEN_FRAMES = (long) (Global.DEFAULT_PTS_PER_SECOND * SECONDS_BETWEEN_FRAMES);

    // Time of last frame write. NO_PTS means no time stamp is set
    private static long mLastPtsWrite = Global.NO_PTS;
    
    // The video stream index, used to ensure we display frames from one and only one video stream from the media container.
    private int mVideoStreamIndex = -1;
    private boolean isSet = false;

    private static final Logger logger = Logger.getLogger(FrameGrabber.class.getName());

    private File input;
    private JFrame frame;
    private Graphics g;
    private IMediaReader reader = null;
    
    //====================================================================================================================
    
    public FrameGrabber(File Input, JFrame frame) {
    	this.input = Input;
        
        this.frame = frame;
        this.g = frame.getGraphics();
        
        // create a media reader for processing video
        this.reader = ToolFactory.makeReader(Input.getAbsolutePath());

        // stipulate that we want BufferedImages created in BGR 24bit color space
        this.reader.setBufferedImageTypeToGenerate(BufferedImage.TYPE_3BYTE_BGR);

        // note that DecodeAndCaptureFrames is derived from MediaReader.ListenerAdapter 
        // and thus may be added as a listener to the MediaReader. DecodeAndCaptureFrames implements onVideoPicture().
        this.reader.addListener(this);
        
        // read out the contents of the media file, note that nothing else happens here. 
        // action happens in the onVideoPicture() method which is called when complete video pictures are extracted from the media source
        while (this.reader.readPacket() == null) {
            do { } while (false);
        }

        frame.dispatchEvent(new WindowEvent(frame, WindowEvent.WINDOW_CLOSING));
        
        System.out.println("Seconds between frames: " + SECONDS_BETWEEN_FRAMES);
        System.out.println("Microseconds between frames: " + MICRO_SECONDS_BETWEEN_FRAMES);
        System.out.println("Time of last frame write: " + mLastPtsWrite);
        System.out.println("DEFAULT_PTS_PER_SECOND: " + Global.DEFAULT_PTS_PER_SECOND);
        System.out.println("Video stream index: " + mVideoStreamIndex);
    }
    

    /**
     * Called after a video frame has been decoded from a media stream. Optionally a BufferedImage version of the frame may be passed
     * if the calling IMediaReader instance was configured to create BufferedImages. This method blocks, so return quickly.
     */
    public void onVideoPicture(IVideoPictureEvent event) {
    	System.out.println("Frame nr: " + CAPTURED_FRAMES);
    	
    	try {
    		Thread.sleep((long) reader.getContainer().getStream(mVideoStreamIndex).getFrameRate().getValue());
    	} catch (Exception e) {
    		//e.printStackTrace();
    	}
    	
        try {
            // if the stream index does not match the selected stream index, then have a closer look
            if (event.getStreamIndex() != mVideoStreamIndex) {
                // if the selected video stream id is not yet set, go ahead an select this lucky video stream
                if (-1 == mVideoStreamIndex) {
                    mVideoStreamIndex = event.getStreamIndex();
                } 
                // otherwise return, no need to show frames from this video stream
                else {
                    return;
                }
            }
            
            /*if (!isSet) {
                double fps = reader.getContainer().getStream(mVideoStreamIndex).getFrameRate().getDouble();
                System.out.println(MICRO_SECONDS_BETWEEN_FRAMES);
                MICRO_SECONDS_BETWEEN_FRAMES = (long) (Global.DEFAULT_PTS_PER_SECOND * 1/fps);
                System.out.println(MICRO_SECONDS_BETWEEN_FRAMES);
                isSet = true;
            }*/

            // if uninitialized, backdate mLastPtsWrite so we get the very first frame
            if (mLastPtsWrite == Global.NO_PTS) {
                mLastPtsWrite = event.getTimeStamp() - MICRO_SECONDS_BETWEEN_FRAMES;
            }

            // if it's time to write the next frame
            if (event.getTimeStamp() - mLastPtsWrite >= MICRO_SECONDS_BETWEEN_FRAMES) {
            	//write out PNG
            	//ImageIO.write(event.getImage(), "png", img);

            	// update last write time
            	mLastPtsWrite += MICRO_SECONDS_BETWEEN_FRAMES;
            	CAPTURED_FRAMES++;

            	BufferedImage img = event.getImage();
            	g.drawImage(img, 0, 0, frame.getWidth(), frame.getHeight(), null);
            }
            
        } catch (Exception ex) {
            //String stack = ExceptionUtils.getStackTrace(ex);
        	String stack = ex.getLocalizedMessage();
            System.err.println(Level.SEVERE.toString() + "Could not write poster image for video file {0}\n\n{1} " + new Object[]{input.getAbsolutePath(),stack});
        }
    }
}
