package dentex.youtube.downloader.service;

import dentex.youtube.downloader.ShareActivity;
import android.app.DownloadManager;
import android.app.PendingIntent;
import android.app.Service;
import android.app.DownloadManager.Query;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.os.IBinder;
import android.util.Log;

public class DownloadsService extends Service {
	
	private static final String DEBUG_TAG = "DownloadsService";

	@Override
	public IBinder onBind(Intent intent) {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public void onCreate() {
		//Toast.makeText(this, "NotificationClickReceiver service created", Toast.LENGTH_SHORT).show();
		Log.d(DEBUG_TAG, "service created");
		//registerReceiver(NotificationClickReceiver, new IntentFilter(DownloadManager.ACTION_NOTIFICATION_CLICKED));
		registerReceiver(NotificationClickReceiver, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
	}
	
	@Override
	  public void onDestroy() {
	    //Toast.makeText(this, "NotificationClickReceiver service destroyed", Toast.LENGTH_SHORT).show();
	    Log.d(DEBUG_TAG, "service destroyed");
	    unregisterReceiver(NotificationClickReceiver);
	}
	
	BroadcastReceiver NotificationClickReceiver = new BroadcastReceiver() {
    	
    	@Override
    	public void onReceive(Context context, Intent intent) {
    		Log.d(DEBUG_TAG, "NotificationClickReceiver: onReceive CALLED");
    		long id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -2);
	        if (ShareActivity.enqueue != -1 && id != -2 && id == ShareActivity.enqueue) {
	            Query query = new Query();
	            query.setFilterById(id);

	            Cursor c = ShareActivity.dm.query(query);
	            if (c.moveToFirst()) {
	                int columnIndex = c.getColumnIndex(DownloadManager.COLUMN_STATUS);
	                int status = c.getInt(columnIndex);
	                if (status == DownloadManager.STATUS_SUCCESSFUL) {
	                	ShareActivity.mBuilder.setContentText("Completed. Click to open.");
	                	
	                	Intent v_intent = new Intent();
                        v_intent.setAction(android.content.Intent.ACTION_VIEW);
                        v_intent.setDataAndType(ShareActivity.videoUri, "video/*");
                        
                        v_intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | 
                        				  Intent.FLAG_ACTIVITY_SINGLE_TOP |
                        				  Intent.FLAG_ACTIVITY_NEW_TASK);
                        
                    	PendingIntent contentIntent = PendingIntent.getActivity(context, 0, v_intent, 0);
                    	ShareActivity.mBuilder.setContentIntent(contentIntent);
                        
	                	ShareActivity.mNotificationManager.notify(ShareActivity.mId, ShareActivity.mBuilder.build());
	                }
	            }
	        }
    	}
    };
}
