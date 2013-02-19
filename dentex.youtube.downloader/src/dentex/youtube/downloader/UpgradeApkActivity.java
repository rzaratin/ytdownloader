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
	public long enqueue;
	private Uri fileUri;
	private AsyncDownload asyncDownload;
	public String onlineVersion;
	public String onlineChangelog;
	public String matchedVersion;
	public String matchedChangeLog;
	public String matchedMd5;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_upgrade_apk);
		
		try {
		    currentVersion = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
		    Log.d(DEBUG_TAG, "current version: " + currentVersion);
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
        
		registerReceiver(apkReceiver, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
		
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
					
					asyncDownload = new AsyncDownload();
					asyncDownload.execute("http://sourceforge.net/projects/ytdownloader/files/");
				} else {
					buttonClickedOnce = false;
					String res = Utils.VersionComparator.compare(matchedVersion, currentVersion);
			    	Log.d(DEBUG_TAG, "version comparison: " + matchedVersion + " " + res + " " + currentVersion);
			    	
			    	if (res.contentEquals(">")) {
				        Log.d(DEBUG_TAG, "version comparison: downloading latest version...");
					    callDownloadApk(matchedVersion);
					    upgradeButton.setEnabled(false);
			    	} else if (res.contentEquals("==")) {
			    		Utils.showPopUp(getString(R.string.information), getString(R.string.upgrade_latest_installed), "info", UpgradeApkActivity.this);
			    		Log.d(DEBUG_TAG, "version comparison: latest version is already installed!");
			    	} else {
			    		// No need for a popup...
			    		Log.d(DEBUG_TAG, "version comparison: installed higher than the one online? ...this should not happen...");
			    	}
				}
			} catch (NullPointerException e) {
				Utils.showPopUp(getString(R.string.error), getString(R.string.upgrade_network_error), "alert", UpgradeApkActivity.this);
				Log.e(DEBUG_TAG, "unable to retrieve update data.");
			}
		} else {
			progressBar2.setVisibility(View.GONE);
			tv.setText(getString(R.string.no_net));
			//upgradeButton.setEnabled(false);
			Utils.showPopUp(getString(R.string.no_net), getString(R.string.no_net_dialog_msg), "alert", this);
		}
	}
	
	@Override
    protected void onResume() {
        super.onResume();
        registerReceiver(apkReceiver, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
    }

    @Override
    protected void onStop() {
        super.onStop();
    	unregisterReceiver(apkReceiver);
    	Log.d(DEBUG_TAG, "apkReceiver unregistered_onStop");
    }
	
	private class AsyncDownload extends AsyncTask<String, Void, Integer> {
		
		protected void onPreExecute() {
			progressBar2.setVisibility(View.VISIBLE);
			tv.setText(R.string.upgrade_uppertext_searching);
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
                return readIt(is, len);
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
        	upgradeButton.setText(getString(R.string.upgrade_button_clicked));
        	
        	//try {
			    /*currentVersion = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
			    Log.d(DEBUG_TAG, "current version: " + currentVersion);*/
			
        	tv.setText(getString(R.string.upgrade_latest) + matchedVersion + getString(R.string.upgrade_installed) + currentVersion);
	        cl.setText(matchedChangeLog);
			    
	        	/*String res = Utils.VersionComparator.compare(matchedVersion, currentVersion);
		    	Log.d(DEBUG_TAG, "version comparison: " + matchedVersion + " " + res + " " + currentVersion);
		    	
		    	if (res.contentEquals(">")) {
		    		
		        	AlertDialog.Builder helpBuilder = new AlertDialog.Builder(UpgradeApkActivity.this);
		        	helpBuilder.setIcon(android.R.drawable.ic_dialog_alert);
		        	helpBuilder.setTitle(getString(R.string.information));
		        	helpBuilder.setMessage(getString(R.string.upgrade_dialog_msg) + matchedVersion);
		            helpBuilder.setPositiveButton(getString(R.string.upgrade_dialog_positive), new DialogInterface.OnClickListener() {
		
		                public void onClick(DialogInterface dialog, int which) {
		                	Log.d(DEBUG_TAG, "version comparison: downloading latest version...");
				    		callDownloadApk(matchedVersion);
		                }
		            });
		            
		            helpBuilder.setNegativeButton(getString(R.string.dialogs_negative), new DialogInterface.OnClickListener() {
		
		                public void onClick(DialogInterface dialog, int which) {
		                	// close
		                }
		            });
		
		            AlertDialog helpDialog = helpBuilder.create();
		            helpDialog.show();
            
		    	} else if (res.contentEquals("==")) {
		    		Utils.showPopUp(getString(R.string.information), getString(R.string.upgrade_latest_installed), "info", UpgradeApkActivity.this);
		    		Log.d(DEBUG_TAG, "version comparison: latest version is already installed!");
		    	} else {
		    		// No need for a popup...
		    		Log.d(DEBUG_TAG, "version comparison: installed higher than the one online? ...this should not happen...");
		    	}*/
		    	
			/*} catch (NameNotFoundException e) {
			    Log.e(DEBUG_TAG, "version not read: " + e.getMessage());
			    currentVersion = "100";*/
			/*} catch (NullPointerException e) {
				Utils.showPopUp(getString(R.string.error), getString(R.string.upgrade_network_error), "alert", UpgradeApkActivity.this);
				Log.e(DEBUG_TAG, "unable to retrieve update data: " + e.getMessage());
			}*/
        }   
	}
	
	private int OnlineUpdateCheck(String content) {
		int res = 3;
		
		// match version name
		Pattern v_pattern = Pattern.compile("versionName=\\\"(.*)\\\"");
        Matcher v_matcher = v_pattern.matcher(content);
        if (v_matcher.find()) {
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
    	if (cl_matcher.find()) {
    		matchedChangeLog = " v" + cl_matcher.group(1);
    		Log.i(DEBUG_TAG, "_online changelog...");
    		res = res - 1;
    	} else {
    		matchedChangeLog = "not_found";
    		Log.e(DEBUG_TAG, "_online changelog not found!");
    	}
    	
    	// match md5
    	// checksum: <code>d7ef1e4668b24517fb54231571b4a74f</code> dentex.youtube.downloader_v
    	Pattern md5_pattern = Pattern.compile("checksum: <code>(.{32})</code> dentex.youtube.downloader_v");
    	Matcher md5_matcher = md5_pattern.matcher(content);
    	if (md5_matcher.find()) {
    		matchedMd5 = md5_matcher.group(1);
    		Log.i(DEBUG_TAG, "_online md5sum: " + matchedMd5);
    		res = res - 1;
    	} else {
    		matchedMd5 = "not_found";
    		Log.e(DEBUG_TAG, "_online md5sum not found!");
    	}
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
			String action = intent.getAction();
            if (DownloadManager.ACTION_DOWNLOAD_COMPLETE.equals(action)) {
            	Query query = new Query();
                query.setFilterById(enqueue);
                Cursor c = downloadManager.query(query);
                if (c.moveToFirst()) {
                	int columnIndex = c.getColumnIndex(DownloadManager.COLUMN_STATUS);
                    int status = c.getInt(columnIndex);
                    if (status == DownloadManager.STATUS_SUCCESSFUL) {
                    	
                    	upgradeButton.setText(getString(R.string.upgrade_button_init));
                    	upgradeButton.setEnabled(true);
                    	
                    	if (Utils.checkMD5(matchedMd5, new File(dir + "/" + apkFilename))) {
                    	
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
	                        helpDialog.show();
                        
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
	                        helpDialog.show();
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
			Toast.makeText(context, "Failed download cancelled", Toast.LENGTH_LONG).show();
		}

	};
}
