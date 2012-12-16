package dentex.youtube.downloader;

import java.io.ByteArrayInputStream;
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
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.app.DownloadManager.Query;
import android.app.DownloadManager.Request;
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

public class ShareActivity extends Activity {
	
	private ProgressBar progressBar1;
    public static final String USER_AGENT_FIREFOX = "Mozilla/5.0 (Windows; U; Windows NT 6.0; en-US; rv:1.9.0.10) Gecko/2009042316 Firefox/3.0.10 (.NET CLR 3.5.30729)";
    private static final String DEBUG_TAG = "ShareActivity";
    private TextView tv;
    private ListView lv;
    private InputStream isFromString;
    private String[] links;
    List<String> codecs = new ArrayList<String>();
    List<String> qualities = new ArrayList<String>();
    List<String> CQchoices = new ArrayList<String>();
    private String titleRaw;
    private String title;
    public int pos;
    public File path;
    private String ytVideoLink;
    public String validatedLink;
    private DownloadManager downloadManager;
    private long enqueue;
	String filename = "video";
	String composedFilename;
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
        registerReceiver(receiver, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_share, menu);
        return true;
    }
    
    public void settingsClick(MenuItem item) {
        Intent c_intent = new Intent(this, SettingsActivity.class);
        startActivity(c_intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(receiver, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(receiver);
    }


    void handleSendText(Intent intent) throws IOException {

        ConnectivityManager connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        if (networkInfo != null && networkInfo.isConnected()) {
            String sharedText = intent.getStringExtra(Intent.EXTRA_TEXT);
            if (linkValidator(sharedText) == "not_a_valid_youtube_link") {
            	progressBar1.setVisibility(View.GONE);
            	tv.setText(getString(R.string.bad_link));
                showPopUp(getString(R.string.error), getString(R.string.bad_link_dialog_msg), "alert");
            } else if (sharedText != null) {
            	showGeneralInfoTutorial();
                new AsyncDownload().execute(sharedText);
                //Toast.makeText(this, "Please wait...", Toast.LENGTH_LONG).show();
            }
        } else {
        	progressBar1.setVisibility(View.GONE);
        	tv.setText(getString(R.string.no_net));
        	showPopUp(getString(R.string.no_net), getString(R.string.no_net_dialog_msg), "alert");
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
        String link = sharedText.replaceAll("feature=.*&", "").replaceAll("&feature=.*", "");
        Pattern pattern = Pattern.compile("(http|https)://www.youtube.com/watch\\?v=.{11}$");
        Matcher matcher = pattern.matcher(link);
        if (matcher.find()) {
            validatedLink = link;
            return link;
        } else {
            return "not_a_valid_youtube_link";
        }
    }
    
    /* Checks if external storage is available for read and write */
    public boolean pathCheckOK() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state) && path.canWrite()) {
        	Pattern extPattern = Pattern.compile("(extSdCard|sdcard1|emmc)");
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
                showPopUp(getString(R.string.error), getString(R.string.invalid_url), "alert");
            }

            String[] lv_arr = CQchoices.toArray(new String[0]);
            lv.setAdapter(new ArrayAdapter<String>(ShareActivity.this, android.R.layout.simple_list_item_1, lv_arr));
            Log.d(DEBUG_TAG, "LISTview done");

            tv.setText(titleRaw);

            lv.setOnItemClickListener(new OnItemClickListener() {
                
				public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
					
					assignPath();
                    
                    //createExternalStorageLogFile(stringToIs(links[position]), "ytd_FINAL_LINK.txt");
                    pos = position;
                    AlertDialog.Builder helpBuilder = new AlertDialog.Builder(ShareActivity.this);

                    helpBuilder.setIcon(android.R.drawable.ic_dialog_info);
                    helpBuilder.setTitle(getString(R.string.list_click_dialog_title));
                    helpBuilder.setMessage(titleRaw + "\n\n\tCodec: " + codecs.get(position) + "\n\tQuality: " + qualities.get(position));

                    helpBuilder.setPositiveButton(getString(R.string.list_click_dialog_positive), new DialogInterface.OnClickListener() {

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
		                    	    		callDownloadManager();
		                    	    	}
		                    	    });
		                    	    adb.show();
	                            } else {
	                            	callDownloadManager();
	                            }
                            } else {
                            	Log.d(DEBUG_TAG, "Destination folder is NOT available and/or NOT writable");
                            	showPopUp(getString(R.string.unable_save), getString(R.string.unable_save_dialog_msg), "alert");
                            }
                        }
                    });

                    helpBuilder.setNeutralButton(getString(R.string.list_click_dialog_neutral), new DialogInterface.OnClickListener() {

                        public void onClick(DialogInterface dialog, int which) {
                        	String wgetCmd;
                        	composedFilename = composeFilename();
                        	wgetCmd = "REQ=`wget -q -e \"convert-links=off\" --keep-session-cookies --save-cookies /dev/null --no-check-certificate \'" + 
                        			validatedLink + "\' -O-` && urlblock=`echo $REQ | grep -oE \'\"url_encoded_fmt_stream_map\".*(\", \"cafe|\", \"watermark)\' |" + 
                        			" sed -e \'s/\"url_encoded_fmt_stream_map\": \"itag=[0-9][0-9]//\' -e \'s/\".*, \"watermark//\' -e \'s/\", \"cafe//\'" + 
                        			" -e \'s/,itag=[0-9]*//g\'` && urlarray=( `echo $urlblock | sed \'s/\\\\\\u0026url=/\\n\\n/g\'` ) && N=" + pos + 
                        			" && downloadurl=`echo \"${urlarray[$N]}\" | sed -e \'s/%3A/:/g\' -e \'s/%2F/\\//g\' -e \'s/%3F/\\?/g\' -e \'s/%3D/\\=/g\'" + 
                        			" -e \'s/%252C/%2C/g\' -e \'s/%26/\\&/g\' -e \'s/\\\\\\u0026type.*\\\\\\u0026sig/\\&signature/g\' -e \'s/%253A/\\:/g\' -e" + 
                        			" \'s/\\\\\\u0026quality.*//\'` && wget -e \"convert-links=off\" --keep-session-cookies --save-cookies /dev/null --tries=5" + 
                        			" --timeout=45 --no-check-certificate \"$downloadurl\" -O " + composedFilename;

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

                    AlertDialog helpDialog = helpBuilder.create();
                    helpDialog.show();
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
        	filename = title + "_" + qualities.get(pos) + "." + codecs.get(pos);
    	    if (useQualitySuffix() == false) filename = title + "." + codecs.get(pos);
    	    return filename;
        }
        
        void callDownloadManager() {
        	ytVideoLink = links[pos];
            downloadManager = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
            Request request = new Request(Uri.parse(ytVideoLink));
			videoUri = Uri.parse(path.toURI() + composedFilename);
            Log.d(DEBUG_TAG, "downloadedVideoUri: " + videoUri);
            request.setDestinationUri(videoUri);
            request.allowScanningByMediaScanner();
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        	enqueue = downloadManager.enqueue(request);
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
    	            	startActivity(intent);
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

    // Given a URL, establishes an HttpUrlConnection and retrieves
    // the web page content as a InputStream, which it returns as a string.
    private String downloadUrl(String myurl) throws IOException {
        InputStream is = null;
        // Only display the first "len" characters of the retrieved web page content.
        int len = 200000;
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
            String contentAsString = readIt(is, len);
            return contentAsString;

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

        Pattern startPattern = Pattern.compile("url_encoded_fmt_stream_map\\\": \\\"itag=.+?\\\\u0026url=");
        Pattern endPattern = Pattern.compile("\\\", \\\"(cafe_experiment|watermark)");
        Matcher matcher = startPattern.matcher(content);
        if (matcher.find()) {
            try {
                String[] start = content.split(startPattern.toString());
                String[] end = start[1].split(endPattern.toString());

                // Other decoding Stuff
                String contentDecoded = URLDecoder.decode(end[0], "UTF-8");

                findCodecAndQuality(contentDecoded);

                String contentDecodedSig = contentDecoded.replaceAll("\\\\u0026type.+?sig", "&signature");
                String contentDecodedAmp = contentDecodedSig.replaceAll("\\\\u0026", "&");
                links = contentDecodedAmp.split(",itag=.+?&url=");
                Log.d(DEBUG_TAG, "number of links found: " + links.length);

            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            //createExternalStorageLogFile(stringToIs(Arrays.toString(links)), "ytd_links.txt");
            //createExternalStorageLogFile(stringToIs(Arrays.toString(codecs.toArray())), "ytd_codecs.txt");
            //createExternalStorageLogFile(stringToIs(Arrays.toString(qualities.toArray())), "ytd_qualities.txt");
            return "Match!\nSee the debug file in 'sdcard/Movies'";
        } else {
            return "No Match";
        }
    }

    private void findVideoFilename(String content) {
        Pattern videoPatern = Pattern.compile("<title>(.*?)</title>");
        Matcher matcher = videoPatern.matcher(content);
        if (matcher.find()) {
            String titleRaw2 = matcher.group();
            titleRaw = titleRaw2.replaceAll("(<| - YouTube</)title>", "").replaceAll("&quot;", "\"");
            title = titleRaw.replaceAll("\\W", "_");
        } else {
            title = "Youtube Video";
        }
    }

    private void findCodecAndQuality(String contentDecoded) {
        Pattern trimPattern = Pattern.compile("(http|,itag.+?url=http).+?type=video/");
        Matcher matcher = trimPattern.matcher(contentDecoded);
        if (matcher.find()) {
            String[] CQ = contentDecoded.split(trimPattern.toString());
            Log.d(DEBUG_TAG, "number of CQ found: " + (CQ.length-1));
            //count = CQ.length;
            int index = 0;
            while ((index+1) < CQ.length) {
                codecMatcher(CQ[index+1], index);
                qualityMatcher(CQ[index+1], index);
                //Log.d(DEBUG_TAG, "CQstr " + index +": " + CQ[index]);
                index++;
            }
            CQchoiceBuilder();
        }
    }

    private void CQchoiceBuilder() {
        Iterator<String> codecsIter = codecs.iterator();
        Iterator<String> qualitiesIter = qualities.iterator();
        while (codecsIter.hasNext()) {
            CQchoices.add(codecsIter.next() + " - " + qualitiesIter.next());
        }
    }

    private void codecMatcher(String currentCQ, int i) {
        Pattern codecPattern = Pattern.compile("(webm|mp4|flv|3gpp)");
        //Pattern codecPattern = Pattern.compile("(webm|mp4|3gpp)");
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
        //Pattern qualityPattern = Pattern.compile("(hd1080|hd720|large|medium)");
        Matcher qualityMatcher = qualityPattern.matcher(currentCQ);
        if (qualityMatcher.find()) {
            qualities.add(qualityMatcher.group());
        } else {
            qualities.add("NoMatch");
        }
        //Log.d(DEBUG_TAG, "CQ index: " + i + ", Quality: " + qualities.get(i));
    }

    public InputStream stringToIs(String text) {
        try {
            isFromString = new ByteArrayInputStream(text.getBytes("UTF-8"));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return isFromString;
    }

    void createExternalStorageLogFile(InputStream stream, String filename) {
        File file = new File(path, filename);

        try {
            // Make sure the directory exists.
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

    BroadcastReceiver receiver = new BroadcastReceiver() {
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
                            }
                        });

                        AlertDialog helpDialog = helpBuilder.create();
                        helpDialog.show();
                    }
                }
            }
        }
    };

}
