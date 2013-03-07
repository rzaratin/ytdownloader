package dentex.youtube.downloader.utils;

import java.io.File;

import android.os.FileObserver;
import android.util.Log;

public class Observer {
	
	static final String DEBUG_TAG = "Observer";

	public static class delFileObserver extends FileObserver {
	    static final String TAG="FileObserver: ";
	
		String rootPath;
		static final int mask = (FileObserver.CREATE | FileObserver.DELETE | FileObserver.DELETE_SELF); 
		
		public delFileObserver(String root){
			super(root, mask);
	
			if (! root.endsWith(File.separator)){
				root += File.separator;
			}
			rootPath = root;
		}
	
		public void onEvent(int event, String path) {
			/*Log.d(DEBUG_TAG, TAG + "onEvent " + event + ", " + path);
			
			if (event == FileObserver.CREATE) {
				Log.d(DEBUG_TAG, TAG + "file " + path + " CREATED");
			}*/
			
			if (event == FileObserver.DELETE || event == FileObserver.DELETE_SELF){
				Log.d(DEBUG_TAG, TAG + "file " + path + " DELETED");
				
				long id = Utils.settings.getLong(path, 0);
				Log.d(DEBUG_TAG, TAG + "id: " +  id);
				Utils.removeIdUpdateNotification(id);
			}
		}
	
		public void close(){
			super.finalize();
		}
	}
}
