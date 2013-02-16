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
import android.widget.ProgressBar;
import android.widget.TextView;
import dentex.youtube.downloader.utils.Utils;

public class UpgradeApkActivity extends Activity {
	//TODO
	//private static final int YTD_SIG_HASH = -1892118308; // final string
	
	private static final int YTD_SIG_HASH = -118685648; // dev test desktop
	//private static final int YTD_SIG_HASH = 1922021506; // dev test laptop
	
	private ProgressBar progressBar2;
	private int currentHashCode;
	private String currentVersion;
	private String apkFilename;
	private static final String DEBUG_TAG = "UpgradeApkActivity";
	public TextView tv;
	public TextView cl;
	private DownloadManager downloadManager;
	File dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
	public long enqueue;
	private Uri fileUri;
	private AsyncDownload asyncDownload;
	public String onlineVersion;
	public String onlineChangelog;
	public String matchedVersion;
	public String matchedChangeLog;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_upgrade_apk);
		
		progressBar2 = (ProgressBar) findViewById(R.id.progressBar2);
        progressBar2.setVisibility(View.VISIBLE);
        
        tv = (TextView) findViewById(R.id.upgrade_textview1);
        cl = (TextView) findViewById(R.id.upgrade_textview2);
        
		registerReceiver(apkReceiver, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
		
		ConnectivityManager connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        if (networkInfo != null && networkInfo.isConnected()) {
			if (Utils.getSigHash(this) == YTD_SIG_HASH) {
				Log.d(DEBUG_TAG, "Found YTD signature: proceding with update check...");
	    		asyncDownload = new AsyncDownload();
	    		asyncDownload.execute("http://sourceforge.net/projects/ytdownloader/files/");
	    	} else {
	    		progressBar2.setVisibility(View.GONE);
	    		Log.d(DEBUG_TAG, "Found different signature: " + currentHashCode + " (F-Droid?). Update check cancelled.");
	    		showPopUp("Found different signature (F-Droid?)", "Update check cancelled.", "info");
	    	}
        } else {
        	progressBar2.setVisibility(View.GONE);
        	tv.setText(getString(R.string.no_net));
        	showPopUp(getString(R.string.no_net), getString(R.string.no_net_dialog_msg), "alert");
        }
	}

	/*private int getSigHash() {
		try {
			Signature[] sigs = getPackageManager().getPackageInfo(getPackageName(), PackageManager.GET_SIGNATURES).signatures;
			for (Signature sig : sigs) {
				currentHashCode = sig.hashCode();
			}
		} catch (NameNotFoundException e) {
		    Log.e("signature not found", e.getMessage());
		    currentHashCode = 0;
		}
		return currentHashCode;
	}*/

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

    	protected Integer doInBackground(String... urls) {
            // params comes from the execute() call: params[0] is the url.
            try {
            	Log.d(DEBUG_TAG, "doInBackground...");
                return downloadUrl(urls[0]);
            } catch (IOException e) {
                return 0;
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
        	
        	try {
			    currentVersion = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
			    Log.d(DEBUG_TAG, "current version: " + currentVersion);
			    
			    tv.setText(getString(R.string.upgrade_latest) + matchedVersion + getString(R.string.upgrade_installed) + currentVersion);
	        	cl.setText(matchedChangeLog);
			    
			    String res = Utils.VersionComparator.compare(matchedVersion, currentVersion);
		    	Log.d(DEBUG_TAG, "version comparison: " + matchedVersion + " " + res + " " + currentVersion);
		    	
		    	if (res.contentEquals(">")) {
		    		
		        	AlertDialog.Builder helpBuilder = new AlertDialog.Builder(UpgradeApkActivity.this);
		        	helpBuilder.setIcon(android.R.drawable.ic_dialog_alert);
		        	helpBuilder.setTitle(getString(R.string.information));
		        	helpBuilder.setMessage(getString(R.string.upgrade_dialog_msg) + matchedVersion);
		            helpBuilder.setPositiveButton(getString(R.string.upgrade_dialog_positive), new DialogInterface.OnClickListener() {
		
		                public void onClick(DialogInterface dialog, int which) {
		                	Log.d(DEBUG_TAG, "version comparison: OK - downloading latest version...");
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
		    		Utils.showPopUp(getString(R.string.information), getString(R.string.upgrade_latest_installed), "info", UpgradeApkActivity.this); //TODO
		    		Log.d(DEBUG_TAG, "version comparison: latest version is already installed!");
		    	} else {
		    		// No need for a popup...
		    		Log.d(DEBUG_TAG, "installed version higher than the one online? ...this should not happen...");
		    	}
		    	
			} catch (NameNotFoundException e) {
			    Log.e("version not read", e.getMessage());
			    currentVersion = "100";
			} catch (NullPointerException e) {
				Utils.showPopUp(getString(R.string.error), getString(R.string.upgrade_network_error), "alert", UpgradeApkActivity.this); //TODO
				Log.d(DEBUG_TAG, "unable to retrieve update data");
			}
        }   
	}
	
	private int OnlineUpdateCheck(String content) {
		Pattern pattern = Pattern.compile("versionName=\\\"(.*)\\\"");
        Matcher matcher = pattern.matcher(content);
        if (matcher.find()) {
        	matchedVersion = matcher.group(1);
	    	Log.d(DEBUG_TAG, "on-line version: " + matchedVersion);
	    	
	    	Pattern cl_pattern = Pattern.compile("<pre><code> v(.*?)</code></pre>", Pattern.DOTALL);
	    	Matcher cl_matcher = cl_pattern.matcher(content);
	    	if (cl_matcher.find()) {
	    		matchedChangeLog = " v" + cl_matcher.group(1);
	    		Log.d(DEBUG_TAG, "found new change-log...");
	    		return 0;
	    	} else {
	    		matchedChangeLog = "not_found";
	    		Log.d(DEBUG_TAG, "no_changelog");
	    		return 2;
	    	}
	    } else {
        	matchedVersion = "not_found";
        	Log.d(DEBUG_TAG, "on-line version: not_found");
        	return 1;
        }
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
		Log.d(DEBUG_TAG, "apk file enqueued");
	
	}

	BroadcastReceiver apkReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
            if (DownloadManager.ACTION_DOWNLOAD_COMPLETE.equals(action)) {
            	Query query = new Query();
                query.setFilterById(enqueue);
                Cursor c = downloadManager.query(query);
                if (c.moveToFirst()) {
                    int columnIndex = c.getColumnIndex(DownloadManager.COLUMN_STATUS);
                    if (DownloadManager.STATUS_SUCCESSFUL == c.getInt(columnIndex)) {
                        AlertDialog.Builder helpBuilder = new AlertDialog.Builder(context);
                        helpBuilder.setIcon(android.R.drawable.ic_dialog_info);
                        helpBuilder.setTitle(getString(R.string.information));
                        helpBuilder.setMessage(getString(R.string.upgraded_dialog_msg));
                        helpBuilder.setPositiveButton("OK", new DialogInterface.OnClickListener() {

                            public void onClick(DialogInterface dialog, int which) {
                                // TODO titolo e msg dialogo download completo
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
                    }
                }
            }
		}
	};

	private int icon;
	
	private void showPopUp(String title, String message, String type) {
        AlertDialog.Builder helpBuilder = new AlertDialog.Builder(this);
        helpBuilder.setTitle(title);
        helpBuilder.setMessage(message);

        if ( type == "alert" ) {
            icon = android.R.drawable.ic_dialog_alert;
        } else if ( type == "info" ) {
            icon = android.R.drawable.ic_dialog_info;
        }

        helpBuilder.setIcon(icon);
        helpBuilder.setPositiveButton("OK", new DialogInterface.OnClickListener() {

            public void onClick(DialogInterface dialog, int which) {
                // Do nothing but close the dialog
            }
        });

        AlertDialog helpDialog = helpBuilder.create();
        helpDialog.show();
    }
}
