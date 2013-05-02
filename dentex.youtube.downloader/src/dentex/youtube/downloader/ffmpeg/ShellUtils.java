package dentex.youtube.downloader.ffmpeg;

/*  code adapted from: https://github.com/guardianproject/android-ffmpeg-java
 *  Copyright (c) 2009, Nathan Freitas, Orbot / The Guardian Project - http://openideals.com/guardian
 *  See LICENSE for licensing information (GPL-3.0)
 */

public class ShellUtils {

	public interface ShellCallback {
		public void shellOut (String shellLine);
		public void processComplete (int exitValue);
		public void processNotStartedCheck (boolean started);
	}
}
