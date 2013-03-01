package dentex.youtube.downloader.service;

import java.util.Arrays;

import android.app.DownloadManager;
import android.app.DownloadManager.Query;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.IBinder;
import android.util.Log;
import dentex.youtube.downloader.ShareActivity;
import dentex.youtube.downloader.utils.Utils;

public class DownloadsService extends Service {
	
	private final String DEBUG_TAG = "DownloadsService";
	public static SharedPreferences settings = ShareActivity.settings;
	public final String PREFS_NAME = ShareActivity.PREFS_NAME;

	@Override
	public IBinder onBind(Intent intent) {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public void onCreate() {
		settings = getSharedPreferences(PREFS_NAME, 0);
		
		//Toast.makeText(this, "NotificationClickReceiver service created", Toast.LENGTH_SHORT).show();
		Log.d(DEBUG_TAG, "service created");
		//registerReceiver(NotificationClickReceiver, new IntentFilter(DownloadManager.ACTION_NOTIFICATION_CLICKED));
		registerReceiver(downloadComplete, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
		registerReceiver(downloadClicked, new IntentFilter(DownloadManager. ACTION_NOTIFICATION_CLICKED));
	}
	
	@Override
	  public void onDestroy() {
	    //Toast.makeText(this, "NotificationClickReceiver service destroyed", Toast.LENGTH_SHORT).show();
	    Log.d(DEBUG_TAG, "service destroyed");
	    unregisterReceiver(downloadComplete);
	}

	BroadcastReceiver downloadComplete = new BroadcastReceiver() {
    	
    	@Override
    	public void onReceive(Context context, Intent intent) {
    		Log.d(DEBUG_TAG, "downloadComplete: onReceive CALLED");
    		long id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
	        
			Query query = new Query();
			query.setFilterById(id);
			Cursor c = ShareActivity.dm.query(query);
			if (c.moveToFirst()) {
				int columnIndex = c.getColumnIndex(DownloadManager.COLUMN_STATUS);
				int status = c.getInt(columnIndex);
				Log.d(DEBUG_TAG, "status: " + status);
				switch (status) {
				
				case DownloadManager.STATUS_SUCCESSFUL:
					Log.d(DEBUG_TAG, "_ID " + id + " SUCCESSFUL");
					break;
				case DownloadManager.STATUS_FAILED:
					Log.d(DEBUG_TAG, "_ID " + id + " FAILED");
					break;
				default:
					Log.d(DEBUG_TAG, "_ID completed with status " + status);
				}
				
				Utils.removeIdUpdateNotification(id);
	        }
    	}
    };
    
    BroadcastReceiver downloadClicked = new BroadcastReceiver() {
    	
    	@Override
    	public void onReceive(Context context, Intent intent) {
    		Log.d(DEBUG_TAG, "downloadClicked: onReceive CALLED");
    		long[] id = intent.getLongArrayExtra(DownloadManager.EXTRA_NOTIFICATION_CLICK_DOWNLOAD_IDS);
    		Log.d(DEBUG_TAG, "_downloads clicked: " + Arrays.toString(id));
    		Query query = new Query();
			query.setFilterById(id);
			Cursor c = ShareActivity.dm.query(query);
			if (c.moveToFirst()) {
				int columnIndex = c.getColumnIndex(DownloadManager.COLUMN_DESCRIPTION);
				Log.d(DEBUG_TAG, "_COLUMN_DESCRIPTION: " + columnIndex);
			}
    	}
    };
    
}
