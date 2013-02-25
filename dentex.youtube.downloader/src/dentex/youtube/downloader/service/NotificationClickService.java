package dentex.youtube.downloader.service;

import android.app.DownloadManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.util.Log;

public class NotificationClickService extends Service {
	
	private static final String DEBUG_TAG = "NotificationClickService";

	@Override
	public IBinder onBind(Intent intent) {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public void onCreate() {
		//Toast.makeText(this, "NotificationClickReceiver service created", Toast.LENGTH_SHORT).show();
		Log.d(DEBUG_TAG, "service created");
		registerReceiver(NotificationClickReceiver, new IntentFilter(DownloadManager.ACTION_NOTIFICATION_CLICKED));
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
   
	    		Intent i = new Intent(android.app.DownloadManager.ACTION_VIEW_DOWNLOADS);
	    		i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | 
	    				Intent.FLAG_ACTIVITY_SINGLE_TOP | 
	    				Intent.FLAG_ACTIVITY_CLEAR_TOP | 
	    				Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
	            context.startActivity(i);
    	}
    };
}
