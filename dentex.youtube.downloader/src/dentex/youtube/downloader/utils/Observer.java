package dentex.youtube.downloader.utils;

import java.io.File;

import dentex.youtube.downloader.ShareActivity;
import dentex.youtube.downloader.service.DownloadsService;

import android.os.FileObserver;

// reference:
// https://gist.github.com/shirou/659180
// https://github.com/shirou
	
public class Observer {
	
	static final String DEBUG_TAG = "Observer";

	public static class delFileObserver extends FileObserver {
	    static final String TAG="FileObserver: ";
	
		static final int mask = (FileObserver.CREATE | FileObserver.DELETE); 
		
		public delFileObserver(String root){
			super(root, mask);
	
			if (! root.endsWith(File.separator)){
				root += File.separator;
			}
		}
	
		public void onEvent(int event, String path) {
			//Utils.logger("d", TAG + "onEvent " + event + ", " + path);
			
			if (event == FileObserver.CREATE) {
				ShareActivity.NotificationHelper();
				Utils.logger("d", TAG + "file " + path + " CREATED", DEBUG_TAG);
			}
			
			if (event == FileObserver.DELETE){
				Utils.logger("d", TAG + "file " + path + " DELETED", DEBUG_TAG);
				
				long id = Utils.settings.getLong(path, 0);
				Utils.logger("d", TAG + "Retrieved ID: " +  id, DEBUG_TAG);
				DownloadsService.removeIdUpdateNotification(id);
			}
		}
	
		public void close(){
			super.finalize();
		}
	}
}
