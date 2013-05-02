package dentex.youtube.downloader.utils;

import java.io.File;

import android.os.FileObserver;
import dentex.youtube.downloader.SettingsActivity;
import dentex.youtube.downloader.ShareActivity;
import dentex.youtube.downloader.service.DownloadsService;
import dentex.youtube.downloader.service.FfmpegDownloadService;

// reference:
// https://gist.github.com/shirou/659180
// https://github.com/shirou
	
public class Observer {
	
	static final String DEBUG_TAG = "Observer";
	public static String observedPath;

	public static class YtdFileObserver extends FileObserver {
	    static final String TAG="FileObserver: ";
	
		static final int mask = (FileObserver.CREATE | FileObserver.DELETE); 
		
		public YtdFileObserver(String root){
			super(root, mask);
			observedPath = root;
			if (! root.endsWith(File.separator)){
				root += File.separator;
			}
		}
	
		public void onEvent(int event, String path) {
			//Utils.logger("d", TAG + "onEvent " + event + ", " + path, DEBUG_TAG);
			
			if (event == FileObserver.CREATE) {
				Utils.logger("d", TAG + "file " + path + " CREATED", DEBUG_TAG);
				
				if (observedPath.equals(FfmpegDownloadService.DIR)) {
					SettingsActivity.SettingsFragment.touchAudioExtrPref(false, false);
				} else {
					ShareActivity.NotificationHelper();
				}
			}
			
			if (event == FileObserver.DELETE) {
				Utils.logger("d", TAG + "file " + path + " DELETED", DEBUG_TAG);

				if (observedPath.equals(FfmpegDownloadService.DIR)) {
					SettingsActivity.SettingsFragment.touchAudioExtrPref(true, false);
				} else {
					long id = Utils.settings.getLong(path, 0);
					Utils.logger("d", TAG + "Retrieved ID: " +  id, DEBUG_TAG);
					DownloadsService.removeIdUpdateNotification(id);
				}
			}
		}
	
		public void close(){
			super.finalize();
		}
	}
}
