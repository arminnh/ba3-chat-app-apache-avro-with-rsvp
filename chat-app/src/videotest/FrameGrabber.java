package videotest;

import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;
import java.awt.Graphics;
import java.awt.Image;
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
import javax.swing.JPanel;

/**
 * Using {@link IMediaReader}, takes a media container, finds the first video 
 * stream, decodes that stream, and then writes video frames out to a PNG image 
 * file, based on the video presentation timestamps.
 *
 * @author aclarke
 * @author trebor
 */
public class FrameGrabber extends MediaListenerAdapter {

    private int CAPTURED_FRAMES = 0;        // count of the number of frames captures
    private int MAX_CAPTURED_FRAMES = 10000;    // maximum number of frames to capture
    
    // The number of seconds between frames.
    public static final double SECONDS_BETWEEN_FRAMES = 1/25;

    // The number of micro-seconds between frames.
    public static long MICRO_SECONDS_BETWEEN_FRAMES = (long) (Global.DEFAULT_PTS_PER_SECOND * SECONDS_BETWEEN_FRAMES);

    // Time of last frame write.
    private static long mLastPtsWrite = Global.NO_PTS;
    
    // The video stream index, used to ensure we display frames from one and only one video stream from the media container.
    private int mVideoStreamIndex = -1;

    private File input;
    private File output;
    
    private static final Logger logger = Logger.getLogger(FrameGrabber.class.getName());

    private JFrame frame;
    private Graphics g;
    
    private IMediaReader reader = null;
    
    private boolean isSet = false;
    //--------------------------------------------------------------------------
    
    /** 
     * FrameGrabber constructor.
     * @param Input Absolute path to input file
     * @param Output Absolute path to output file
     */
    public FrameGrabber(File Input, File Output, JFrame frame) {
        input = Input;
        output = Output;
        
        this.frame = frame;
        this.g = frame.getGraphics();
        
        // create a media reader for processing video
        reader = ToolFactory.makeReader(Input.getAbsolutePath());
//        MAX_CAPTURED_FRAMES = reader.

        // stipulate that we want BufferedImages created in BGR 24bit color space
        reader.setBufferedImageTypeToGenerate(BufferedImage.TYPE_3BYTE_BGR);

        // note that DecodeAndCaptureFrames is derived from MediaReader.ListenerAdapter and thus may be added as a listener
        // to the MediaReader. DecodeAndCaptureFrames implements onVideoPicture().
        reader.addListener(this);

        System.out.println("frames to capture: " + MAX_CAPTURED_FRAMES);
        System.out.println("Seconds between frames: " + SECONDS_BETWEEN_FRAMES);
        System.out.println("Microseconds between frames: " + MICRO_SECONDS_BETWEEN_FRAMES);
        System.out.println("Time of last frame write: " + mLastPtsWrite);
        System.out.println("DEFAULT_PTS_PER_SECOND: " + Global.DEFAULT_PTS_PER_SECOND);
        System.out.println("Video stream index: " + mVideoStreamIndex);
        
        // read out the contents of the media file, note that nothing else happens here.  
        // action happens in the onVideoPicture() method which is called when complete 
        // video pictures are extracted from the media source
//        while (reader.readPacket() == null && CAPTURED_FRAMES < MAX_CAPTURED_FRAMES) {
        while (reader.readPacket() == null && CAPTURED_FRAMES < MAX_CAPTURED_FRAMES) {
            do {
            } while (false);
        }

        System.out.println("frames to capture: " + MAX_CAPTURED_FRAMES);
        System.out.println("Seconds between frames: " + SECONDS_BETWEEN_FRAMES);
        System.out.println("Microseconds between frames: " + MICRO_SECONDS_BETWEEN_FRAMES);
        System.out.println("Time of last frame write: " + mLastPtsWrite);
        System.out.println("DEFAULT_PTS_PER_SECOND: " + Global.DEFAULT_PTS_PER_SECOND);
        System.out.println("Video stream index: " + mVideoStreamIndex);
    }
    
    //--------------------------------------------------------------------------

    /**
     * Called after a video frame has been decoded from a media stream.
     * Optionally a BufferedImage version of the frame may be passed
     * if the calling {@link IMediaReader} instance was configured to
     * create BufferedImages. This method blocks, so return quickly.
     */
    public void onVideoPicture(IVideoPictureEvent event) {
    	System.out.println("Frame nr: " + CAPTURED_FRAMES);
        try {
            // if the stream index does not match the selected stream index, then have a closer look
            if (event.getStreamIndex() != mVideoStreamIndex) {
                // if the selected video stream id is not yet set, go ahead an select this lucky video stream
                if (-1 == mVideoStreamIndex) {
                    mVideoStreamIndex = event.getStreamIndex();
                } // otherwise return, no need to show frames from this video stream
                else {
                    return;
                }
            }
            
            if (!isSet) {
                double fps = reader.getContainer().getStream(mVideoStreamIndex).getFrameRate().getDouble();
                System.out.println(MICRO_SECONDS_BETWEEN_FRAMES);
                MICRO_SECONDS_BETWEEN_FRAMES = (long) (Global.DEFAULT_PTS_PER_SECOND * 1/fps);
                System.out.println(MICRO_SECONDS_BETWEEN_FRAMES);
                isSet = true;
            }
            
            // if uninitialized, backdate mLastPtsWrite so we get the very first frame
            if (mLastPtsWrite == Global.NO_PTS) {
                mLastPtsWrite = event.getTimeStamp() - MICRO_SECONDS_BETWEEN_FRAMES;
            }
//            while () {
//            	Thread.sleep();
//            }
            // if it's time to write the next frame
            if (event.getTimeStamp() - mLastPtsWrite >= MICRO_SECONDS_BETWEEN_FRAMES) {
                // write out PNG
        		RenderedImage im = event.getImage();
//                ImageIO.write(im, "png", output);
//                ImageIO.write(event.getImage(), "png", img);
                
                // indicate file written
//                double seconds = ((double) event.getTimeStamp()) / Global.DEFAULT_PTS_PER_SECOND;
                
                // update last write time
                mLastPtsWrite += MICRO_SECONDS_BETWEEN_FRAMES;
                CAPTURED_FRAMES++;
//                System.out.println("Wrote frame to file");
                
                BufferedImage img = (BufferedImage) im;
				g.drawImage(img, 0, 0, 800, 600, null);
            }
            
        } catch (Exception ex) {
            //String stack = ExceptionUtils.getStackTrace(ex);
        	String stack = ex.getLocalizedMessage();
            logger.log(Level.SEVERE, "Could not write poster image for video file {0}\n\n{1}", new Object[]{input.getAbsolutePath(),stack});
        }
    }
} // end class