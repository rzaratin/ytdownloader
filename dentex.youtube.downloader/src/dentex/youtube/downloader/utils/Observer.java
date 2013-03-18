package dentex.youtube.downloader.utils;

import java.io.File;

import dentex.youtube.downloader.service.DownloadsService;

import android.os.FileObserver;
import android.util.Log;

// reference:
// https://gist.github.com/shirou/659180 - https://github.com/shirou
	
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
			//Log.d(DEBUG_TAG, TAG + "onEvent " + event + ", " + path);
			
			if (event == FileObserver.CREATE) {
				Log.d(DEBUG_TAG, TAG + "file " + path + " CREATED");
			}
			
			if (event == FileObserver.DELETE){
				Log.d(DEBUG_TAG, TAG + "file " + path + " DELETED");
				
				long id = Utils.settings.getLong(path, 0);
				Log.d(DEBUG_TAG, TAG + "Retrieved ID: " +  id);
				DownloadsService.removeIdUpdateNotification(id);
			}
		}
	
		public void close(){
			super.finalize();
		}
	}
}
