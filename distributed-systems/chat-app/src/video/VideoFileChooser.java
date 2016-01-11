package video;

import java.io.*;
import javax.swing.*;
import javax.swing.filechooser.FileFilter;

public class VideoFileChooser extends JFrame {
	
	private static final long serialVersionUID = -315195984033206096L;
	private File file = null;

	public VideoFileChooser() {
		super("File Chooser Test Frame");

		JFileChooser chooser = new JFileChooser("./");
		chooser.setAcceptAllFileFilterUsed(false);
		chooser.addChoosableFileFilter(new FileFilter() {
			 
			String extensions[] = {"3gp", "3g2", "aac", "asf", "dvd", "dxa", "ffm", "flac", "flv", "flic", "mpg",
					"h261", "h263", "h264", "gif", "image2", "image2pipe", "ipod", "m4v", "matroska", "webm",
					"md5", "mjpeg", "mlp", "mov", "mp4", "m4a", "mj2", "mp2", "mp3", "mpeg", "mpjpeg", "mvi", 
					"mxf", "wav", "xwma", "mkv", "avi"};
			
		    public String getDescription() {
		        return "Video Files";
		    }
		 
		    public boolean accept(File f) {
		    	if (f.isDirectory())
		    		return true;
		    	
		    	for (String extension : extensions) {
		    		if (f.getName().toLowerCase().endsWith("." + extension))
		    			return true;
		    	}
		    	return false;
		    }
		});
		
		int option = chooser.showOpenDialog(VideoFileChooser.this);
		if (option == JFileChooser.APPROVE_OPTION) {
			this.file = chooser.getSelectedFile();
		}
		else {
			System.out.println("You cancelled.");
		}
	}
	
	public File getFile() {
		return file;
	}

	public void setFile(File file) {
		this.file = file;
	}
}
