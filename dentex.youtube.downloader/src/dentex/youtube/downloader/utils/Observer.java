package dentex.youtube.downloader.utils;

import java.io.File;

import dentex.youtube.downloader.ShareActivity;

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
				Log.d(Utils.DEBUG_TAG, TAG + "file " + path + " DELETED");
				
				long id = Utils.settings.getLong(path, 0);
				Log.d(Utils.DEBUG_TAG, TAG + "id: " +  id);
				Observer.removeIdUpdateNotification(id);
			}
		}
	
		public void close(){
			super.finalize();
		}
	}

	public static void removeIdUpdateNotification(long id) {
		
		if (id != 0) {
			if (ShareActivity.sequence.remove(id)) {
				Log.d(Utils.DEBUG_TAG, "_ID " + id + " REMOVED from Notification");
			} else {
				Log.d(Utils.DEBUG_TAG, "_ID " + id + " Already REMOVED from Notification");
			}
		} else {
			Log.e(Utils.DEBUG_TAG, "_ID  not found!");
		}
		
		if (ShareActivity.sequence.size() > 0) {
			//ShareActivity.mBuilder.setContentText("Downloading " + ShareActivity.sequence.size() + " video files.");
			ShareActivity.mBuilder.setContentText(ShareActivity.pt1 + " " + ShareActivity.sequence.size() + " " + ShareActivity.pt2);
			ShareActivity.mNotificationManager.notify(ShareActivity.mId, ShareActivity.mBuilder.build());
			Log.d(Utils.DEBUG_TAG, "Notification: video num. updated");
		} else {
			ShareActivity.mBuilder.setContentText(ShareActivity.noDownloads);
			ShareActivity.mNotificationManager.notify(ShareActivity.mId, ShareActivity.mBuilder.build());
			Log.d(Utils.DEBUG_TAG, "Notification: no downloads in progress");
		}
	}

}
