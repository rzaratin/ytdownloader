package dentex.youtube.downloader.service;

import java.io.File;
import java.io.IOException;

import android.app.DownloadManager;
import android.app.DownloadManager.Query;
import android.app.NotificationManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;
import dentex.youtube.downloader.R;
import dentex.youtube.downloader.ShareActivity;
import dentex.youtube.downloader.utils.Utils;

public class DownloadsService extends Service {
	
	private final static String DEBUG_TAG = "DownloadsService";
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
    	
    	private NotificationManager cNotificationManager;
		private NotificationCompat.Builder cBuilder;

		@Override
    	public void onReceive(Context context, Intent intent) {
    		Log.d(DEBUG_TAG, "downloadComplete: onReceive CALLED");
    		long id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
    		String filename = settings.getString(String.valueOf(id), "file");
    		
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
					
					cBuilder =  new NotificationCompat.Builder(context);
					cNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
					
			    	cBuilder.setSmallIcon(R.drawable.icon_new);
					cBuilder.setContentTitle(filename);
					
					if (copy == true) {
						File src = new File(ShareActivity.dir_Downloads, filename);
						//File src = new File("/storage/sdcard1/Video/MTB/Br_test.mp4");
						File dst = new File(ShareActivity.path, filename);
						//File dst = new File("/storage/sdcard1/Video/Br_test.mp4");
						
						Toast.makeText(context, context.getString(R.string.copy_progress), Toast.LENGTH_SHORT).show();
				        cBuilder.setContentText(context.getString(R.string.copy_progress));
						cNotificationManager.notify(ID, cBuilder.build());
						Log.i(DEBUG_TAG, "_ID " + ID + " Copy in progress...");
						
						//mBuilder.setContentTitle(getString(R.string.app_name));
						if (settings.getBoolean("enable_own_notification", true) == true) {
							try {
								removeIdUpdateNotification(id);
							} catch (NullPointerException e) {
								Log.e(DEBUG_TAG, "NullPointerException on removeIdUpdateNotification(id)");
							}
						}
							
						try {
							Utils.copyFile(src, dst, context);
							
							Toast.makeText(context,  filename + ": " + context.getString(R.string.copy_ok), Toast.LENGTH_SHORT).show();
					        cBuilder.setContentText(context.getString(R.string.copy_ok));
							cNotificationManager.notify(DownloadsService.ID, cBuilder.build());
							Log.i(DEBUG_TAG, "_ID " + ID + " Copy OK");
							
							if (ShareActivity.dm.remove(id) == 0) {
								Toast.makeText(context, "error: temp download file NOT removed", Toast.LENGTH_LONG).show();
								Log.e(DEBUG_TAG, "temp download file NOT removed");
				        	}
						} catch (IOException e) {
							Toast.makeText(context, filename + ": " + getString(R.string.copy_error), Toast.LENGTH_LONG).show();
							cBuilder.setContentText(getString(R.string.copy_error));
							cNotificationManager.notify(ID, cBuilder.build());
							Log.e(DEBUG_TAG, "_ID " + ID + "Copy to extSdCard FAILED");
						}
					}
					break;
				case DownloadManager.STATUS_FAILED:
					Log.e(DEBUG_TAG, "_ID " + id + " FAILED (status " + status + ")");
					Log.e(DEBUG_TAG, " Reason: " + reason);
					Toast.makeText(context,  filename + ": " + getString(R.string.download_failed), Toast.LENGTH_LONG).show();
					break;
				default:
					Log.w(DEBUG_TAG, "_ID " + id + " completed with status " + status);
				}
				//mBuilder.setContentTitle(getString(R.string.app_name));
				if (settings.getBoolean("enable_own_notification", true) == true) {
					try {
						removeIdUpdateNotification(id);
					} catch (NullPointerException e) {
						Log.e(DEBUG_TAG, "NullPointerException on removeIdUpdateNotification(id)");
					}
				}
	        }
    	}
    };
    
    public static void removeIdUpdateNotification(long id) {
		if (id != 0) {
			if (ShareActivity.sequence.remove(id)) {
				Log.d(DEBUG_TAG, "_ID " + id + " REMOVED from Notification");
			} else {
				Log.d(DEBUG_TAG, "_ID " + id + " Already REMOVED from Notification");
			}
		} else {
			Log.e(DEBUG_TAG, "_ID  not found!");
		}
		
		if (ShareActivity.sequence.size() > 0) {
			ShareActivity.mBuilder.setContentText(ShareActivity.pt1 + " " + ShareActivity.sequence.size() + " " + ShareActivity.pt2);
			ShareActivity.mNotificationManager.notify(ShareActivity.mId, ShareActivity.mBuilder.build());
		} else {
			ShareActivity.mBuilder.setContentText(ShareActivity.noDownloads);
			ShareActivity.mNotificationManager.notify(ShareActivity.mId, ShareActivity.mBuilder.build());
			//Log.d(DEBUG_TAG, "Notification: no downloads in progress");
		}
	}
}
