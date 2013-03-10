package dentex.youtube.downloader.service;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.app.DownloadManager;
import android.app.DownloadManager.Query;
import android.app.DownloadManager.Request;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;
import dentex.youtube.downloader.R;
import dentex.youtube.downloader.ShareActivity;
import dentex.youtube.downloader.utils.Utils;

public class AutoUpgradeApkService extends Service {
	
	private String currentVersion;
	private String apkFilename;
	private static final String DEBUG_TAG = "AutoUpgradeApkService";
	private DownloadManager downloadManager;
	File dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
	private static String webPage;
	public long enqueue;
	private Uri fileUri;
	private AsyncUpdate asyncAutoUpdate;
	public String onlineVersion;
	public String onlineChangelog;
	public static String matchedVersion;
	public static String matchedChangeLog;
	public String matchedMd5;
	boolean isAsyncTaskRunning = false;
	private String compRes = "init";
	
	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public void onCreate() {
		Log.d(DEBUG_TAG, "service created");
		try {
		    currentVersion = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
		    Log.d(DEBUG_TAG, "current version: " + currentVersion);
		} catch (NameNotFoundException e) {
		    Log.e(DEBUG_TAG, "version not read: " + e.getMessage());
		    currentVersion = "100";
		}
		ConnectivityManager connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
		
		if (networkInfo != null && networkInfo.isConnected() && matchedVersion != "n.a.") {
			
			registerReceiver(apkReceiver, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
			
			try {
				//init version and changelog
				matchedChangeLog = null;
				matchedVersion = null;
				
				asyncAutoUpdate = new AsyncUpdate();
				webPage = "http://sourceforge.net/projects/ytdownloader/files/";
				asyncAutoUpdate.execute(webPage);
			} catch (NullPointerException e) {
				Log.e(DEBUG_TAG, "unable to retrieve update data.");
			}
		} else {
			Log.e(DEBUG_TAG, getString(R.string.no_net));
		}
	}
	
	@Override
	public void onDestroy() {
		Log.d(DEBUG_TAG, "service destroyed");
		unregisterReceiver(apkReceiver);
		//stopSelf();
	}

	
	private class AsyncUpdate extends AsyncTask<String, Void, Integer> {
		
		protected void onPreExecute() {
			isAsyncTaskRunning = true;
		}

    	protected Integer doInBackground(String... urls) {
            // params comes from the execute() call: params[0] is the url.
            try {
            	Log.d(DEBUG_TAG, "doInBackground...");
                return downloadUrl(urls[0]);
            } catch (IOException e) {
            	Log.e(DEBUG_TAG, "doInBackground: " + e.getMessage());
            	matchedVersion = "n.a.";
                return 1;
            }
        }

        private int downloadUrl(String myurl) throws IOException {
            InputStream is = null;
            int len = 100000;
            Log.d(DEBUG_TAG, "The link is: " + myurl);
            try {
                URL url = new URL(myurl);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestProperty("User-Agent","<em>" + ShareActivity.USER_AGENT_FIREFOX + "</em>");
                conn.setReadTimeout(20000 /* milliseconds */);
                conn.setConnectTimeout(30000 /* milliseconds */);
                conn.setInstanceFollowRedirects(false);
                conn.setRequestMethod("GET");
                conn.setDoInput(true);
                conn.connect();
                int response = conn.getResponseCode();
                Log.d(DEBUG_TAG, "The response is: " + response);
                is = conn.getInputStream();
                if (!asyncAutoUpdate.isCancelled()) {
                	return readIt(is, len);
                } else {
                	Log.d(DEBUG_TAG, "asyncUpdate cancelled @ 'return readIt'");
                	return 3;
                }
                
            } finally {
                if (is != null) {
                    is.close();
                }
            }
		}
        
        public int readIt(InputStream stream, int len) throws IOException, UnsupportedEncodingException {
            Reader reader = null;
            reader = new InputStreamReader(stream, "UTF-8");
            char[] buffer = new char[len];
            reader.read(buffer);
            String content = new String(buffer);
           	return OnlineUpdateCheck(content);
        }
        
        @Override
        protected void onPostExecute(Integer result) {
     
	        if (matchedVersion.contentEquals("n.a.")) {
	        }
	        
	        if (compRes.contentEquals(">")) {
		        Log.d(DEBUG_TAG, "version comparison: downloading latest version...");
		        
		        NotificationCompat.Builder builder =  new NotificationCompat.Builder(AutoUpgradeApkService.this);
            	
            	builder.setSmallIcon(R.drawable.icon_nb)
            	        .setContentTitle(getString(R.string.app_name))
            	        .setContentText("v" + matchedVersion + " " + getString(R.string.new_v_download));
            	
            	NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            	notificationManager.notify(2, builder.build());
		        
		        callDownloadApk(matchedVersion);
	    	} else if (compRes.contentEquals("==")) {
	    		//PopUps.showPopUp(getString(R.string.information), getString(R.string.upgrade_latest_installed), "info", AutoUpgradeApk.this);
	    		Log.d(DEBUG_TAG, "version comparison: latest version is already installed!");

	    	} else if (compRes.contentEquals("<")) {
	    		// No need for a popup...
	    		Log.d(DEBUG_TAG, "version comparison: installed higher than the one online? ...this should not happen...");

	    	} else if (compRes.contentEquals("init")) {
	    		Log.d(DEBUG_TAG, "version comparison not tested");
	    	}
        }   
	}
	
	private int OnlineUpdateCheck(String content) {
		Log.d(DEBUG_TAG, "OnlineUpdateCheck");
		int res = 3;
		if (asyncAutoUpdate.isCancelled()) {
			Log.d(DEBUG_TAG, "asyncUpdate cancelled @ 'OnlineUpdateCheck' begin");
			return 3;
		}
		// match version name
		Pattern v_pattern = Pattern.compile("versionName=\\\"(.*)\\\"");
        Matcher v_matcher = v_pattern.matcher(content);
        if (v_matcher.find() && !asyncAutoUpdate.isCancelled()) {
        	matchedVersion = v_matcher.group(1);
	    	Log.i(DEBUG_TAG, "_on-line version: " + matchedVersion);
	    	res = res - 1;
	    } else {
        	matchedVersion = "not_found";
        	Log.e(DEBUG_TAG, "_online version: not found!");
        }
        
        // match changelog
        Pattern cl_pattern = Pattern.compile("<pre><code> v(.*?)</code></pre>", Pattern.DOTALL);
    	Matcher cl_matcher = cl_pattern.matcher(content);
    	if (cl_matcher.find() && !asyncAutoUpdate.isCancelled()) {
    		matchedChangeLog = " v" + cl_matcher.group(1);
    		Log.i(DEBUG_TAG, "_online changelog...");
    		res = res - 1;
    	} else {
    		matchedChangeLog = "not_found";
    		Log.e(DEBUG_TAG, "_online changelog not found!");
    	}
    	
    	// match md5
    	// checksum: <code>d7ef1e4668b24517fb54231571b4a74f</code> dentex.youtube.downloader_v1.4
    	Pattern md5_pattern = Pattern.compile("checksum: <code>(.{32})</code> dentex.youtube.downloader_v");
    	Matcher md5_matcher = md5_pattern.matcher(content);
    	if (md5_matcher.find() && !asyncAutoUpdate.isCancelled()) {
    		matchedMd5 = md5_matcher.group(1);
    		Log.i(DEBUG_TAG, "_online md5sum: " + matchedMd5);
    		res = res - 1;
    	} else {
    		matchedMd5 = "not_found";
    		Log.e(DEBUG_TAG, "_online md5sum not found!");
    	}
    	
    	compRes = Utils.VersionComparator.compare(matchedVersion, currentVersion);
    	Log.d(DEBUG_TAG, "version comparison: " + matchedVersion + " " + compRes + " " + currentVersion);
    	
    	return res;
    }
	
	void callDownloadApk(String ver) {
		String apklink = "http://sourceforge.net/projects/ytdownloader/files/dentex.youtube.downloader_v" + ver + ".apk/download";
		apkFilename = "dentex.youtube.downloader_v" + ver + ".apk";
	    Request request = new Request(Uri.parse(apklink));
	    fileUri = Uri.parse(dir.toURI() + apkFilename);
	    request.setDestinationUri(fileUri);
	    request.allowScanningByMediaScanner();
	    request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE);
	    request.setTitle("YouTube Downloader v" + ver);
	    downloadManager = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
	    enqueue = downloadManager.enqueue(request);
	}

	BroadcastReceiver apkReceiver = new BroadcastReceiver() {

		@Override
        public void onReceive(final Context context, final Intent intent) {
	        long id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -2);
	        if (enqueue != -1 && id != -2 && id == enqueue) {
	            Query query = new Query();
	            query.setFilterById(id);
	            DownloadManager dm = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
	            Cursor c = dm.query(query);
	            if (c.moveToFirst()) {
	                int columnIndex = c.getColumnIndex(DownloadManager.COLUMN_STATUS);
	                int status = c.getInt(columnIndex);
	                if (status == DownloadManager.STATUS_SUCCESSFUL) {
                    	
                    	if (Utils.checkMD5(matchedMd5, new File(dir, apkFilename))) {
                    	
                    		NotificationCompat.Builder builder =  new NotificationCompat.Builder(context);
                        	
                        	builder.setSmallIcon(R.drawable.icon_nb)
                        	        .setContentTitle(getString(R.string.app_name))
                        	        .setContentText("v" + matchedVersion + " " + getString(R.string.new_v_install));
                        	
                        	NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
	
                            Intent intent1 = new Intent();
                            intent1.setAction(android.content.Intent.ACTION_VIEW);
                        	intent1.setDataAndType(fileUri, "application/vnd.android.package-archive");
                        	PendingIntent contentIntent = PendingIntent.getActivity(context, 0, intent1, 0);
                        	builder.setContentIntent(contentIntent);
                        	
                        	notificationManager.notify(2, builder.build());
                    	} else {
                    		deleteBadDownload(context, intent);
                    	}
	                }
	            }
            }
		}

		public void deleteBadDownload(final Context context, final Intent intent) {
			long id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -2);
			downloadManager.remove(id);
			Toast.makeText(context, getString(R.string.failed_download), Toast.LENGTH_LONG).show();
		}

	};
}
