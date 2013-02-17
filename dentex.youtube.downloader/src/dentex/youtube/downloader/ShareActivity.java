package dentex.youtube.downloader;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.app.DownloadManager.Query;
import android.app.DownloadManager.Request;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.media.MediaScannerConnection;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import dentex.youtube.downloader.SettingsActivity.SettingsFragment;
import dentex.youtube.downloader.utils.Utils;

public class ShareActivity extends Activity {
	
	private ProgressBar progressBar1;
    public static final String USER_AGENT_FIREFOX = "Mozilla/5.0 (Windows; U; Windows NT 6.0; en-US; rv:1.9.0.10) Gecko/2009042316 Firefox/3.0.10 (.NET CLR 3.5.30729)";
    private static final String DEBUG_TAG = "ShareActivity";
    private TextView tv;
    private ListView lv;
    //private InputStream isFromString;
    List<String> links = new ArrayList<String>();
    List<String> codecs = new ArrayList<String>();
    List<String> qualities = new ArrayList<String>();
    List<String> sizes = new ArrayList<String>();
    List<String> cqsChoices = new ArrayList<String>();
    private String titleRaw;
    private String title;
    public int pos;
    public File path;
    public String validatedLink;
    private DownloadManager downloadManager;
    private long enqueue;
	String vfilename = "video";
	String composedFilename = "";
    private Uri videoUri;
    private int icon;
	public CheckBox showAgain1;
	public CheckBox showAgain2;
	public TextView userFilename;
	public static SharedPreferences settings;
	public static final String PREFS_NAME = "dentex.youtube.downloader_preferences";
	public final File dir_Downloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
	public final File dir_DCIM = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM);
	public final File dir_Movies = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES);
	boolean sshInfoCheckboxEnabled;
	boolean generalInfoCheckboxEnabled;
	boolean fileRenameEnabled;
	public File chooserFolder;
	private AsyncDownload asyncDownload;
	public String videoFileSize = "empty";
	private AsyncSizeQuery sizeQuery;
	public AlertDialog helpDialog;
	private AlertDialog.Builder  helpBuilder;
	protected boolean downloadingApk = false;
	public static String onlineVersion;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_share);
        
    	settings = getSharedPreferences(PREFS_NAME, 0);
        
        progressBar1 = (ProgressBar) findViewById(R.id.progressBar1);
        progressBar1.setVisibility(View.VISIBLE);
        
        lv = (ListView) findViewById(R.id.list);
        tv = (TextView) findViewById(R.id.textView1);
        
        
        // Get intent, action and MIME type
        Intent intent = getIntent();
        String action = intent.getAction();
        String type = intent.getType();

        if (Intent.ACTION_SEND.equals(action) && type != null) {
            if ("text/plain".equals(type)) {
                try {
                    handleSendText(intent);
                } catch (IOException e) {
                    e.printStackTrace();
                    Log.d(DEBUG_TAG, "Error: " + e.toString());
                }
            }
        }
        registerReceiver(completeReceiver, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
        registerReceiver(clickReceiver, new IntentFilter(DownloadManager.ACTION_NOTIFICATION_CLICKED));
        //registerReceiver(apkReceiver, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_share, menu);
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        super.onOptionsItemSelected(item);
        switch(item.getItemId()){
        	case R.id.menu_settings:
        		startActivity(new Intent(this, SettingsActivity.class));
        	return true;
        	case R.id.menu_dm:
        		startActivity(new Intent(android.app.DownloadManager.ACTION_VIEW_DOWNLOADS));
        	return true;
        	default:
        		return super.onOptionsItemSelected(item);
        }

    }
 
    
    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(completeReceiver, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
        registerReceiver(clickReceiver, new IntentFilter(DownloadManager.ACTION_NOTIFICATION_CLICKED));
        //registerReceiver(apkReceiver, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
    }

    @Override
    protected void onStop() {
        super.onStop();
    	unregisterReceiver(completeReceiver);
    	unregisterReceiver(clickReceiver);
    	//unregisterReceiver(apkReceiver);
    	Log.d(DEBUG_TAG, "Receivers unregistered_onStop");
    }


    void handleSendText(Intent intent) throws IOException {

        ConnectivityManager connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        if (networkInfo != null && networkInfo.isConnected()) {
            String sharedText = intent.getStringExtra(Intent.EXTRA_TEXT);
            if (linkValidator(sharedText) == "not_a_valid_youtube_link") {
            	progressBar1.setVisibility(View.GONE);
            	tv.setText(getString(R.string.bad_link));
            	Utils.showPopUp(getString(R.string.error), getString(R.string.bad_link_dialog_msg), "alert", this);
            } else if (sharedText != null) {
            	showGeneralInfoTutorial();
            	// YouTube job
            	asyncDownload = new AsyncDownload();
            	asyncDownload.execute(validatedLink);
            }
        } else {
        	progressBar1.setVisibility(View.GONE);
        	tv.setText(getString(R.string.no_net));
        	Utils.showPopUp(getString(R.string.no_net), getString(R.string.no_net_dialog_msg), "alert", this);
        }
    }
    
    void showGeneralInfoTutorial() {
        generalInfoCheckboxEnabled = settings.getBoolean("general_info", true);
        if (generalInfoCheckboxEnabled == true) {
        	AlertDialog.Builder adb = new AlertDialog.Builder(ShareActivity.this);
    	    LayoutInflater adbInflater = LayoutInflater.from(ShareActivity.this);
    	    View generalInfo = adbInflater.inflate(R.layout.dialog_general_info, null);
    	    showAgain1 = (CheckBox) generalInfo.findViewById(R.id.showAgain1);
    	    showAgain1.setChecked(true);
    	    adb.setView(generalInfo);
    	    adb.setTitle(getString(R.string.tutorial_title));    	    
    	    adb.setMessage(getString(R.string.tutorial_msg));

    	    adb.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
    	    	public void onClick(DialogInterface dialog, int which) {
    	    		if (showAgain1.isChecked() == false) {
    	    			SharedPreferences.Editor editor = settings.edit();
    	    			editor.putBoolean("general_info", false);
    	    			editor.commit();
    	    			sshInfoCheckboxEnabled = settings.getBoolean("general_info", true);
    	    			Log.d(DEBUG_TAG, "generalInfoCheckboxEnabled: " + generalInfoCheckboxEnabled);
    	    		}
        		}
        	});
    	    adb.show();
        }
    }
    
    private String linkValidator(String sharedText) {
    	String link = sharedText;
    	Pattern pattern = Pattern.compile("(http|https).*(v=.{11}).*");
        Matcher matcher = pattern.matcher(link);
        if (matcher.find()) {
            validatedLink = matcher.group(1) + "://www.youtube.com/watch?" + matcher.group(2);
            return validatedLink;
        }
        return "not_a_valid_youtube_link";
    }
    
    /* Checks if external storage is available for read and write */
    public boolean pathCheckOK() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state) && path.canWrite()) {
        	Pattern extPattern = Pattern.compile(SettingsFragment.EXT_CARD_NAMES);
        	Matcher extMatcher = extPattern.matcher(path.toString());
        	if (extMatcher.find()) {
        		return false;
        	} else {
        		return true;
        	}
        } else {
        	return false;
        }
    }
    
    public void assignPath() {
    	boolean Location = settings.getBoolean("swap_location", false);
        
        if (Location == false) {
            String location = settings.getString("standard_location", "Downloads");
            Log.d(DEBUG_TAG, "location: " + location);
            
            if (location.equals("DCIM") == true) {
            	path = dir_DCIM;
            }
            if (location.equals("Movies") == true) {
            	path = dir_Movies;
            } 
            if (location.equals("Downloads") == true) {
            	path = dir_Downloads;
            }
            
        } else {
        	String cs = settings.getString("CHOOSER_FOLDER", "");
        	chooserFolder = new File(cs);
        	Log.d(DEBUG_TAG, "chooserFolder: " + chooserFolder);
        	path = chooserFolder;
        }
        Log.d(DEBUG_TAG, "path: " + path);
    }

    private class AsyncDownload extends AsyncTask<String, Void, String> {

    	protected String doInBackground(String... urls) {
            // params comes from the execute() call: params[0] is the url.
            try {
            	Log.d(DEBUG_TAG, "doInBackground...");
                return downloadUrl(urls[0]);
            } catch (IOException e) {
                return "e";
            }
        }

        // onPostExecute displays the results of the AsyncTask.
        @Override
        protected void onPostExecute(String result) {

        	progressBar1.setVisibility(View.GONE);
        	
            if (result == "e") {
            	tv.setText(getString(R.string.invalid_url_short));
                Utils.showPopUp(getString(R.string.error), getString(R.string.invalid_url), "alert", ShareActivity.this);
            }

            final String[] lv_arr = cqsChoices.toArray(new String[0]);
            lv.setAdapter(new ArrayAdapter<String>(ShareActivity.this, android.R.layout.simple_list_item_1, lv_arr));
            Log.d(DEBUG_TAG, "LISTview done with " + lv_arr.length + " items.");

            tv.setText(titleRaw);
            lv.setOnItemClickListener(new OnItemClickListener() {
            	
            	//private String currentSize;

				public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
					
					assignPath();
                    //createLogFile(stringToIs(links[position]), "ytd_FINAL_LINK.txt");
					
                    pos = position;                    
                    helpBuilder = new AlertDialog.Builder(ShareActivity.this);
                    
                    helpBuilder.setIcon(android.R.drawable.ic_dialog_info);
                    helpBuilder.setTitle(getString(R.string.list_click_dialog_title));
                    
                    if (settings.getBoolean("show_size", false) == false) {
                    	helpBuilder.setMessage(titleRaw + 
        						"\n\n\tCodec: " + codecs.get(position) + 
        						"\n\tQuality: " + qualities.get(position));
                    } else {
                    	sizeQuery = new AsyncSizeQuery();
                    	sizeQuery.execute(links.get(position));
                    }

                    helpBuilder.setPositiveButton(getString(R.string.list_click_download_local), new DialogInterface.OnClickListener() {

                        public void onClick(DialogInterface dialog, int which) {
                        	if (pathCheckOK() == true) {
                            	Log.d(DEBUG_TAG, "Destination folder is available and writable");
                        		composedFilename = composeFilename();
	                            fileRenameEnabled = settings.getBoolean("enable_rename", false);

	                            if (fileRenameEnabled == true) {
	                            	AlertDialog.Builder adb = new AlertDialog.Builder(ShareActivity.this);
	                            	LayoutInflater adbInflater = LayoutInflater.from(ShareActivity.this);
		                    	    View inputFilename = adbInflater.inflate(R.layout.dialog_input_filename, null);
		                    	    userFilename = (TextView) inputFilename.findViewById(R.id.input_filename);
		                    	    userFilename.setText(title);
		                    	    adb.setView(inputFilename);
		                    	    adb.setTitle(getString(R.string.rename_dialog_title));
		                    	    adb.setMessage(getString(R.string.rename_dialog_msg));
		                    	    adb.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
		                    	    	public void onClick(DialogInterface dialog, int which) {
		                    	    		title = userFilename.getText().toString();
		                    	    		composedFilename = composeFilename();
											callDownloadManager(links.get(pos));
		                    	    	}
		                    	    });
		                    	    adb.show();
	                            } else {
									callDownloadManager(links.get(pos));
	                            }
                            } else {
                            	Log.d(DEBUG_TAG, "Destination folder is NOT available and/or NOT writable");
                            	Utils.showPopUp(getString(R.string.unable_save), getString(R.string.unable_save_dialog_msg), "alert", ShareActivity.this);
                            }
                        }
                    });

                    helpBuilder.setNeutralButton(getString(R.string.list_click_download_ssh), new DialogInterface.OnClickListener() {

                        public void onClick(DialogInterface dialog, int which) {
                        	String wgetCmd;
                        	composedFilename = composeFilename();
                        	
                        	wgetCmd = "REQ=`wget -q -e \"convert-links=off\" --keep-session-cookies --save-cookies /dev/null --no-check-certificate \'" + 
                        			validatedLink + "\' -O-` && urlblock=`echo $REQ | grep -oE \'url_encoded_fmt_stream_map\": \".*\' | sed -e \'s/\", \".*//\'" + 
                        			" -e \'s/url_encoded_fmt_stream_map\": \"//\'` && urlarray=( `echo $urlblock | sed \'s/,/\\n\\n/g\'` ) && N=" + pos + 
                        			" && block=`echo \"${urlarray[$N]}\" | sed -e \'s/%3A/:/g\' -e \'s/%2F/\\//g\' -e \'s/%3F/\\?/g\' -e \'s/%3D/\\=/g\'" + 
                        			" -e \'s/%252C/%2C/g\' -e \'s/%26/\\&/g\' -e \'s/%253A/\\:/g\' -e \'s/\", \"/\"-\"/\' -e \'s/sig=/signature=/\'" + 
                        			" -e \'s/x-flv/flv/\' -e \'s/\\\\\\u0026/\\&/g\'` && url=`echo $block | grep -oE \'http://.*\' | sed -e \'s/&type=.*//\'" + 
                        			" -e \'s/&signature=.*//\' -e \'s/&quality=.*//\' -e \'s/&fallback_host=.*//\'` &&" + 
                        			" sig=`echo $block | grep -oE \'signature=.{81}\'` && downloadurl=`echo $url\\&$sig | sed \'s/&itag=[0-9][0-9]&signature/\\&signature/\'` && wget -e \"convert-links=off\"" +
                        			" --keep-session-cookies --save-cookies /dev/null --tries=5 --timeout=45 --no-check-certificate \"$downloadurl\" -O " + 
                        			composedFilename;
                        	
                        	//Log.d(DEBUG_TAG, "wgetCmd: " + wgetCmd);
                            
                        	ClipData cmd = ClipData.newPlainText("simple text", wgetCmd);
                            ClipboardManager cb = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                            cb.setPrimaryClip(cmd);
                            
                            sshInfoCheckboxEnabled = settings.getBoolean("ssh_info", true);
                            if (sshInfoCheckboxEnabled == true) {
	                            AlertDialog.Builder adb = new AlertDialog.Builder(ShareActivity.this);
	                    	    LayoutInflater adbInflater = LayoutInflater.from(ShareActivity.this);
	                    	    View sshInfo = adbInflater.inflate(R.layout.dialog_ssh_info, null);
	                    	    showAgain2 = (CheckBox) sshInfo.findViewById(R.id.showAgain2);
	                    	    showAgain2.setChecked(true);
	                    	    adb.setView(sshInfo);
	                    	    adb.setTitle(getString(R.string.ssh_info_tutorial_title));
	                    	    adb.setMessage(getString(R.string.ssh_info_tutorial_msg));
	                    	    adb.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
	                    	    	public void onClick(DialogInterface dialog, int which) {
	                    	    		if (showAgain2.isChecked() == false) {
	                    	    			SharedPreferences.Editor editor = settings.edit();
	                    	    			editor.putBoolean("ssh_info", false);
	                    	    			editor.commit();
	                    	    			sshInfoCheckboxEnabled = settings.getBoolean("ssh_info", true);
	                    	    			Log.d(DEBUG_TAG, "sshInfoCheckboxEnabled: " + sshInfoCheckboxEnabled);
	                    	    		}
	                    	    		callConnectBot(); 
	                        		}
	                        	});
	                    	    adb.show();
                    	    } else {
                    	    	callConnectBot();
                    	    }
                        }
                    });

                    helpBuilder.setNegativeButton(getString(R.string.dialogs_negative), new DialogInterface.OnClickListener() {

                        public void onClick(DialogInterface dialog, int which) {
                            //Toast.makeText(ShareActivity.this, "Download canceled...", Toast.LENGTH_SHORT).show();
                        }
                    });
                    if (settings.getBoolean("show_size", false) == false) {
                    	helpDialog = helpBuilder.create();
                    	helpDialog.show();
                    }
                    
                }
            });
        }
        
        public boolean useQualitySuffix() {
        	boolean qualitySuffixEnabled = settings.getBoolean("enable_q_suffix", true);
        	if (qualitySuffixEnabled == true) {
        		return true;
        	} else {
        		return false;
        	}
        }
        
        public String composeFilename() {
        	vfilename = title + "_" + qualities.get(pos) + "." + codecs.get(pos);
    	    if (useQualitySuffix() == false) vfilename = title + "." + codecs.get(pos);
    	    Log.d(DEBUG_TAG, "filename: " + vfilename);
    	    return vfilename;
        }

		void callConnectBot() {
        	Context context = getApplicationContext();
    		PackageManager pm = context.getPackageManager();
    		Intent appStartIntent = pm.getLaunchIntentForPackage("org.connectbot");
    		if (null != appStartIntent) {
    			Log.d(DEBUG_TAG, "appStartIntent: " + appStartIntent);
    			context.startActivity(appStartIntent);
    		} else {
    			AlertDialog.Builder cb = new AlertDialog.Builder(ShareActivity.this);
    	        cb.setTitle(getString(R.string.callConnectBot_dialog_title));
    	        cb.setMessage(getString(R.string.callConnectBot_dialog_msg));
    	        icon = android.R.drawable.ic_dialog_alert;
    	        cb.setIcon(icon);
    	        cb.setPositiveButton(getString(R.string.callConnectBot_dialog_positive), new DialogInterface.OnClickListener() {
    	            public void onClick(DialogInterface dialog, int which) {
    	            	Intent intent = new Intent(Intent.ACTION_VIEW); 
    	            	intent.setData(Uri.parse("market://details?id=org.connectbot"));
    	            	try {
    	            		startActivity(intent);
    	            	} catch (ActivityNotFoundException exception){
    	            		Utils.showPopUp(getString(R.string.no_market), getString(R.string.no_net_dialog_msg), "alert", ShareActivity.this);
    	            	}
    	            }
    	        });
    	        cb.setNegativeButton(getString(R.string.dialogs_negative), new DialogInterface.OnClickListener() {
    	        	public void onClick(DialogInterface dialog, int which) {
    	                // Do nothing but close the dialog
    	            }
    	        });

    	        AlertDialog helpDialog = cb.create();
    	        helpDialog.show();
    		}
        }
	}
    
    void callDownloadManager(String link) {
        downloadManager = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
        Request request = new Request(Uri.parse(link));
		videoUri = Uri.parse(path.toURI() + composedFilename);
        Log.d(DEBUG_TAG, "downloadedVideoUri: " + videoUri);
        request.setDestinationUri(videoUri);
        request.allowScanningByMediaScanner();
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        request.setTitle(vfilename);
    	enqueue = downloadManager.enqueue(request);
    	downloadingApk = false;
    }

    // Given a URL, establishes an HttpUrlConnection and retrieves
    // the web page content as a InputStream, which it returns as a string.
    private String downloadUrl(String myurl) throws IOException {
        InputStream is = null;
        // Only display the first "len" characters of the retrieved web page content.
        int len = 2000000;
        Log.d(DEBUG_TAG, "The link is: " + myurl);
        try {
            URL url = new URL(myurl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestProperty("User-Agent","<em>" + USER_AGENT_FIREFOX + "</em>");
            conn.setReadTimeout(20000 /* milliseconds */);
            conn.setConnectTimeout(30000 /* milliseconds */);
            conn.setInstanceFollowRedirects(false);
            conn.setRequestMethod("GET");
            conn.setDoInput(true);
            //Starts the query
            conn.connect();
            int response = conn.getResponseCode();
            Log.d(DEBUG_TAG, "The response is: " + response);
            is = conn.getInputStream();

            //Convert the InputStream into a string
            return readIt(is, len);

            //Makes sure that the InputStream is closed after the app is finished using it.
        } finally {
            if (is != null) {
                is.close();
            }
        }
    }
    
    // Reads an InputStream and converts it to a String.
    public String readIt(InputStream stream, int len) throws IOException, UnsupportedEncodingException {
        Reader reader = null;
        reader = new InputStreamReader(stream, "UTF-8");
        char[] buffer = new char[len];
        reader.read(buffer);
        String content = new String(buffer);
       	return urlBlockMatchAndDecode(content);
    }

	public String urlBlockMatchAndDecode(String content) {

        findVideoFilename(content);

        Pattern startPattern = Pattern.compile("url_encoded_fmt_stream_map\\\": \\\"");
        Pattern endPattern = Pattern.compile("\\\", \\\"");
        Matcher matcher = startPattern.matcher(content);
        if (matcher.find()) {
            try {
                String[] start = content.split(startPattern.toString());
                String[] end = start[1].split(endPattern.toString());

                // Other decoding Stuff
                String contentDecoded = URLDecoder.decode(end[0], "UTF-8");
                contentDecoded = contentDecoded.replaceAll(", ", "-");
                contentDecoded = contentDecoded.replaceAll("sig=", "signature=");
                contentDecoded = contentDecoded.replaceAll("x-flv", "flv");
                contentDecoded = contentDecoded.replaceAll("\\\\u0026", "&");
                //Log.d(DEBUG_TAG, "contentDecoded: " + contentDecoded);
                findCodecAndQualityAndLinks(contentDecoded);

            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            //createLogFile(stringToIs(Arrays.toString(links)), "ytd_links.txt");
            //createLogFile(stringToIs(Arrays.toString(codecs.toArray())), "ytd_codecs.txt");
            //createLogFile(stringToIs(Arrays.toString(qualities.toArray())), "ytd_qualities.txt");
            return "Match!";
        } else {
            return "No Match";
        }
    }

	private void findVideoFilename(String content) {
        Pattern videoPatern = Pattern.compile("<title>(.*?)</title>");
        Matcher matcher = videoPatern.matcher(content);
        if (matcher.find()) {
            titleRaw = matcher.group().replaceAll("(<| - YouTube</)title>", "").replaceAll("&quot;", "\"").replaceAll("&amp;", "&").replaceAll("&#39;", "'");
            title = titleRaw.replaceAll("\\W", "_");
        } else {
            title = "Youtube Video";
        }
        Log.d(DEBUG_TAG, "findVideoFilename: " + title);
    }

    private void findCodecAndQualityAndLinks(String contentDecoded) {
        Pattern trimPattern = Pattern.compile(",");
        Matcher matcher = trimPattern.matcher(contentDecoded);
        if (matcher.find()) {
            String[] CQS = contentDecoded.split(trimPattern.toString());
            Log.d(DEBUG_TAG, "number of CQ found: " + (CQS.length-1));
            int index = 0;
            while ((index+1) < CQS.length) {
                codecMatcher(CQS[index], index);
                qualityMatcher(CQS[index], index);
                linksComposer(CQS[index], index);
                //Log.d(DEBUG_TAG, "block " + index + ": " + CQS[index]);
                index++;
            }
            cqsChoicesBuilder();
        }
    }

    private void cqsChoicesBuilder() {
        Iterator<String> codecsIter = codecs.iterator();
        Iterator<String> qualitiesIter = qualities.iterator();
        while (codecsIter.hasNext()) {
            cqsChoices.add(codecsIter.next() + "\t-\t" + qualitiesIter.next());
        }
    }

    private void linksComposer(String block, int i) {
    	Pattern urlPattern = Pattern.compile("http://.*");
    	Matcher urlMatcher = urlPattern.matcher(block);
    	if (urlMatcher.find()) {
    		Pattern sigPattern = Pattern.compile("signature=[[0-9][A-Z]]{40}\\.[[0-9][A-Z]]{40}");
    		Matcher sigMatcher = sigPattern.matcher(block);
    		if (sigMatcher.find()) {
    			String url = urlMatcher.group();
    			url = url.replaceAll("&type=.*", "");
    			url = url.replaceAll("&signature=.*", "");
    			url = url.replaceAll("&quality=.*", "");
    			url = url.replaceAll("&fallback_host=.*", "");
    			//Log.d(DEBUG_TAG, "url: " + url);
    			String sig = sigMatcher.group();
    			//Log.d(DEBUG_TAG, "sig: " + sig);
    			String linkToAdd = url + "&" + sig;
    			linkToAdd = linkToAdd.replaceAll("&itag=[0-9][0-9]&signature", "&signature");
    			links.add(linkToAdd);
    		}
    	}
	}
    
    private class AsyncSizeQuery extends AsyncTask<String, Void, String> {

		protected String doInBackground(String... urls) {
            // params comes from the execute() call: params[0] is the url.
            try {
                return getVideoFileSizeAsync(urls[0]);
            } catch (IOException e) {
                return "e";
            }
        }

        @Override
        protected void onPostExecute(String result) {
        	videoFileSize = result;
        	
        	helpBuilder.setMessage(titleRaw + 
					"\n\n\tCodec: " + codecs.get(pos) + 
					"\n\tQuality: " + qualities.get(pos) +
					"\n\tSize: " + videoFileSize);
        	helpDialog = helpBuilder.create();
            helpDialog.show();
        	
        	Log.d(DEBUG_TAG, "video File Size: " + videoFileSize);
        }
	}
    
    private String getVideoFileSizeAsync(String link) throws IOException {
		final URL uri = new URL(link);
		URLConnection ucon;
		try {
			ucon=uri.openConnection();
			ucon.connect();
			int file_size = ucon.getContentLength();
			return MakeSizeHumanReadable(file_size, true);
		} catch(final IOException e) {
			return "n.a.";
		}
	}
	
	@SuppressLint("DefaultLocale")
	private String MakeSizeHumanReadable(int bytes, boolean si) {
		String hr;
		int unit = si ? 1000 : 1024;
	    if (bytes < unit) {
	    	hr = bytes + " B";
		} else {
			int exp = (int) (Math.log(bytes) / Math.log(unit));
			String pre = (si ? "kMGTPE" : "KMGTPE").charAt(exp-1) + (si ? "" : "i");
			hr = String.format("%.1f %sB", bytes / Math.pow(unit, exp), pre);
		}
		hr = hr.replace("-1 B", "n.a.");
	    return hr;
	}

    private void codecMatcher(String currentCQ, int i) {
        Pattern codecPattern = Pattern.compile("(webm|mp4|flv|3gpp)");
        Matcher codecMatcher = codecPattern.matcher(currentCQ);
        if (codecMatcher.find()) {
            codecs.add(codecMatcher.group());
        } else {
            codecs.add("NoMatch");
        }
        //Log.d(DEBUG_TAG, "CQ index: " + i + ", Codec: " + codecs.get(i));
    }

    private void qualityMatcher(String currentCQ, int i) {
        Pattern qualityPattern = Pattern.compile("(hd1080|hd720|large|medium|small)");
        Matcher qualityMatcher = qualityPattern.matcher(currentCQ);
        if (qualityMatcher.find()) {
            qualities.add(qualityMatcher.group());
        } else {
            qualities.add("NoMatch");
        }
        //Log.d(DEBUG_TAG, "CQ index: " + i + ", Quality: " + qualities.get(i));
    }

    /*public InputStream stringToIs(String text) {
        try {
            isFromString = new ByteArrayInputStream(text.getBytes("UTF-8"));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return isFromString;
    }*/

    void createLogFile(InputStream stream, String filename) {
        File file = new File(path, filename);

        try {
            path.mkdirs();

            // If external storage is not currently mounted this will silently fail.
            InputStream is = stream;
            OutputStream os = new FileOutputStream(file);
            byte[] data = new byte[is.available()];
            is.read(data);
            os.write(data);
            is.close();
            os.close();

            MediaScannerConnection.scanFile(this,
                                            new String[] { file.toString() }, null,
            new MediaScannerConnection.OnScanCompletedListener() {
                public void onScanCompleted(String path, Uri uri) {
                    Log.i("ExternalStorage", "Scanned " + path + ":");
                    Log.i("ExternalStorage", "-> uri=" + uri);
                }
            });
        } catch (IOException e) {
            // Unable to create file, likely because external storage is not currently mounted.
            Log.w("ExternalStorage", "Error writing " + file, e);
        }
    }

    BroadcastReceiver completeReceiver = new BroadcastReceiver() {

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
                        helpBuilder.setMessage(getString(R.string.download_complete_dialog_msg1) + titleRaw + getString(R.string.download_complete_dialog_msg2));
                        helpBuilder.setPositiveButton(getString(R.string.download_complete_dialog_positive), new DialogInterface.OnClickListener() {

                            public void onClick(DialogInterface dialog, int which) {

                                Intent v_intent = new Intent();
                                v_intent.setAction(android.content.Intent.ACTION_VIEW);
                                v_intent.setDataAndType(videoUri, "video/*");
                                startActivity(v_intent);
                            }
                        });

                        helpBuilder.setNegativeButton(getString(R.string.dialogs_negative), new DialogInterface.OnClickListener() {

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
            
    BroadcastReceiver clickReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();            
            if (DownloadManager.ACTION_NOTIFICATION_CLICKED.equals(action)) {
            	Query query = new Query();
	            query.setFilterById(enqueue);
	            Cursor c2 = downloadManager.query(query);
	            if (c2.moveToFirst()) {
	            	int columnIndex = c2.getColumnIndex(DownloadManager.COLUMN_STATUS);
	                if (DownloadManager.STATUS_RUNNING == c2.getInt(columnIndex) ||
	                	DownloadManager.STATUS_PAUSED == c2.getInt(columnIndex) ||
	                	DownloadManager.STATUS_PENDING == c2.getInt(columnIndex)) {
	                	AlertDialog.Builder helpBuilder = new AlertDialog.Builder(context);
	                	helpBuilder.setIcon(android.R.drawable.ic_dialog_alert);
	                	helpBuilder.setTitle(getString(R.string.cancel_download_dialog_title));
                        helpBuilder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {

                            public void onClick(DialogInterface dialog, int which) {
                    			downloadManager.remove(enqueue);
                            	Log.d(DEBUG_TAG, "download cancelled");
                            }
                        });
                        
                        helpBuilder.setNegativeButton(getString(R.string.dialogs_negative), new DialogInterface.OnClickListener() {

                            public void onClick(DialogInterface dialog, int which) {
                            }
                        });

                        AlertDialog helpDialog = helpBuilder.create();
                        helpDialog.show();
	                }
	            }
            }
        }
	};

	/*private void showPopUp(String title, String message, String type) {
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
    }*/
}
