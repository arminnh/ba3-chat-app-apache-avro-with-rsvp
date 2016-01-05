package video;

import java.awt.image.BufferedImage;
import com.xuggle.mediatool.IMediaReader;
import com.xuggle.mediatool.MediaListenerAdapter;
import com.xuggle.mediatool.ToolFactory;
import com.xuggle.mediatool.event.IVideoPictureEvent;
import com.xuggle.xuggler.ICodec;
import com.xuggle.xuggler.IContainer;
import com.xuggle.xuggler.IStream;

/**
 * Using IMediaReader, takes a media container, finds the first video  stream, decodes that stream, 
 * and then writes video frames out to a PNG image file, based on the video presentation timestamps.
 */
public class VideoDecoder extends MediaListenerAdapter implements Runnable {
    // The video stream index, used to ensure we display frames from one and only one video stream from the media container.
    private int videoStreamIndex = -1;
    private IMediaReader reader = null;
    private VideoSender s;
    private double fps;

    public VideoDecoder(VideoSender videoSender) {
    	this.s = videoSender;
    	
        // create a media reader for processing video
        this.reader = ToolFactory.makeReader(s.video.getAbsolutePath());

        // stipulate that we want BufferedImages created in BGR 24bit color space
        this.reader.setBufferedImageTypeToGenerate(BufferedImage.TYPE_3BYTE_BGR);

        // note that DecodeAndCaptureFrames is derived from MediaReader.ListenerAdapter 
        // and thus may be added as a listener to the MediaReader. DecodeAndCaptureFrames implements onVideoPicture().
        this.reader.addListener(this);
        
        IContainer container = IContainer.make();
		if (container.open(s.video.getAbsolutePath(), IContainer.Type.READ, null) < 0)
        	throw new IllegalArgumentException("could not open file: " + s.video);
		
        this.initVideoStreamIndex(container);
        /*
         * 30fps
         * 1/30 = seconds for 1 frame
         * 1000*1/30 = ms for 1 frame
         * 1000000000*1/30 = ns for 1 frame
         */
        fps = container.getStream(videoStreamIndex).getFrameRate().getDouble();
        s.NANOSECONDS_PER_FRAME = (long) (1/fps * 1000000000);
        System.out.println("FPS: " + fps);
    }

	private void initVideoStreamIndex(IContainer container) {
		for (int i = 0; i < container.getNumStreams(); i++) {
			// Find the stream object
			IStream stream = container.getStream(i);
			
			// Get the pre-configured decoder that can decode this stream;
			if (stream.getStreamCoder().getCodecType() == ICodec.Type.CODEC_TYPE_VIDEO) {
				videoStreamIndex = i;
				break;
			}
		}
		
		if (videoStreamIndex == -1)
			throw new RuntimeException("could not find video stream in container: " + s.video);
	}
    
	@Override
	public void run() {
        // read out the contents of the media file, note that nothing else happens here. 
        // action happens in the onVideoPicture() method which is called when complete video pictures are extracted from the media source
        while (this.reader.readPacket() == null && s.frame.isVisible()) {
            do { } while (false);
        }
        //System.out.println("VIDEO DECODED");
        
    	s.videoDecoded = true;
	}
    
    /**
     * Called after a video frame has been decoded from a media stream. Optionally a BufferedImage version of the frame may be passed
     * if the calling IMediaReader instance was configured to create BufferedImages. This method blocks, so return quickly.
     */
    public void onVideoPicture(IVideoPictureEvent event) {
        if (event.getStreamIndex() != videoStreamIndex) {
            return;
        }

        /*
         * s.imgBuffer.put should wait in case the queue is full, 
         * but it doesn't and throws an out of memory exception
         * so we make the thread sleep manually
         */
        while (s.imgBuffer.size() > 10*this.fps) {
        	//TODO: find the right sleep time
        	/*try {
				Thread.sleep(5*s.NANOSECONDS_PER_FRAME);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}*/
        }
        
        try {
			s.imgBuffer.put(event.getImage());
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
    }
}
