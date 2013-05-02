package dentex.youtube.downloader.ffmpeg;

/*  code adapted from: https://github.com/guardianproject/android-ffmpeg-java
 *  Copyright (c) 2009, Nathan Freitas, Orbot / The Guardian Project - http://openideals.com/guardian
 *  See LICENSE for licensing information (GPL-3.0)
 */

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import android.content.Context;
import android.util.Log;
import dentex.youtube.downloader.utils.Utils;

public class FfmpegController {

	private final static String DEBUG_TAG = "FfmpegController";
	public static final String ffmpegBinName = "ffmpeg";
	
	public File mBinFileDir;
	public String mFfmpegBinPath;
	private Context mContext;

	public FfmpegController(Context context) throws FileNotFoundException, IOException {
		mContext = context;
		
		mBinFileDir = context.getDir("bin", 0);
		mFfmpegBinPath = new File(mBinFileDir, ffmpegBinName).getAbsolutePath();
	}
	
	public void execFFMPEG (List<String> cmd, ShellUtils.ShellCallback sc) {
		execChmod(mFfmpegBinPath, "755");
		execProcess(cmd, sc);
	}
	
	public  void execChmod(String filepath, String code) {
		Utils.logger("d", "Trying to chmod '" + filepath + "' to: " + code, DEBUG_TAG);
		try {
			Runtime.getRuntime().exec("chmod " + code + " " + filepath);
		} catch (IOException e) {
			Log.e(DEBUG_TAG, "Error changing file permissions!", e);
		}
	}
	
	public  int execProcess(List<String> cmds, ShellUtils.ShellCallback sc) {		
		StringBuilder cmdlog = new StringBuilder();
		for (String cmd : cmds) {
			cmdlog.append(cmd);
			cmdlog.append(' ');
		}
		Utils.logger("v", cmdlog.toString(), DEBUG_TAG);
		
		ProcessBuilder pb = new ProcessBuilder(/*"liblame.so"*/);
		
		Map<String, String> envMap = pb.environment();
		envMap.put("LD_LIBRARY_PATH", mContext.getApplicationInfo().nativeLibraryDir);

		pb.directory(mBinFileDir);
		pb.command(cmds);

    	Process process = null;
    	int exitVal = 1; // Default error
    	boolean started = true;
    	try {
    		
    		process = pb.start();    
    	
    		// any error message?
    		StreamGobbler errorGobbler = new 
    				StreamGobbler(process.getErrorStream(), "ERROR", sc);            
        
    		// any output?
    		StreamGobbler outputGobbler = new 
    				StreamGobbler(process.getInputStream(), "OUTPUT", sc);
            
    		// kick them off
    		errorGobbler.start();
    		outputGobbler.start();
     
    		exitVal = process.waitFor();
        
    		sc.processComplete(exitVal);
    		
    	} catch (Exception e) {
    		Log.e(DEBUG_TAG, "Error executing ffmpeg command! - " + e.getCause());
    		started = false;
    	} finally {
    		if (process != null) {
    			Utils.logger("w", "destroyng process", DEBUG_TAG);
    			process.destroy();
    		}
    		sc.processNotStartedCheck(started);
    	}
        return exitVal;
	}
	
	public void extractAudio (File videoIn, File audioOut, String type, String mp3BitRate, 
			ShellUtils.ShellCallback sc) throws IOException, InterruptedException {
		
		List<String> cmd = new ArrayList<String>();

		cmd.add(mFfmpegBinPath);
		cmd.add("-y");
		cmd.add("-i");
		cmd.add(videoIn.getAbsolutePath());
		cmd.add("-vn");
		cmd.add("-acodec");
		
		if (type.equals("conv")) {
			cmd.add("libmp3lame"); 
			cmd.add("-ab"); 
			cmd.add(mp3BitRate);
		} else {
			cmd.add("copy");
		}
		
		cmd.add(audioOut.getAbsolutePath());

		execFFMPEG(cmd, sc);
	}
	
	class StreamGobbler extends Thread {
	    InputStream is;
	    String type;
	    ShellUtils.ShellCallback sc;
	    
	    StreamGobbler(InputStream is, String type, ShellUtils.ShellCallback sc) {
	        this.is = is;
	        this.type = type;
	        this.sc = sc;
		}
	    
	    public void run() {
	    	try {
	    		InputStreamReader isr = new InputStreamReader(is);
	            BufferedReader br = new BufferedReader(isr);
	            String line = null;
	            while ((line = br.readLine()) != null) {
	            	if (sc != null) {
	            		sc.shellOut(line);
	            	}
	            }
	        } catch (IOException ioe) {
	                Log.e(DEBUG_TAG,"error reading shell log", ioe);
	        }
	    }
	}
}