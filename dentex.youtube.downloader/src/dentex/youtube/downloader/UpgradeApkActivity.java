package dentex.youtube.downloader;

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

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.app.DownloadManager.Query;
import android.app.DownloadManager.Request;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import dentex.youtube.downloader.utils.PopUps;
import dentex.youtube.downloader.utils.Utils;

public class UpgradeApkActivity extends Activity {
	
	private ProgressBar progressBar2;
	private String currentVersion;
	private String apkFilename;
	private static final String DEBUG_TAG = "UpgradeApkActivity";
	public boolean buttonClickedOnce = false;
	public TextView tv;
	public TextView cl;
	public Button upgradeButton;
	private DownloadManager downloadManager;
	File dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
	private String webPage;
	public long enqueue;
	private Uri fileUri;
	private AsyncUpdate asyncUpdate;
	public String onlineVersion;
	public String onlineChangelog;
	public String matchedVersion;
	public String matchedChangeLog;
	public String matchedMd5;
	boolean isAsyncTaskRunning = false;
	private String compRes = "init";
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		// Theme init
    	Utils.themeInit(this);
    	
		setContentView(R.layout.activity_upgrade_apk);
		
		try {
		    currentVersion = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
		    Utils.logger("d", "current version: " + currentVersion, DEBUG_TAG);
		} catch (NameNotFoundException e) {
		    Log.e(DEBUG_TAG, "version not read: " + e.getMessage());
		    currentVersion = "100";
		}
		
		upgradeButton = (Button) findViewById(R.id.upgrade_button);
		
		progressBar2 = (ProgressBar) findViewById(R.id.progressBar2);
        progressBar2.setVisibility(View.GONE);
        
        tv = (TextView) findViewById(R.id.upgrade_upper_text);
        tv.setText(getString(R.string.upgrade_uppertext_init) + currentVersion);
        
        cl = (TextView) findViewById(R.id.upgrade_textview2);
	}

	public void upgradeButtonClick(View v) {
		ConnectivityManager connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
		
		if (networkInfo != null && networkInfo.isConnected() && matchedVersion != "n.a.") {
			try {
				if (buttonClickedOnce == false) {
					buttonClickedOnce = true;
					
					//init version and changelog
					matchedChangeLog = null;
					matchedVersion = null;
					cl.setText("");
					
					asyncUpdate = new AsyncUpdate();
					webPage = "http://sourceforge.net/projects/ytdownloader/files/";
					asyncUpdate.execute(webPage);
				} else {
					buttonClickedOnce = false;
					callDownloadApk(matchedVersion);
				    upgradeButton.setEnabled(false);
				}
			} catch (NullPointerException e) {
				PopUps.showPopUp(getString(R.string.error), getString(R.string.upgrade_network_error), "alert", UpgradeApkActivity.this);
				Log.e(DEBUG_TAG, "unable to retrieve update data.");
				
			}
		} else {
			progressBar2.setVisibility(View.GONE);
			tv.setText(getString(R.string.no_net));
			upgradeButton.setEnabled(false);
			PopUps.showPopUp(getString(R.string.no_net), getString(R.string.no_net_dialog_msg), "alert", this);
		}
	}
	
	@Override
    protected void onStart() {
        super.onStart();
        registerReceiver(apkReceiver, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
        Log.v(DEBUG_TAG, "_onStart");
    }
	
    @Override
    protected void onRestart() {
    	super.onRestart();
    	Log.v(DEBUG_TAG, "_onRestart");
    }

    @Override
    public void onPause() {
    	super.onPause();
    	Log.v(DEBUG_TAG, "_onPause");
    }
    
    @Override
    protected void onStop() {
        super.onStop();
    	unregisterReceiver(apkReceiver);
    	Log.v(DEBUG_TAG, "_onStop");
    	
    	if (isAsyncTaskRunning) {
    		asyncUpdate.cancel(true);
    		isAsyncTaskRunning = false;
    	}
    }
	
	private class AsyncUpdate extends AsyncTask<String, Void, Integer> {
		
		protected void onPreExecute() {
			upgradeButton.setEnabled(false);
			progressBar2.setVisibility(View.VISIBLE);
			tv.setText(R.string.upgrade_uppertext_searching);
			isAsyncTaskRunning = true;
		}

    	protected Integer doInBackground(String... urls) {
            // params comes from the execute() call: params[0] is the url.
            try {
            	Utils.logger("d", "doInBackground...", DEBUG_TAG);
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
            Utils.logger("d", "The link is: " + myurl, DEBUG_TAG);
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
                Utils.logger("d", "The response is: " + response, DEBUG_TAG);
                is = conn.getInputStream();
                if (!asyncUpdate.isCancelled()) {
                	return readIt(is, len);
                } else {
                	Utils.logger("d", "asyncUpdate cancelled @ 'return readIt'", DEBUG_TAG);
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
        	
        	progressBar2.setVisibility(View.GONE);
			
        	tv.setText(getString(R.string.upgrade_latest) + matchedVersion + getString(R.string.upgrade_installed) + currentVersion);
	        cl.setText(matchedChangeLog);
	        
	        if (matchedVersion.contentEquals("n.a.")) {
	        	Toast.makeText(UpgradeApkActivity.this, "Invalid HTTP server response", Toast.LENGTH_LONG).show();
	        }
	        
	        if (compRes.contentEquals(">")) {
		        Utils.logger("d", "version comparison: downloading latest version...", DEBUG_TAG);
			    upgradeButton.setEnabled(true);
			    upgradeButton.setText(getString(R.string.upgrade_button_download));
	    	} else if (compRes.contentEquals("==")) {
	    		PopUps.showPopUp(getString(R.string.information), getString(R.string.upgrade_latest_installed), "info", UpgradeApkActivity.this);
	    		Utils.logger("d", "version comparison: latest version is already installed!", DEBUG_TAG);
	    		upgradeButton.setEnabled(false);
	    	} else if (compRes.contentEquals("<")) {
	    		// No need for a popup...
	    		Utils.logger("d", "version comparison: installed higher than the one online? ...this should not happen...", DEBUG_TAG);
	    		upgradeButton.setEnabled(false);
	    	} else if (compRes.contentEquals("init")) {
	    		Utils.logger("d", "version comparison not tested", DEBUG_TAG);
	    		upgradeButton.setEnabled(false);
	    	}
        }   
	}
	
	private int OnlineUpdateCheck(String content) {
		Utils.logger("d", "OnlineUpdateCheck", DEBUG_TAG);
		int res = 3;
		if (asyncUpdate.isCancelled()) {
			Utils.logger("d", "asyncUpdate cancelled @ 'OnlineUpdateCheck' begin", DEBUG_TAG);
			return 3;
		}
		// match version name
		Pattern v_pattern = Pattern.compile("versionName=\\\"(.*)\\\"");
        Matcher v_matcher = v_pattern.matcher(content);
        if (v_matcher.find() && !asyncUpdate.isCancelled()) {
        	matchedVersion = v_matcher.group(1);
	    	Utils.logger("i", "_on-line version: " + matchedVersion, DEBUG_TAG);
	    	res = res - 1;
	    } else {
        	matchedVersion = "not_found";
        	Log.e(DEBUG_TAG, "_online version: not found!");
        }
        
        // match changelog
        Pattern cl_pattern = Pattern.compile("<pre><code> v(.*?)</code></pre>", Pattern.DOTALL);
    	Matcher cl_matcher = cl_pattern.matcher(content);
    	if (cl_matcher.find() && !asyncUpdate.isCancelled()) {
    		matchedChangeLog = " v" + cl_matcher.group(1);
    		Utils.logger("i", "_online changelog...", DEBUG_TAG);
    		res = res - 1;
    	} else {
    		matchedChangeLog = "not_found";
    		Log.e(DEBUG_TAG, "_online changelog not found!");
    	}
    	
    	// match md5
    	// checksum: <code>d7ef1e4668b24517fb54231571b4a74f</code> dentex.youtube.downloader_v1.4
    	Pattern md5_pattern = Pattern.compile("checksum: <code>(.{32})</code> dentex.youtube.downloader_v");
    	Matcher md5_matcher = md5_pattern.matcher(content);
    	if (md5_matcher.find() && !asyncUpdate.isCancelled()) {
    		matchedMd5 = md5_matcher.group(1);
    		Utils.logger("i", "_online md5sum: " + matchedMd5, DEBUG_TAG);
    		res = res - 1;
    	} else {
    		matchedMd5 = "not_found";
    		Log.e(DEBUG_TAG, "_online md5sum not found!");
    	}
    	
    	compRes = Utils.VersionComparator.compare(matchedVersion, currentVersion);
    	Utils.logger("d", "version comparison: " + matchedVersion + " " + compRes + " " + currentVersion, DEBUG_TAG);
    	
    	return res;
    }
	
	void callDownloadApk(String ver) {
		String apklink = "http://sourceforge.net/projects/ytdownloader/files/dentex.youtube.downloader_v" + ver + ".apk/download";
		apkFilename = "dentex.youtube.downloader_v" + ver + ".apk";
		downloadManager = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
	    Request request = new Request(Uri.parse(apklink));
	    fileUri = Uri.parse(dir.toURI() + apkFilename);
	    request.setDestinationUri(fileUri);
	    request.allowScanningByMediaScanner();
	    request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
	    request.setTitle("YouTube Downloader v" + ver);
	    enqueue = downloadManager.enqueue(request);
	}

	BroadcastReceiver apkReceiver = new BroadcastReceiver() {

		@Override
        public void onReceive(final Context context, final Intent intent) {
	        long id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -2);
	        if (enqueue != -1 && id != -2 && id == enqueue) {
	            Query query = new Query();
	            query.setFilterById(id);
	            DownloadManager dm = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
	            Cursor c = dm.query(query);
	            if (c.moveToFirst()) {
	                int columnIndex = c.getColumnIndex(DownloadManager.COLUMN_STATUS);
	                int status = c.getInt(columnIndex);
	                if (status == DownloadManager.STATUS_SUCCESSFUL) {
                    	
                    	upgradeButton.setText(getString(R.string.upgrade_button_init));
                    	upgradeButton.setEnabled(true);
                    	
                    	if (Utils.checkMD5(matchedMd5, new File(dir, apkFilename))) {
                    	
	                        AlertDialog.Builder helpBuilder = new AlertDialog.Builder(context);
	                        helpBuilder.setIcon(android.R.drawable.ic_dialog_info);
	                        helpBuilder.setTitle(getString(R.string.information));
	                        helpBuilder.setMessage(getString(R.string.upgraded_dialog_msg));
	                        helpBuilder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
	
	                            public void onClick(DialogInterface dialog, int which) {
	
	                                Intent intent = new Intent();
	                                intent.setAction(android.content.Intent.ACTION_VIEW);
	                            	intent.setDataAndType(fileUri, "application/vnd.android.package-archive");
	                            	startActivity(intent);
	                            }
	                        });
	
	                        helpBuilder.setNegativeButton(getString(R.string.upgraded_dialog_negative), new DialogInterface.OnClickListener() {
	
	                            public void onClick(DialogInterface dialog, int which) {
	                            	// cancel
	                            }
	                        });
	
	                        AlertDialog helpDialog = helpBuilder.create();
	                        if (! ((Activity) context).isFinishing()) {
	                        	helpDialog.show();
	                        }
                        
                    	} else {
                    		AlertDialog.Builder helpBuilder = new AlertDialog.Builder(context);
	                        helpBuilder.setIcon(android.R.drawable.ic_dialog_info);
	                        helpBuilder.setTitle(getString(R.string.information));
	                        helpBuilder.setMessage(getString(R.string.upgrade_bad_md5_dialog_msg));
	                        helpBuilder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
	                        	
	                            public void onClick(DialogInterface dialog, int which) {
	                            	deleteBadDownload(context, intent);
	                            	callDownloadApk(matchedVersion);
	                            	upgradeButton.setEnabled(false);
	                            }
	                        });
	
	                        helpBuilder.setNegativeButton(getString(R.string.dialogs_negative), new DialogInterface.OnClickListener() {
	
	                            public void onClick(DialogInterface dialog, int which) {
	                            	deleteBadDownload(context, intent);
	                            	// cancel
	                            }
	                        });

	                        AlertDialog helpDialog = helpBuilder.create();
	                        if (! ((Activity) context).isFinishing()) {
	                        	helpDialog.show();
	                        }
                    	}
                    } else if (status == DownloadManager.STATUS_FAILED) {
                    	deleteBadDownload(context, intent);
                    }
                }
            }
		}

		public void deleteBadDownload(final Context context, final Intent intent) {
			long id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -2);
			downloadManager.remove(id);
			Toast.makeText(context, getString(R.string.download_failed), Toast.LENGTH_LONG).show();
		}

	};
}
