package dentex.youtube.downloader.service;

import java.io.File;
import java.io.IOException;

import com.bugsense.trace.BugSenseHandler;

import android.app.DownloadManager;
import android.app.DownloadManager.Query;
import android.app.DownloadManager.Request;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;
import dentex.youtube.downloader.R;
import dentex.youtube.downloader.SettingsActivity;
import dentex.youtube.downloader.YTD;
import dentex.youtube.downloader.ffmpeg.FfmpegController;
import dentex.youtube.downloader.utils.Observer;
import dentex.youtube.downloader.utils.Utils;

public class FfmpegDownloadService extends Service {
	
	private static final String DEBUG_TAG = "FfmpegDownloadService";
	public static Context nContext;
	public long enqueue;
	public String ffmpegBinName = FfmpegController.ffmpegBinName;
	private int cpuVers;
	private String sdCardAppDir;
	public static String DIR;
	private DownloadManager dm;
	public Observer.YtdFileObserver ffmpegBinObserver;

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}
	
	@Override
	public void onCreate() {
		Utils.logger("d", "service created", DEBUG_TAG);
		BugSenseHandler.initAndStartSession(getApplicationContext(), YTD.BAK);
		nContext = getBaseContext();	
		registerReceiver(ffmpegReceiver, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
	}

	public static Context getContext() {
        return nContext;
    }
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		cpuVers = intent.getIntExtra("CPU", 7);
		Utils.logger("d", "arm CPU version: " + cpuVers, DEBUG_TAG);
		
		sdCardAppDir = intent.getStringExtra("DIR");
		DIR = sdCardAppDir;
		
		downloadFfmpeg();
		
		super.onStartCommand(intent, flags, startId);
		return START_NOT_STICKY;
	}
	
	@Override
	public void onDestroy() {
		Utils.logger("d", "service destroyed", DEBUG_TAG);
		unregisterReceiver(ffmpegReceiver);
	}
	
	private void downloadFfmpeg() {
		String link = getString(R.string.ffmpeg_download_dialog_msg_link, cpuVers);

		Utils.logger("d", "FFmpeg download link: " + link, DEBUG_TAG);
		
        Request request = new Request(Uri.parse(link));
        request.setDestinationInExternalFilesDir(nContext, null, ffmpegBinName);
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE);
        request.setVisibleInDownloadsUi(false);
        request.setTitle(getString(R.string.ffmpeg_download_notification));
        dm = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
        enqueue = dm.enqueue(request);
        
		ffmpegBinObserver = new Observer.YtdFileObserver(DIR);
        ffmpegBinObserver.startWatching();
	}
	
	public static void copyFfmpegToAppDataDir(Context context, File src, File dst) {
		try {
			Toast.makeText(context, "YTD: " + context.getString(R.string.ffmpeg_install), Toast.LENGTH_LONG).show();
			Utils.logger("i", "trying to copy FFmpeg binary to private App dir", DEBUG_TAG);
			Utils.copyFile(src, dst);
			
			Toast.makeText(context, "YTD: " + context.getString(R.string.ffmpeg_ready), Toast.LENGTH_LONG).show();
			SettingsActivity.SettingsFragment.touchAudioExtrPref(true, true);
		} catch (IOException e) {
			Toast.makeText(context, "YTD: " + context.getString(R.string.ffmpeg_install_failed), Toast.LENGTH_LONG).show();
			Log.e(DEBUG_TAG, "ffmpeg copy to app_bin failed. " + e.getMessage());
			SettingsActivity.SettingsFragment.touchAudioExtrPref(true, false);
		}
	}

	BroadcastReceiver ffmpegReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			Utils.logger("d", "ffmpegReceiver: onReceive CALLED", DEBUG_TAG);
    		long id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
    		
    		//if (enqueue != -1 && id != -2 && id == enqueue) {
	    		Query query = new Query();
				query.setFilterById(id);
				Cursor c = dm.query(query);
				if (c.moveToFirst()) {
				
					int statusIndex = c.getColumnIndex(DownloadManager.COLUMN_STATUS);
					int reasonIndex = c.getColumnIndex(DownloadManager.COLUMN_REASON);
					int status = c.getInt(statusIndex);
					int reason = c.getInt(reasonIndex);

					switch (status) {
					
					case DownloadManager.STATUS_SUCCESSFUL:
	    		
						File src = new File(nContext.getExternalFilesDir(null), ffmpegBinName);
						File dst = new File(nContext.getDir("bin", 0), ffmpegBinName);
						
						String md5 = null;
						if (cpuVers == 7) md5 = "33fcf4d5a3b2e5193bd42c2c1fc2abc7";
						if (cpuVers == 5) md5 = "0606931cfbaca351a47e59ab198bc81e";
						
						if (Utils.checkMD5(md5, src)) {
							copyFfmpegToAppDataDir(context, src, dst);
						} else {
							SettingsActivity.SettingsFragment.touchAudioExtrPref(true, false);
							deleteBadDownload(id);
						}
						break;
						
					case DownloadManager.STATUS_FAILED:
						Log.e(DEBUG_TAG, ffmpegBinName + ", _ID " + id + " FAILED (status " + status + ")");
						Log.e(DEBUG_TAG, " Reason: " + reason);
						Toast.makeText(nContext,  ffmpegBinName + ": " + getString(R.string.download_failed), Toast.LENGTH_LONG).show();
						
						SettingsActivity.SettingsFragment.touchAudioExtrPref(true, false);
						deleteBadDownload(id);
						break;
						
					default:
						Utils.logger("w", ffmpegBinName + ", _ID " + id + " completed with status " + status, DEBUG_TAG);
					}
				}
    		//}
    		ffmpegBinObserver.stopWatching();
    		stopSelf();
		}
	};
	
	private void deleteBadDownload (long id) {
		dm.remove(id);
		Toast.makeText(this, getString(R.string.download_failed), Toast.LENGTH_LONG).show();
		
	}
	
}
