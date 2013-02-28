package dentex.youtube.downloader.service;

import java.io.File;

import android.app.Service;
import android.content.Intent;
import android.os.FileObserver;
import android.os.IBinder;
import android.util.Log;
import dentex.youtube.downloader.ShareActivity;

public class ObserverService extends Service {
	
	private final String DEBUG_TAG = "DeletionObserverService";
	
	@Override
	public IBinder onBind(Intent intent) {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public void onCreate() {
		//Bundle b = this.getIntent().getExtras();
		//String uriString = b.getString("video_uri");
		Log.d(DEBUG_TAG, "service created");
		FileObserver fileObserver = new delFileObserver(ShareActivity.videoUri.getPath());
		fileObserver.startWatching();
		
	}
	
	public class delFileObserver extends FileObserver{
        static final String TAG="FILEOBSERVER";

    	String rootPath;
    	static final int mask = (FileObserver.DELETE | FileObserver.DELETE_SELF); 
    	
    	public delFileObserver(String root){
    		super(root, mask);

    		if (! root.endsWith(File.separator)){
    			root += File.separator;
    		}
    		rootPath = root;
    	}

    	public void onEvent(int event, String path) {
    		Log.d(DEBUG_TAG, "onEvent(" + event + ", " + path + ")");
    		
    		if (event == FileObserver.DELETE || event == FileObserver.DELETE_SELF){
    			Log.d(DEBUG_TAG, "file " + path + " deleted");
    		}
    	}

    	public void close(){
    		super.finalize();
    	}
    }

}
