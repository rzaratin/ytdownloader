package dentex.youtube.downloader.service;

import java.io.File;
import java.io.IOException;

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
import android.widget.Toast;
import dentex.youtube.downloader.R;
import dentex.youtube.downloader.ShareActivity;
import dentex.youtube.downloader.utils.Utils;

public class DownloadsService extends Service {
	
	private final String DEBUG_TAG = "DownloadsService";
	public static SharedPreferences settings = ShareActivity.settings;
	public final String PREFS_NAME = ShareActivity.PREFS_NAME;
	public boolean copy;
	public static int ID;

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}
	
	@Override
	public void onCreate() {
		settings = getSharedPreferences(PREFS_NAME, 0);
		Log.d(DEBUG_TAG, "service created");
		registerReceiver(downloadComplete, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		copy = intent.getExtras().getBoolean("COPY");
		if (copy == true) {
			Log.d(DEBUG_TAG, "Copy to extSdcard: true");
		} else {
			Log.d(DEBUG_TAG, "Copy to extSdcard: false");
		}
		return super.onStartCommand(intent, flags, startId);
	}
	
	@Override
	public void onDestroy() {
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
				
				int statusIndex = c.getColumnIndex(DownloadManager.COLUMN_STATUS);
				int reasonIndex = c.getColumnIndex(DownloadManager.COLUMN_REASON);
				int status = c.getInt(statusIndex);
				int reason = c.getInt(reasonIndex);

				switch (status) {
				case DownloadManager.STATUS_SUCCESSFUL:
					Log.d(DEBUG_TAG, "_ID " + id + " SUCCESSFUL (status " + status + ")");
					ID = (int) id;
					String title = settings.getString(String.valueOf(id), "");
					ShareActivity.mBuilder.setContentTitle(title);
					if (copy == true) {
						try {
							//File src = new File(ShareActivity.dir_Downloads, ShareActivity.composedFilename);
							File src = new File("/storage/sdcard1/Video/MTB/Br_test.mp4");
							//File dst = new File(ShareActivity.path, ShareActivity.composedFilename);
							File dst = new File("/storage/sdcard1/Video/Br_test.mp4");
							
							Toast.makeText(context, context.getString(R.string.copy_progress), Toast.LENGTH_SHORT).show();
					        ShareActivity.mBuilder.setContentText(context.getString(R.string.copy_progress));
							ShareActivity.mNotificationManager.notify(ID, ShareActivity.mBuilder.build());
							Log.d(DEBUG_TAG, context.getString(R.string.copy_progress));
							
							ShareActivity.mBuilder.setContentTitle(getString(R.string.app_name));
							Utils.removeIdUpdateNotification(id);
							
							Utils.copyFile(src, dst, context);
							
							Toast.makeText(context, context.getString(R.string.copy_ok), Toast.LENGTH_SHORT).show();
					        ShareActivity.mBuilder.setContentText(context.getString(R.string.copy_ok));
							ShareActivity.mNotificationManager.notify(DownloadsService.ID, ShareActivity.mBuilder.build());
							Log.d(DEBUG_TAG, context.getString(R.string.copy_ok));
							
						} catch (IOException e) {
							Toast.makeText(context, getString(R.string.copy_error), Toast.LENGTH_SHORT).show();
							ShareActivity.mBuilder.setContentText(getString(R.string.copy_error));
							ShareActivity.mNotificationManager.notify(ID, ShareActivity.mBuilder.build());
							Log.e(DEBUG_TAG, "copy FAILED: " + e.getMessage());
						}
					}
					break;
				case DownloadManager.STATUS_FAILED:
					Log.e(DEBUG_TAG, "_ID " + id + " FAILED (status " + status + ")");
					Log.e(DEBUG_TAG, " Reason: " + reason);
					Toast.makeText(context, getString(R.string.download_failed), Toast.LENGTH_SHORT).show();
					break;
				default:
					Log.w(DEBUG_TAG, "_ID " + id + " completed with status " + status);
				}
				ShareActivity.mBuilder.setContentTitle(getString(R.string.app_name));
				Utils.removeIdUpdateNotification(id);
	        }
    	}
    };    
}
