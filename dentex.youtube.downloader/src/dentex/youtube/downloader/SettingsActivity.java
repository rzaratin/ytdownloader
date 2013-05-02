package dentex.youtube.downloader;

import group.pals.android.lib.ui.filechooser.FileChooserActivity;
import group.pals.android.lib.ui.filechooser.io.localfile.LocalFile;
import group.pals.android.lib.ui.filechooser.services.IFileProvider;

import java.io.File;
import java.util.List;
import java.util.Stack;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Parcelable;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.text.format.DateUtils;
import android.view.ContextThemeWrapper;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;
import dentex.youtube.downloader.ffmpeg.FfmpegController;
import dentex.youtube.downloader.service.AutoUpgradeApkService;
import dentex.youtube.downloader.service.FfmpegDownloadService;
import dentex.youtube.downloader.utils.PopUps;
import dentex.youtube.downloader.utils.Utils;

public class SettingsActivity extends Activity {
	
	public static final String DEBUG_TAG = "SettingsActivity";
	private static final int _ReqChooseFile = 0;
	public static String chooserSummary;
    public static SharedPreferences settings = ShareActivity.settings;
	public final String PREFS_NAME = ShareActivity.PREFS_NAME;
	public static Activity mActivity;
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        this.setTitle(R.string.title_activity_settings);
        
    	settings = getSharedPreferences(PREFS_NAME, 0);

    	// Theme init
    	Utils.themeInit(this);
    	
        // Language init
    	Utils.langInit(this);
    	
        // Load default preferences values
        PreferenceManager.setDefaultValues(this, R.xml.settings, false);
        
        // Display the fragment as the main content.
        getFragmentManager().beginTransaction()
                .replace(android.R.id.content, new SettingsFragment())
                .commit();
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
    	getMenuInflater().inflate(R.menu.activity_settings, menu);
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        super.onOptionsItemSelected(item);
        switch(item.getItemId()){
        	case R.id.menu_donate:
        		startActivity(new Intent(this, DonateActivity.class));
        		return true;
        	case R.id.menu_about:
        		startActivity(new Intent(this, AboutActivity.class));
        		return true;
        	case R.id.menu_dm:
        		startActivity(new Intent(android.app.DownloadManager.ACTION_VIEW_DOWNLOADS));
        		return true;
        	case R.id.menu_tutorials:
        		startActivity(new Intent(this, TutorialsActivity.class));
        		return true;
        	default:
        		return super.onOptionsItemSelected(item);
        }
    }

	public static class SettingsFragment extends PreferenceFragment implements OnSharedPreferenceChangeListener {
    	
		//private Preference dm;
		private Preference filechooser;
		private Preference up;
		private CheckBoxPreference ownNot;
		private Preference th;
		private Preference lang;
		private static CheckBoxPreference audio;
		protected int cpuVers;
		public static String link;

		public static final int YTD_SIG_HASH = -1892118308; // final string
		//public static final int YTD_SIG_HASH = -118685648; // dev test: desktop
		//public static final int YTD_SIG_HASH = 1922021506; // dev test: laptop
		
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            addPreferencesFromResource(R.xml.settings);
            
            final ContextThemeWrapper boxThemeContextWrapper = new ContextThemeWrapper(getActivity(), R.style.BoxTheme);
            mActivity = getActivity();

            String cf = settings.getString("CHOOSER_FOLDER", "");
            if (cf.isEmpty()) {
            	chooserSummary = getString(R.string.chooser_location_summary);
            } else {
            	chooserSummary = settings.getString("CHOOSER_FOLDER", "");
            }
            initSwapPreference();
            initSizePreference();
            
            for(int i=0;i<getPreferenceScreen().getPreferenceCount();i++){
                initSummary(getPreferenceScreen().getPreference(i));
            }

            /*dm = (Preference) findPreference("dm");
            dm.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            	
                public boolean onPreferenceClick(Preference preference) {
                	startActivity(new Intent(android.app.DownloadManager.ACTION_VIEW_DOWNLOADS));
                    return true;
                }
            });*/
            
            filechooser = (Preference) getPreferenceScreen().findPreference("open_chooser");
            filechooser.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            	
                public boolean onPreferenceClick(Preference preference) {
                	Intent intent = new Intent(getActivity(),  FileChooserActivity.class);
            		intent.putExtra(FileChooserActivity._Rootpath, (Parcelable) new LocalFile(Environment.getExternalStorageDirectory()));
            		intent.putExtra(FileChooserActivity._FilterMode, IFileProvider.FilterMode.DirectoriesOnly);
            		startActivityForResult(intent, _ReqChooseFile);
                    return true;
                }
            });
            
            up = (Preference) findPreference("update");
            up.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            	
                public boolean onPreferenceClick(Preference preference) {
		            Intent intent = new Intent(getActivity(),  UpgradeApkActivity.class);
		            startActivity(intent);
		            return true;
                }
            });
            
            initUpdate();
            
            ownNot = (CheckBoxPreference) findPreference("enable_own_notification");
            ownNot.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            	
                public boolean onPreferenceClick(Preference preference) {
                	if (!ownNot.isChecked() && ShareActivity.mId == 1) {
                		ShareActivity.mNotificationManager.cancelAll();
                		ShareActivity.mId = 0;
                	}
					return true;
                }
            });
            
            th = (Preference) findPreference("choose_theme");
			th.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
				
				public boolean onPreferenceChange(Preference preference, Object newValue) {
					String theme = settings.getString("choose_theme", "D");
			    	if (theme.equals("D")) {
			    		getActivity().setTheme(R.style.AppThemeDark);
			    	} else {
			    		getActivity().setTheme(R.style.AppThemeLight);
			    	}
			    	
			    	if (!theme.equals(newValue)) reload();
					return true;
				}
			});
			
			lang = (Preference) findPreference("lang");
			lang.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
				
				public boolean onPreferenceChange(Preference preference, Object newValue) {
					String language = settings.getString("lang", "default");
					if (!language.equals(newValue)) reload();
					return true;
				}
			});

			audio = (CheckBoxPreference) findPreference("enable_audio_extraction");
			audio.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
				
				public boolean onPreferenceChange(Preference preference, Object newValue) {
					boolean audioExtrEnabled = settings.getBoolean("enable_audio_extraction", false);
					File binDir = getActivity().getDir("bin", 0);
					boolean ffmpegInstalled = new File(binDir, "ffmpeg").exists();
					if (!audioExtrEnabled) {
						cpuVers = armCpuVersion();
						boolean isCpuSupported = (cpuVers > 0) ? true : false;
						Utils.logger("d", "isCpuSupported: " + isCpuSupported, DEBUG_TAG);
						
						if (!isCpuSupported) {
							audio.setEnabled(false);
							audio.setChecked(false);
							settings.edit().putBoolean("FFMPEG_SUPPORTED", false).commit();

							AlertDialog.Builder adb = new AlertDialog.Builder(boxThemeContextWrapper);
	                        adb.setIcon(android.R.drawable.ic_dialog_alert);
	                        adb.setTitle(getString(R.string.ffmpeg_device_not_supported));
	                        adb.setMessage(getString(R.string.ffmpeg_support_mail));
	                        
	                        adb.setPositiveButton("OK", new DialogInterface.OnClickListener() {
	                        	
	                            public void onClick(DialogInterface dialog, int which) {
	                            	/*
	                            	 * adapted form same source as createEmailOnlyChooserIntent below
	                            	 */
	                            	Intent i = new Intent(Intent.ACTION_SEND);
	                                i.setType("*/*");
	                                
	                                String content = Utils.getCpuInfo();
	                                /*File destDir = getActivity().getExternalFilesDir(null); 
	                                String filename = "cpuInfo.txt";
	                                try {
										Utils.createLogFile(destDir, filename, content);
										i.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(new File(destDir, filename)));*/
		                                i.putExtra(Intent.EXTRA_EMAIL, new String[] { "samuele.rini76@gmail.com" });
		                                i.putExtra(Intent.EXTRA_SUBJECT, "YTD: device info report");
		                                i.putExtra(Intent.EXTRA_TEXT, content);

		                                startActivity(createEmailOnlyChooserIntent(i, getString(R.string.email_via)));
									/*} catch (IOException e) {
										Log.e(DEBUG_TAG, "IOException on creating cpuInfo Log file ", e);
									}*/
	                            }
	                        });
	                        
	                        adb.setNegativeButton(getString(R.string.dialogs_negative), new DialogInterface.OnClickListener() {
	                        	
	                            public void onClick(DialogInterface dialog, int which) {
	                            	// cancel
	                            }
	                        });
	
	                        AlertDialog helpDialog = adb.create();
	                        if (! (getActivity()).isFinishing()) {
	                        	helpDialog.show();
	                        }	                            
						} else {
							settings.edit().putBoolean("FFMPEG_SUPPORTED", true).commit();
						}
						
						Utils.logger("d", "ffmpegInstalled: " + ffmpegInstalled, DEBUG_TAG);
					
						if (!ffmpegInstalled && isCpuSupported) {
							AlertDialog.Builder adb = new AlertDialog.Builder(boxThemeContextWrapper);
	                        adb.setIcon(android.R.drawable.ic_dialog_info);
	                        adb.setTitle(getString(R.string.ffmpeg_download_dialog_title));
	                        
	                        link = getString(R.string.ffmpeg_download_dialog_msg_link, cpuVers);
	                        String msg = getString(R.string.ffmpeg_download_dialog_msg);
	                        
	                        String ffmpegSize;
	                        if (cpuVers == 5) {
	                        	ffmpegSize = getString(R.string.ffmpeg_size_arm5);
	                        } else if (cpuVers == 7) {
	                        	ffmpegSize = getString(R.string.ffmpeg_size_arm7);
	                        } else {
	                        	ffmpegSize = "n.a.";
	                        }
	                        String size = getString(R.string.size) + " " + ffmpegSize;
	                        adb.setMessage(msg + " " + link + "\n" + size);

	                        adb.setPositiveButton("OK", new DialogInterface.OnClickListener() {
	
	                            public void onClick(DialogInterface dialog, int which) {
	                            	File sdcardAppDir = getActivity().getExternalFilesDir(null);
	                            	if (sdcardAppDir != null) {
	                            		File src = new File(getActivity().getExternalFilesDir(null), FfmpegController.ffmpegBinName);
	                            		File dst = new File(getActivity().getDir("bin", 0), FfmpegController.ffmpegBinName);
	                            		if (!src.exists()) {
			                            	Intent intent = new Intent(getActivity(), FfmpegDownloadService.class);
			                            	intent.putExtra("CPU", cpuVers);
			                            	intent.putExtra("DIR", sdcardAppDir.getAbsolutePath());
			                            	getActivity().startService(intent);
	                            		} else {
	                            			FfmpegDownloadService.copyFfmpegToAppDataDir(getActivity(), src, dst);
	                            		}
	                            	} else {
	                            		Utils.logger("w", getString(R.string.unable_save_dialog_msg), DEBUG_TAG);
	                            		PopUps.showPopUp(getString(R.string.error), getString(R.string.unable_save_dialog_msg), "alert", getActivity());
	                            	}
	                            }
	                        });
	
	                        adb.setNegativeButton(getString(R.string.dialogs_negative), new DialogInterface.OnClickListener() {
	
	                            public void onClick(DialogInterface dialog, int which) {
	                            	// cancel
	                            }
	                        });
	
	                        AlertDialog helpDialog = adb.create();
	                        if (! (getActivity()).isFinishing()) {
	                        	helpDialog.show();
	                        }
						}
					}
					if (ffmpegInstalled) {
						return true;
					} else {
						return false;
					}
				}
			});
			
			initAudioPreference();
		}
        
        private int armCpuVersion() {
        	String cpuAbi = Build.CPU_ABI;
			Utils.logger("d", "CPU_ABI: " + cpuAbi, DEBUG_TAG);
			if (cpuAbi.equals("armeabi-v7a")) {
				return 7;
			} else if (cpuAbi.equals("armeabi")) {
				return 5;
			} else {
				return 0;
			}
		}

		public void reload() {
        	Intent intent = getActivity().getIntent();
        	intent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
    		getActivity().finish();
    		getActivity().overridePendingTransition(0, 0);
    		startActivity(intent);
    		getActivity().overridePendingTransition(0, 0);
        }

		public void initUpdate() {
			int prefSig = settings.getInt("APP_SIGNATURE", 0);
			Utils.logger("d", "prefSig: " + prefSig, DEBUG_TAG);
			
			if (prefSig == 0 ) {
				if (Utils.getSigHash(SettingsFragment.this) == YTD_SIG_HASH) {
					Utils.logger("d", "Found YTD signature: update check possile", DEBUG_TAG);
					up.setEnabled(true);
					
					if (settings.getBoolean("autoupdate", false)) {
						Utils.logger("i", "autoupdate enabled", DEBUG_TAG);
						autoUpdate(getActivity());
					}
		    	} else {
		    		Utils.logger("d", "Found different signature: " + Utils.currentHashCode + " (F-Droid?). Update check cancelled.", DEBUG_TAG);
		    		up.setEnabled(false);
		    		up.setSummary(R.string.update_disabled_summary);
		    	}
				SharedPreferences.Editor editor = settings.edit();
		    	editor.putInt("APP_SIGNATURE", Utils.currentHashCode);
		    	if (editor.commit()) Utils.logger("d", "saving sig pref...", DEBUG_TAG);
			} else {
				if (prefSig == YTD_SIG_HASH) {
					Utils.logger("d", "YTD signature in PREFS: update check possile", DEBUG_TAG);
					up.setEnabled(true);
					
					if (settings.getBoolean("autoupdate", false)) {
						Utils.logger("i", "autoupdate enabled", DEBUG_TAG);
						autoUpdate(getActivity());
					}
				} else {
					Utils.logger("d", "diffrent YTD signature in prefs (F-Droid?). Update check cancelled.", DEBUG_TAG);
					up.setEnabled(false);
				}
			}
		}

		private void initSwapPreference() {
			boolean swap = settings.getBoolean("swap_location", false);
			PreferenceScreen p = (PreferenceScreen) findPreference("open_chooser");
            if (swap == true) {
            	p.setEnabled(true);
            } else {
            	p.setEnabled(false);
            }
		}
		
		private void initSizePreference() {
			CheckBoxPreference s = (CheckBoxPreference) findPreference("show_size");
			CheckBoxPreference l = (CheckBoxPreference) findPreference("show_size_list");
            if (l.isChecked()) {
            	s.setEnabled(false);
            	s.setChecked(true);
            } else {
            	s.setEnabled(true);
            }
		}
		
		private void initAudioPreference() {
			boolean ffmpegSupported = settings.getBoolean("FFMPEG_SUPPORTED", true);
			if (ffmpegSupported) {
				String encode = settings.getString("audio_extraction_type", "extr");
				Preference p = (Preference) findPreference("mp3_bitrate");
				if (encode.equals("conv") == true) {
					p.setEnabled(true);
				} else {
					p.setEnabled(false);
				}
			} else {
				touchAudioExtrPref(false, false);
			}
		}
        
		/*@Override
	    public void onStart() {
	        super.onStart();
	        Utils.logger("v", "_onStart");
	    }*/
	    
        @Override
        public void onResume(){
        	super.onResume();
        	// Set up a listener whenever a key changes            
        	getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
        	Utils.logger("v", "_onResume", DEBUG_TAG);
        }
       
        @Override
        public void onPause() {
        	super.onPause();
        	// Unregister the listener whenever a key changes            
        	getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
        	Utils.logger("v", "_onPause", DEBUG_TAG);
        }
        
        /*@Override
        public void onStop() {
            super.onStop();
        	Utils.logger("v", "_onStop");
        }*/
        
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        	updatePrefSummary(findPreference(key));
        	initSwapPreference();
        	initSizePreference();
        	initAudioPreference();
        }

		private void initSummary(Preference p){
        	if (p instanceof PreferenceCategory){
        		PreferenceCategory pCat = (PreferenceCategory) p;
        		for(int i=0;i<pCat.getPreferenceCount();i++){
        			initSummary(pCat.getPreference(i));
        	    }
        	}else{
        		updatePrefSummary(p);
        	}
        }
        
        private void updatePrefSummary(Preference p){
        	if (p instanceof ListPreference) {
        		ListPreference listPref = (ListPreference) p;
        	    p.setSummary(listPref.getEntry());
        	}
        	if (p instanceof PreferenceScreen && p.getKey().equals("open_chooser")) {
        		p.setSummary(chooserSummary);
        	}
        }

        @Override
		public void onActivityResult(int requestCode, int resultCode, Intent data) {
            switch (requestCode) {
            case _ReqChooseFile:
                if (resultCode == RESULT_OK) {
                    @SuppressWarnings("unchecked")
					List<LocalFile> files = (List<LocalFile>) data.getSerializableExtra(FileChooserActivity._Results);
                    	
                	File chooserFolder = files.get(0);
					chooserSummary = chooserFolder.toString();
                	Utils.logger("d", "file-chooser selection: " + chooserSummary, DEBUG_TAG);
                	
                	switch (pathCheck(chooserFolder)) {
                		case 0:
                			// Path on standard sdcard
                			setChooserPrefAndSummary();
	                		break;
                		case 1:
                			// Path not writable
                			chooserSummary = ShareActivity.dir_Downloads.getAbsolutePath();
                			setChooserPrefAndSummary();
                			PopUps.showPopUp(getString(R.string.system_warning_title), getString(R.string.system_warning_msg), "alert", getActivity());
                			//Toast.makeText(getActivity(), getString(R.string.system_warning_title), Toast.LENGTH_SHORT).show();
                			break;
                		case 2:
                			// Path not mounted
                			Toast.makeText(getActivity(), getString(R.string.sdcard_unmounted_warning), Toast.LENGTH_SHORT).show();
                	}
                }
                break;
            }
        }

		public void setChooserPrefAndSummary() {
			for(int i=0;i<getPreferenceScreen().getPreferenceCount();i++){
				initSummary(getPreferenceScreen().getPreference(i));
			}
			settings.edit().putString("CHOOSER_FOLDER", chooserSummary).apply();
		}
        
        public int pathCheck(File path) {
            String state = Environment.getExternalStorageState();
            if (Environment.MEDIA_MOUNTED.equals(state)) {
            	if (path.canWrite()) {
					return 0;
				} else {
					Utils.logger("w", "Path not writable", DEBUG_TAG);
					return 1;
				}
            } else {
            	Utils.logger("w", "Path not mounted", DEBUG_TAG);
            	return 2;
            }
        }
        
        public static void autoUpdate(Context context) {
	        long storedTime = settings.getLong("time", 0); // final string
	        //long storedTime = 10000; // dev test: forces auto update
	        
	        boolean shouldCheckForUpdate = !DateUtils.isToday(storedTime);
	        Utils.logger("i", "shouldCheckForUpdate: " + shouldCheckForUpdate, DEBUG_TAG);
	        if (shouldCheckForUpdate) {
	        	Intent intent = new Intent(context, AutoUpgradeApkService.class);
	        	context.startService(intent);
	        }
	        
	        long time = System.currentTimeMillis();
	        if (settings.edit().putLong("time", time).commit()) Utils.logger("i", "time written in prefs", DEBUG_TAG);
		}

		public static void touchAudioExtrPref(final boolean enable, final boolean check) {
			mActivity.runOnUiThread(new Runnable() {
				public void run() {
					Utils.logger("d", "audio-extraction-checkbox: " + "enabled: " + enable + " / checked: " + check, DEBUG_TAG);
					audio.setEnabled(enable);
					audio.setChecked(check);
			    }
			});
		}
		
		/* Intent createEmailOnlyChooserIntent from Stack Overflow:
		 * 
		 * http://stackoverflow.com/questions/2197741/how-to-send-email-from-my-android-application/12804063#12804063
		 * 
		 * Q: http://stackoverflow.com/users/138030/rakesh
		 * A: http://stackoverflow.com/users/1473663/nobu-games
		 */
		public Intent createEmailOnlyChooserIntent(Intent source, CharSequence chooserTitle) {
			Stack<Intent> intents = new Stack<Intent>();
	        Intent i = new Intent(Intent.ACTION_SENDTO, Uri.fromParts("mailto",
	        		"info@domain.com", null));
	        List<ResolveInfo> activities = getActivity().getPackageManager()
	                .queryIntentActivities(i, 0);

	        for(ResolveInfo ri : activities) {
	            Intent target = new Intent(source);
	            target.setPackage(ri.activityInfo.packageName);
	            intents.add(target);
	        }

	        if(!intents.isEmpty()) {
	            Intent chooserIntent = Intent.createChooser(intents.remove(0),
	                    chooserTitle);
	            chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS,
	                    intents.toArray(new Parcelable[intents.size()]));

	            return chooserIntent;
	        } else {
	        	return Intent.createChooser(source, chooserTitle);
	        }
		}
	}
}
