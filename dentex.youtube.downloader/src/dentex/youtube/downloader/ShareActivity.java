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
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.app.DownloadManager.Query;
import android.app.DownloadManager.Request;
import android.app.NotificationManager;
import android.app.PendingIntent;
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
import android.content.res.AssetManager;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaScannerConnection;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import dentex.youtube.downloader.service.DownloadsService;
import dentex.youtube.downloader.utils.Observer;
import dentex.youtube.downloader.utils.PopUps;
import dentex.youtube.downloader.utils.Utils;

public class ShareActivity extends Activity {
	
	private Intent SharingIntent;
	private ProgressBar progressBar1;
	private ProgressBar filesizeProgressBar;
    public static final String USER_AGENT_FIREFOX = "Mozilla/5.0 (X11; Linux i686; rv:10.0) Gecko/20100101 Firefox/10.0";
	private static final String DEBUG_TAG = "ShareActivity";
    private TextView tv;
    private ListView lv;
    public ArrayAdapter<String> aA;
    //private InputStream isFromString;
    List<String> links = new ArrayList<String>();
    List<String> codecs = new ArrayList<String>();
    List<String> qualities = new ArrayList<String>();
    List<String> sizes = new ArrayList<String>();
    List<String> listEntries = new ArrayList<String>();
    private String titleRaw;
    private String title;
    public int pos;
    public static File path;
    public String validatedLink;
    public static DownloadManager dm;
    public static long enqueue;
	String vfilename = "video";
	public static String composedFilename = "";
    public static Uri videoUri;
	public boolean videoOnExt;
    private int icon;
    public ScrollView generalInfoScrollview;
	public CheckBox showAgain1;
	public CheckBox showAgain2;
	public CheckBox showAgain3;
	public TextView userFilename;
	public static SharedPreferences settings;
	public static final String PREFS_NAME = "dentex.youtube.downloader_preferences";
	public static final File dir_Downloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
	public static final File dir_DCIM = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM);
	public static final File dir_Movies = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES);
	boolean sshInfoCheckboxEnabled;
	boolean generalInfoCheckboxEnabled;
	boolean fileRenameEnabled;
	public static File chooserFolder;
	private AsyncDownload asyncDownload;
	public boolean isAsyncDownloadRunning = false;
	public String videoFileSize = "empty";
	private AsyncSizeQuery sizeQuery;
	public AlertDialog helpDialog;
	public AlertDialog waitBox;
	private AlertDialog.Builder  helpBuilder;
	private AlertDialog.Builder  waitBuilder;
	private Bitmap img;
	private ImageView imgView;
	private String videoId;
	public static String pt1;
	public static String pt2;
	public static String noDownloads;
	public static Observer.delFileObserver fileObserver;
	public static int mId = 0;
	public static NotificationManager mNotificationManager;
	public static NotificationCompat.Builder mBuilder;
	public static String onlineVersion;
	public static List<Long> sequence = new ArrayList<Long>();
	boolean showSizeListPref;
	boolean showSizePref;
	ContextThemeWrapper boxThemeContextWrapper = new ContextThemeWrapper(ShareActivity.this, R.style.BoxTheme);

    @SuppressLint("CutPasteId")
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        settings = getSharedPreferences(PREFS_NAME, 0);
        
    	// Theme init
    	Utils.themeInit(this);
    	
        setContentView(R.layout.activity_share);
        
    	showSizeListPref = settings.getBoolean("show_size_list", false);

    	// Language init
        String lang  = settings.getString("lang", "default");
        if (!lang.equals("default")) {
	        Locale locale = new Locale(lang);
	        Locale.setDefault(locale);
	        Configuration config = new Configuration();
	        config.locale = locale;
	        getBaseContext().getResources().updateConfiguration(config, null);
        }
        
        progressBar1 = (ProgressBar) findViewById(R.id.progressBar1);
        
        lv = (ListView) findViewById(R.id.list);

        tv = (TextView) findViewById(R.id.textView1);
        
        imgView = (ImageView)findViewById(R.id.imgview);

        updateInit();
        
        // Get intent, action and MIME type
        Intent intent = getIntent();
        String action = intent.getAction();
        String type = intent.getType();

        if (Intent.ACTION_SEND.equals(action) && type != null) {
            if ("text/plain".equals(type)) {
                try {
                	SharingIntent = intent;
                    handleSendText(SharingIntent);
                } catch (IOException e) {
                    e.printStackTrace();
                    Log.d(DEBUG_TAG, "Error: " + e.toString());
                }
            }
        }
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
    protected void onStart() {
        super.onStart();
        registerReceiver(inAppCompleteReceiver, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
        Log.v(DEBUG_TAG, "_onStart");
    }
    
    /*@Override
    protected void onRestart() {
    	super.onRestart();
    	Log.v(DEBUG_TAG, "_onRestart");
    }

    @Override
    public void onPause() {
    	super.onPause();
    	Log.v(DEBUG_TAG, "_onPause");
    }*/
    
    @Override
    protected void onStop() {
        super.onStop();
    	unregisterReceiver(inAppCompleteReceiver);
    	Log.v(DEBUG_TAG, "_onStop");
    }
    
    @Override
	public void onBackPressed() {
    	super.onBackPressed();
    	/*
    	 * The next call is here onBackPressed(), and NOT in onStop() because 
    	 * I want to cancel the asyncDownload task only on back button pressed,
    	 * and not when switching to Preferences or D.M. from this activity.
    	 */
    	if (isAsyncDownloadRunning) {
    		asyncDownload.cancel(true);
    	}
		Log.i(DEBUG_TAG, "_onBackPressed");
	}

    void handleSendText(Intent intent) throws IOException {

        ConnectivityManager connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        if (networkInfo != null && networkInfo.isConnected()) {
            String sharedText = intent.getStringExtra(Intent.EXTRA_TEXT);
            if (linkValidator(sharedText) == "not_a_valid_youtube_link") {
            	progressBar1.setVisibility(View.GONE);
            	tv.setText(getString(R.string.bad_link));
            	PopUps.showPopUp(getString(R.string.error), getString(R.string.bad_link_dialog_msg), "alert", this);
            } else if (sharedText != null) {
            	showGeneralInfoTutorial();
            	asyncDownload = new AsyncDownload();
            	asyncDownload.execute(validatedLink);
            }
        } else {
        	progressBar1.setVisibility(View.GONE);
        	tv.setText(getString(R.string.no_net));
        	PopUps.showPopUp(getString(R.string.no_net), getString(R.string.no_net_dialog_msg), "alert", this);
        }
    }
    
    void showGeneralInfoTutorial() {
        generalInfoCheckboxEnabled = settings.getBoolean("general_info", true);
        if (generalInfoCheckboxEnabled == true) {
        	AlertDialog.Builder adb = new AlertDialog.Builder(boxThemeContextWrapper);
    	    LayoutInflater adbInflater = LayoutInflater.from(ShareActivity.this);
    	    View generalInfo = adbInflater.inflate(R.layout.dialog_general_info, null);
    	    generalInfoScrollview = (ScrollView) generalInfo.findViewById(R.id.generalInfoScrollview);

    	    showAgain1 = (CheckBox) generalInfo.findViewById(R.id.showAgain1);
    	    showAgain1.setChecked(true);
    	    adb.setView(generalInfo);
    	    adb.setTitle(getString(R.string.tutorial_title));    	    
    	    //adb.setMessage(getString(R.string.tutorial_msg));

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
            videoId = matcher.group(2).replace("v=", "");
            return validatedLink;
        }
        return "not_a_valid_youtube_link";
    }
    
    public static void assignPath() {
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

    	protected void onPreExecute() {
    		isAsyncDownloadRunning = true;
    		tv.setText(R.string.loading);
    		progressBar1.setVisibility(View.VISIBLE);
    	}
    	
    	protected String doInBackground(String... urls) {
            try {
            	Log.d(DEBUG_TAG, "doInBackground...");
            	
            	if (settings.getBoolean("show_thumb", false)) {
            		downloadThumbnail(generateThumbUrl());
            	}
            	
            	return downloadUrl(urls[0]);
            } catch (IOException e) {
                return "e";
            }
        }

        @Override
        protected void onPostExecute(String result) {

        	progressBar1.setVisibility(View.GONE);
        	
        	if (settings.getBoolean("show_thumb", false)) {
        		imgView.setImageBitmap(img);
        	}
        	isAsyncDownloadRunning = false;
        	
            if (result == "e") {
            	tv.setText(getString(R.string.invalid_url_short));
                PopUps.showPopUp(getString(R.string.error), getString(R.string.invalid_url), "alert", ShareActivity.this);
                titleRaw = getString(R.string.invalid_response);
            }

            String[] lv_arr = listEntries.toArray(new String[0]);
            
            aA = new ArrayAdapter<String>(ShareActivity.this, android.R.layout.simple_list_item_1, lv_arr);
            
            lv.setAdapter(aA);
            lv.setLongClickable(true);
            Log.d(DEBUG_TAG, "LISTview done with " + lv_arr.length + " items.");

            tv.setText(titleRaw);
            
            lv.setOnItemClickListener(new OnItemClickListener() {
				public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
					Log.i(DEBUG_TAG, "Selected link: " + links.get(pos));
					assignPath();
                    //createLogFile(stringToIs(links[position]), "ytd_FINAL_LINK.txt");
					
                    pos = position;     
                    //pos = 45;		// to test IndexOutOfBound Exception...
                    
                	helpBuilder = new AlertDialog.Builder(new ContextThemeWrapper(ShareActivity.this, R.style.BoxTheme));
                    helpBuilder.setIcon(android.R.drawable.ic_dialog_info);
                    helpBuilder.setTitle(getString(R.string.list_click_dialog_title));
                    
                    if (showSizeListPref) {
                    	showSizePref = true;
                    } else {
                    	showSizePref = settings.getBoolean("show_size", false);
                    }
					
					try {
                        if (!showSizePref) {
                        	helpBuilder.setMessage(titleRaw + 
                        			getString(R.string.codec) + " " + codecs.get(pos) + 
                					getString(R.string.quality) + " " + qualities.get(pos));
                        } else {
                        	if (!showSizeListPref) {
                        		sizeQuery = new AsyncSizeQuery();
                        		sizeQuery.execute(links.get(position));
                        	} else {
                        		helpBuilder.setMessage(titleRaw + 
                            			getString(R.string.codec) + " " + codecs.get(pos) + 
                    					getString(R.string.quality) + " " + qualities.get(pos) +
                    					getString(R.string.size) + " " + sizes.get(pos));
                        	}
                        }
					} catch (IndexOutOfBoundsException e) {
			    		Toast.makeText(ShareActivity.this, getString(R.string.video_list_error_toast), Toast.LENGTH_SHORT).show();
			    	}
					
                    helpBuilder.setPositiveButton(getString(R.string.list_click_download_local), new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                        	try {
                            	Log.d(DEBUG_TAG, "Destination folder is available and writable");
                        		composedFilename = composeFilename();
	                            fileRenameEnabled = settings.getBoolean("enable_rename", false);

	                            if (fileRenameEnabled == true) {
	                            	
									AlertDialog.Builder adb = new AlertDialog.Builder(boxThemeContextWrapper);
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
                        	} catch (IndexOutOfBoundsException e) {
    							Toast.makeText(ShareActivity.this, getString(R.string.video_list_error_toast), Toast.LENGTH_SHORT).show();
    						}
                        }
                    });
					

                    helpBuilder.setNeutralButton(getString(R.string.list_click_download_ssh), new DialogInterface.OnClickListener() {

                        public void onClick(DialogInterface dialog, int which) {
                        	try {
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
    	                            AlertDialog.Builder adb = new AlertDialog.Builder(boxThemeContextWrapper);
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
                        	} catch (IndexOutOfBoundsException e) {
                        		Toast.makeText(ShareActivity.this, getString(R.string.video_list_error_toast), Toast.LENGTH_SHORT).show();
                        	}
                        }
                    });

                    helpBuilder.setNegativeButton(getString(R.string.dialogs_negative), new DialogInterface.OnClickListener() {

                        public void onClick(DialogInterface dialog, int which) {
                            //Toast.makeText(ShareActivity.this, "Download canceled...", Toast.LENGTH_SHORT).show();
                        }
                    });
                    
                    if ((!showSizePref) || (showSizeListPref && showSizePref)) {
                    	helpDialog = helpBuilder.create();
                    	helpDialog.show();
                    }
                }
            });
            
            lv.setOnItemLongClickListener(new OnItemLongClickListener() {

            	@Override
				public boolean onItemLongClick(AdapterView<?> parent, View view, final int position, long id) {
            		pos = position;
					AlertDialog.Builder builder = new AlertDialog.Builder(boxThemeContextWrapper);
				    builder.setTitle(R.string.long_click_title)
				    	   //.setIcon(android.R.drawable.ic_menu_share)
				           .setItems(R.array.long_click_entries, new DialogInterface.OnClickListener() {
				               public void onClick(DialogInterface dialog, int which) {
				            	   composedFilename = composeFilename();
				            	   switch (which) {
				            	   case 0: // copy
				            		    ClipData cmd = ClipData.newPlainText("simple text", links.get(position));
				            		    ClipboardManager cb = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
				            		    cb.setPrimaryClip(cmd);
				            		    break;
				            	   case 1: // share
			            			    Intent sharingIntent = new Intent(android.content.Intent.ACTION_SEND);
			            			    sharingIntent.setType("text/plain");
			            			    sharingIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, composedFilename);
			            			    sharingIntent.putExtra(android.content.Intent.EXTRA_TEXT, links.get(position));
			            			    startActivity(Intent.createChooser(sharingIntent, "Share YouTube link:"));
				            	   }
				           }
				    });
				    builder.create().show();
				    return true;
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
    			AlertDialog.Builder cb = new AlertDialog.Builder(boxThemeContextWrapper);
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
    	            		PopUps.showPopUp(getString(R.string.no_market), getString(R.string.no_net_dialog_msg), "alert", ShareActivity.this);
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
		videoUri = Uri.parse(path.toURI() + composedFilename);
        Log.d(DEBUG_TAG, "videoUri: " + videoUri);
        
        dm = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
        Request request = new Request(Uri.parse(link));
        request.setDestinationUri(videoUri);
        request.allowScanningByMediaScanner();
        
        String visValue = settings.getString("download_manager_notification", "VISIBLE");
        int vis;
		if (visValue.equals("VISIBLE_NOTIFY_COMPLETED")) {
			vis = DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED;
		} else if (visValue.equals("HIDDEN")) {
			vis = DownloadManager.Request.VISIBILITY_HIDDEN;
		} else {
			vis = DownloadManager.Request.VISIBILITY_VISIBLE;
		}
        request.setNotificationVisibility(vis);
        request.setTitle(vfilename);
    	
    	Intent intent1 = new Intent(ShareActivity.this, DownloadsService.class);
    	intent1.putExtra("COPY", false);
    	
		try {
			enqueue = dm.enqueue(request);
        	Log.d(DEBUG_TAG, "_ID " + enqueue + " enqueued");
        } catch (SecurityException e) {
        	// handle path on etxSdCard:
        	Log.w(DEBUG_TAG, e.getMessage());
        	showExtsdcardInfo();
        	intent1.putExtra("COPY", true);
        	videoOnExt = true;
        	tempDownloadToSdcard(request);
        }
		
    	startService(intent1);
		
		settings.edit().putString(String.valueOf(enqueue), composedFilename).apply();
    	
    	if (settings.getBoolean("enable_own_notification", true) == true) {
    		Log.i(DEBUG_TAG, "enable_own_notification: true");
			sequence.add(enqueue);
			settings.edit().putLong(composedFilename, enqueue).apply();
			
			if (videoOnExt == true) {
				fileObserver = new Observer.delFileObserver(dir_Downloads.getAbsolutePath());
			} else {
				fileObserver = new Observer.delFileObserver(path.getAbsolutePath());
			}
			fileObserver.startWatching();
			
			NotificationHelper();
		}
    }
    
    public String generateThumbUrl() {
		// link example "http://i2.ytimg.com/vi/8wr-uQX1Grw/mqdefault.jpg"
    	Random random = new Random();
    	int num = random.nextInt(4 - 1) + 1;
    	String url = "http://i" + num + ".ytimg.com/vi/" + videoId + "/mqdefault.jpg";
    	Log.i(DEBUG_TAG, "thumbnail url: " + url);
    	return url;
	}

	void showExtsdcardInfo() {
        generalInfoCheckboxEnabled = settings.getBoolean("extsdcard_info", true);
        if (generalInfoCheckboxEnabled == true) {
        	AlertDialog.Builder adb = new AlertDialog.Builder(boxThemeContextWrapper);
    	    LayoutInflater adbInflater = LayoutInflater.from(ShareActivity.this);
    	    View generalInfo = adbInflater.inflate(R.layout.dialog_extsdcard_info, null);
    	    showAgain3 = (CheckBox) generalInfo.findViewById(R.id.showAgain3);
    	    showAgain3.setChecked(true);
    	    adb.setView(generalInfo);
    	    adb.setTitle(getString(R.string.extsdcard_info_title));    	    
    	    adb.setMessage(getString(R.string.extsdcard_info_msg));

    	    adb.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
    	    	public void onClick(DialogInterface dialog, int which) {
    	    		if (showAgain3.isChecked() == false) {
    	    			settings.edit().putBoolean("extsdcard_info", false).commit();
    	    			sshInfoCheckboxEnabled = settings.getBoolean("extsdcard_info", true);
    	    			Log.d(DEBUG_TAG, "generalInfoCheckboxEnabled: " + generalInfoCheckboxEnabled);
    	    		}
        		}
        	});
    	    adb.show();
        }
    }
      
    private void tempDownloadToSdcard(Request request) {
    	videoUri = Uri.parse(dir_Downloads.toURI() + composedFilename);
        Log.d(DEBUG_TAG, "** NEW ** videoUri: " + videoUri);
        request.setDestinationUri(videoUri);
        enqueue = dm.enqueue(request);
    }

    private void NotificationHelper() {
    	pt1 = getString(R.string.notification_downloading_pt1);
    	pt2 = getString(R.string.notification_downloading_pt2);
    	noDownloads = getString(R.string.notification_no_downloads);
    	
    	mBuilder =  new NotificationCompat.Builder(this);
    	
    	mBuilder.setSmallIcon(R.drawable.icon_nb)
    	        .setContentTitle(getString(R.string.app_name))
    	        .setContentText(getString(R.string.notification_downloading_pt1) + " " + sequence.size() + " " + getString(R.string.notification_downloading_pt2));
    	
    	mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
    	
    	Intent notificationIntent = new Intent(android.app.DownloadManager.ACTION_VIEW_DOWNLOADS);
    	notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
    	PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
    	mBuilder.setContentIntent(contentIntent);
    	mId = 1;
    	mNotificationManager.notify(mId, mBuilder.build());
	}

	// Given a URL, establishes an HttpUrlConnection and retrieves
    // the web page content as a InputStream, which it returns as a string.
    private String downloadUrl(String myurl) throws IOException {
        InputStream is = null;
        // Only display the first "len" characters of the retrieved web page content.
        int len = 2000000;
        Log.d(DEBUG_TAG, "The link is: " + myurl);
        if (!asyncDownload.isCancelled()) {
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
	            if (!asyncDownload.isCancelled()) {
	            	return readIt(is, len);
	            } else {
	            	Log.d(DEBUG_TAG, "asyncDownload cancelled @ 'return readIt'");
	            	return null;
	            }
	
	            //Makes sure that the InputStream is closed after the app is finished using it.
	        } finally {
	            if (is != null) {
	                is.close();
	            }
	        }
        } else {
        	Log.d(DEBUG_TAG, "asyncDownload cancelled @ 'downloadUrl' begin");
        	return null;
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
		
		if (asyncDownload.isCancelled()) {
			Log.d(DEBUG_TAG, "asyncDownload cancelled @ urlBlockMatchAndDecode begin");
			return "Cancelled!";
		}
		
        findVideoFilename(content);

        Pattern pattern = Pattern.compile("url_encoded_fmt_stream_map\\\": \\\"(.*?)\\\"");
        Matcher matcher = pattern.matcher(content);
        if (matcher.find()) {
        	Pattern blockPattern = Pattern.compile(",");
            Matcher blockMatcher = blockPattern.matcher(matcher.group(1));
            if (blockMatcher.find() && !asyncDownload.isCancelled()) {
            	String[] CQS = matcher.group(1).split(blockPattern.toString());
                Log.d(DEBUG_TAG, "number of entries found: " + (CQS.length-1));
                int index = 0;
                while ((index+1) < CQS.length) {
                	try {
						CQS[index] = URLDecoder.decode(CQS[index], "UTF-8");
					} catch (UnsupportedEncodingException e) {
						Log.e(DEBUG_TAG, e.getMessage());
					}
                	
                    codecMatcher(CQS[index], index);
                    qualityMatcher(CQS[index], index);
                    linkComposer(CQS[index], index);
                    //Log.d(DEBUG_TAG, "block " + index + ": " + CQS[index]);
                    index++;
                }
                listEntriesBuilder();
            } else {
            	Log.d(DEBUG_TAG, "asyncDownload cancelled @ 'findCodecAndQualityAndLinks' match");
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
        Pattern titlePattern = Pattern.compile("<title>(.*?)</title>");
        Matcher titleMatcher = titlePattern.matcher(content);
        if (titleMatcher.find()) {
            titleRaw = titleMatcher.group().replaceAll("(<| - YouTube</)title>", "").replaceAll("&quot;", "\"").replaceAll("&amp;", "&").replaceAll("&#39;", "'");
            title = titleRaw.replaceAll("\\W", "_");
        } else {
            title = "Youtube Video";
        }
        Log.d(DEBUG_TAG, "findVideoFilename: " + title);
    }

    private void listEntriesBuilder() {
    	if (settings.getBoolean("show_size_list", false)) {
	        Iterator<String> codecsIter = codecs.iterator();
	        Iterator<String> qualitiesIter = qualities.iterator();
	        Iterator<String> sizesIter = sizes.iterator();
	        while (codecsIter.hasNext()) {
	        	try {
	        		listEntries.add(codecsIter.next() + " - " + qualitiesIter.next() + " - " + sizesIter.next());
	        	} catch (NoSuchElementException e) {
	        		listEntries.add("//");
	        	}
	        }
    	} else {
            Iterator<String> codecsIter = codecs.iterator();
            Iterator<String> qualitiesIter = qualities.iterator();
            while (codecsIter.hasNext()) {
            	try {
                	listEntries.add(codecsIter.next() + " - " + qualitiesIter.next());
            	} catch (NoSuchElementException e) {
	        		listEntries.add("//");
	        	}	
            }
            
    	}
    }
    
    private void linkComposer(String block, int i) {
    	Pattern urlPattern = Pattern.compile("url=(.+?)\\\\u0026");
    	Matcher urlMatcher = urlPattern.matcher(block);
    	String url = null;
		if (urlMatcher.find()) {
    		url = urlMatcher.group(1);
    	} else {
    		Pattern urlPattern2 = Pattern.compile("url=(.+?)$");
    		Matcher urlMatcher2 = urlPattern2.matcher(block);
    		if (urlMatcher2.find()) {
        		url = urlMatcher2.group(1);
        	} else {
        		Log.e(DEBUG_TAG, "url: " + url);
        	}
    	}
    		
    	Pattern sigPattern = Pattern.compile("sig=([[0-9][A-Z]]{39,40}\\.[[0-9][A-Z]]{39,40})");
    	Matcher sigMatcher = sigPattern.matcher(block);
    	String sig = null;
		if (sigMatcher.find()) {
    		sig = "signature=" + sigMatcher.group(1);
    	} else {
    		Log.e(DEBUG_TAG, "sig: " + sig);
    	}

		//Log.d(DEBUG_TAG, "url: " + url);
		//Log.d(DEBUG_TAG, "sig: " + sig);
    	
		String composedLink = url + "&" + sig;

		links.add(composedLink);
		//Log.i(DEBUG_TAG, composedLink);
		if (settings.getBoolean("show_size_list", false) && !asyncDownload.isCancelled()) {
			sizes.add(getVideoFileSize(composedLink));
		}
	}
    
    private class AsyncSizeQuery extends AsyncTask<String, Void, String> {
    	
    	protected void onPreExecute() {
    		waitBuilder = new AlertDialog.Builder(boxThemeContextWrapper);
    		LayoutInflater adbInflater = LayoutInflater.from(ShareActivity.this);
    	    View barView = adbInflater.inflate(R.layout.wait_for_filesize, null);
    	    filesizeProgressBar = (ProgressBar) barView.findViewById(R.id.filesizeProgressBar);
    	    filesizeProgressBar.setVisibility(View.VISIBLE);
    	    waitBuilder.setView(barView);
    	    waitBuilder.setIcon(android.R.drawable.ic_dialog_info);
    	    waitBuilder.setTitle(R.string.wait);
    	    waitBuilder.setMessage(titleRaw + 
    	    		getString(R.string.codec) + " " + codecs.get(pos) + 
					getString(R.string.quality) + " " + qualities.get(pos));
    	    waitBox = waitBuilder.create();
    	    waitBox.show();
    	}

		protected String doInBackground(String... urls) {
            // params comes from the execute() call: params[0] is the url.
            return getVideoFileSize(urls[0]);
        }

        @Override
        protected void onPostExecute(String result) {
        	
        	filesizeProgressBar.setVisibility(View.GONE);
        	waitBox.dismiss();
        	
        	videoFileSize = result;
        	
        	helpBuilder.setMessage(titleRaw + 
        			getString(R.string.codec) + " " + codecs.get(pos) + 
					getString(R.string.quality) + " " + qualities.get(pos) +
					getString(R.string.size) + " " + videoFileSize);
        	helpDialog = helpBuilder.create();
            helpDialog.show();
        }
	}
    
    private String getVideoFileSize(String link) {
    	String size;
		try {
			final URL uri = new URL(link);
			HttpURLConnection ucon = (HttpURLConnection) uri.openConnection();
			ucon.connect();
			int file_size = ucon.getContentLength();
			size = MakeSizeHumanReadable(file_size, true);
		} catch(IOException e) {
			size = "n.a.";
		}
		Log.i(DEBUG_TAG, "video File Size: " + size);
		return size;
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
    
    void downloadThumbnail(String fileUrl) {
    	InputStream is = null;
    	URL myFileUrl = null;
    	try {
    		myFileUrl = new URL(fileUrl);
    	} catch (MalformedURLException e) {
    		try {
				myFileUrl =  new URL("https://raw.github.com/dentex/ytdownloader/master/dentex.youtube.downloader/assets/placeholder.png");
			} catch (MalformedURLException e1) {
				e1.printStackTrace();
			}
    		e.printStackTrace();
    	}
    	try {
    		HttpURLConnection conn = (HttpURLConnection) myFileUrl.openConnection();
    		conn.setDoInput(true);
    		conn.connect();
    		is = conn.getInputStream();

    		img = BitmapFactory.decodeStream(is);
    	} catch (IOException e) {
    		InputStream assIs = null;
    		AssetManager assMan = getAssets();
            try {
				assIs = assMan.open("placeholder.png");
			} catch (IOException e1) {
				e1.printStackTrace();
			}
            img = BitmapFactory.decodeStream(assIs);
		}
    }
    
    public void updateInit() {
		int prefSig = settings.getInt("APP_SIGNATURE", 0);
		Log.d(DEBUG_TAG, "prefSig: " + prefSig);
		
		if (prefSig == SettingsActivity.SettingsFragment.YTD_SIG_HASH) {
				Log.d(DEBUG_TAG, "YTD signature in PREFS: update check possile");
				
				if (settings.getBoolean("autoupdate", false)) {
					Log.i(DEBUG_TAG, "autoupdate enabled");
					SettingsActivity.SettingsFragment.autoUpdate(ShareActivity.this);
				}
		} else {
			Log.d(DEBUG_TAG, "diffrent or null YTD signature. Update check cancelled.");
		}
	}

    BroadcastReceiver inAppCompleteReceiver = new BroadcastReceiver() {

		@Override
        public void onReceive(Context context, Intent intent) {
			//Log.d(DEBUG_TAG, "inAppCompleteReceiver: onReceive CALLED");
	        long id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -2);
	        if (enqueue != -1 && id != -2 && id == enqueue && videoOnExt == false) {
	            Query query = new Query();
	            query.setFilterById(id);

	            Cursor c = dm.query(query);
	            if (c.moveToFirst()) {
	                int columnIndex = c.getColumnIndex(DownloadManager.COLUMN_STATUS);
	                int status = c.getInt(columnIndex);
	                if (status == DownloadManager.STATUS_SUCCESSFUL) {
                        AlertDialog.Builder helpBuilder = new AlertDialog.Builder(boxThemeContextWrapper);
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
                        if (! ((Activity) context).isFinishing()) {
                        	helpDialog.show();
                        }
                    }
                }
            }
        }
    };
}
