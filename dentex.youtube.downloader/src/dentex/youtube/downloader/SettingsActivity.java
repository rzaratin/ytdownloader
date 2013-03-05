package dentex.youtube.downloader;

import group.pals.android.lib.ui.filechooser.FileChooserActivity;
import group.pals.android.lib.ui.filechooser.io.localfile.LocalFile;
import group.pals.android.lib.ui.filechooser.services.IFileProvider;

import java.io.File;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Parcelable;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.util.Log;
import android.widget.Toast;

import com.stericson.RootTools.RootTools;

import dentex.youtube.downloader.docs.ChangelogActivity;
import dentex.youtube.downloader.docs.CreditsShowActivity;
import dentex.youtube.downloader.docs.GplShowActivity;
import dentex.youtube.downloader.docs.MitShowActivity;
import dentex.youtube.downloader.utils.PopUps;
import dentex.youtube.downloader.utils.Utils;

public class SettingsActivity extends Activity {
	
	private static final int _ReqChooseFile = 0;
	public static String chooserSummary;
    public static SharedPreferences settings = ShareActivity.settings;
	public final String PREFS_NAME = ShareActivity.PREFS_NAME;
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Load default preferences values
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
        
        this.setTitle(R.string.title_activity_settings);
        
    	settings = getSharedPreferences(PREFS_NAME, 0);
        
        // Language init
        String lang  = settings.getString("lang", "default");
        if (!lang.equals("default")) {
	        Locale locale = new Locale(lang);
	        Locale.setDefault(locale);
	        Configuration config = new Configuration();
	        config.locale = locale;
	        getBaseContext().getResources().updateConfiguration(config, null);
        }

        // Display the fragment as the main content.
        getFragmentManager().beginTransaction()
                .replace(android.R.id.content, new SettingsFragment())
                .commit();
    }
    
    public static class SettingsFragment extends PreferenceFragment implements OnSharedPreferenceChangeListener {
    	
    	public static final String EXT_CARD_NAMES = "(extSdCard|sdcard1|emmc|ext_card)";
		private static final String DEBUG_TAG = "SettingsActivity";
		private Preference dm;
		private Preference filechooser;
		private Preference quickStart;
		private Preference gpl;
		private Preference mit;
		private Preference credits;
		private Preference git;
		private Preference hg;
		private Preference gc;
		private Preference share;
		private Preference cl;
		private Preference up;
		private CheckBoxPreference ownNot;
		private Preference loc;
		private static CheckBoxPreference suCp;
		private boolean rooted;
		private String rootTestDone;
		protected boolean pathIsOnExtSdCard = false;
		
		public static final int YTD_SIG_HASH = -1892118308; // final string
		//public static final int YTD_SIG_HASH = -118685648; // dev test desktop
		//public static final int YTD_SIG_HASH = 1922021506; // dev test laptop
		
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            addPreferencesFromResource(R.xml.preferences);
            
            suCp = (CheckBoxPreference) findPreference("su_cp");
            
            initSuCpCheckbox();
            suCp.setOnPreferenceClickListener(new OnPreferenceClickListener() {
				public boolean onPreferenceClick(Preference preference) {
					if (suCp.isChecked()) {
				         initRootTest();
				            
				         if (!rooted) {
				        	 Log.d(DEBUG_TAG, "We don't have root: disabling suCp checkbox");
				        	 suCp.setChecked(false);
				        	 suCp.setEnabled(false);
				         } else {
				        	 Log.d(DEBUG_TAG, "Device is rooted");
				         }
					} else {
						pathIsOnExtSdCard = settings.getBoolean("PATH_ON_EXTSDCARD", false);
						if (!pathIsOnExtSdCard) {
							standardDownloadFolderFallback();
						}
					}
	                return true;
                }
            });
            
            dm = (Preference) findPreference("dm");
            dm.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            	
                public boolean onPreferenceClick(Preference preference) {
                	startActivity(new Intent(android.app.DownloadManager.ACTION_VIEW_DOWNLOADS));
                	suCp.setEnabled(false);
                    return true;
                }
            });

            String cf = settings.getString("CHOOSER_FOLDER", "");
            if (cf.isEmpty()) {
            	chooserSummary = getString(R.string.chooser_location_summary);
            } else {
            	chooserSummary = settings.getString("CHOOSER_FOLDER", "");
            }
            initSwapPreference();
            
            for(int i=0;i<getPreferenceScreen().getPreferenceCount();i++){
                initSummary(getPreferenceScreen().getPreference(i));
            }

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
            
            quickStart = (Preference) getPreferenceScreen().findPreference("quick_start");
            quickStart.setOnPreferenceClickListener(new OnPreferenceClickListener() {
				
				public boolean onPreferenceClick(Preference preference) {
					PopUps.showPopUpInFragment(getString(R.string.quick_start_title), getString(R.string.quick_start_text), "info", SettingsFragment.this);
					return true;
				}
			});
            
            gpl = (Preference) findPreference("gpl");
            gpl.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            	
                public boolean onPreferenceClick(Preference preference) {
                	Intent intent = new Intent(getActivity(),  GplShowActivity.class);
            		startActivity(intent);
                    return true;
                }
            });
            
            mit = (Preference) findPreference("mit");
            mit.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            	
                public boolean onPreferenceClick(Preference preference) {
                	Intent intent = new Intent(getActivity(),  MitShowActivity.class);
            		startActivity(intent);
                    return true;
                }
            });
            
            credits = (Preference) findPreference("credits");
            credits.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            	
                public boolean onPreferenceClick(Preference preference) {
                	Intent intent = new Intent(getActivity(),  CreditsShowActivity.class);
            		startActivity(intent);
                    return true;
                }
            });
            
            git = (Preference) findPreference("ytd_code_git");
            git.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            	
                public boolean onPreferenceClick(Preference preference) {
                	String url = "https://github.com/dentex/ytdownloader/";
                	Intent i = new Intent(Intent.ACTION_VIEW);
                	i.setData(Uri.parse(url));
                	startActivity(i);
                	return true;
                }
            });
            
            hg = (Preference) findPreference("ytd_code_hg");
            hg.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            	
                public boolean onPreferenceClick(Preference preference) {
                	String url = "https://sourceforge.net/projects/ytdownloader/";
                	Intent i = new Intent(Intent.ACTION_VIEW);
                	i.setData(Uri.parse(url));
                	startActivity(i);
                	return true;
                }
            });
            
            gc = (Preference) findPreference("chooser_code");
            gc.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            	
                public boolean onPreferenceClick(Preference preference) {
                	String url = "https://code.google.com/p/android-filechooser/";
                	Intent i = new Intent(Intent.ACTION_VIEW);
                	i.setData(Uri.parse(url));
                	startActivity(i);
                	return true;
                }
            });
            
            share = (Preference) findPreference("share");
            share.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            	
                public boolean onPreferenceClick(Preference preference) {
                    try {
                    	Intent shareIntent =   
                    	new Intent(android.content.Intent.ACTION_SEND);   
                    	shareIntent.setType("text/plain");  
                    	shareIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, "YouTube Downloader");
                    	String shareMessage = getString(R.string.share_message);
                    	shareIntent.putExtra(android.content.Intent.EXTRA_TEXT, shareMessage);
                    	startActivity(Intent.createChooser(shareIntent, "Share this YTD"));
                    } catch (final ActivityNotFoundException e) {
                    	Log.d(DEBUG_TAG, "No suitable Apps found.");
                    	PopUps.showPopUpInFragment(getString(R.string.attention), getString(R.string.share_warning), "alert", SettingsFragment.this);
                    }
                	return true;
                }
            });
            
            loc = (Preference) findPreference("help_translate");
            loc.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            	
                public boolean onPreferenceClick(Preference preference) {
                	String url = "http://www.getlocalization.com/ytdownloader/";
                	Intent i = new Intent(Intent.ACTION_VIEW);
                	i.setData(Uri.parse(url));
                	startActivity(i);
                	return true;
                }
            });
            
            cl = (Preference) findPreference("changelog");
            cl.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            	
                public boolean onPreferenceClick(Preference preference) {
                	Intent intent = new Intent(getActivity(),  ChangelogActivity.class);
                	startActivity(intent);
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
 
			int prefSig = settings.getInt("APP_SIGNATURE", 0);
			Log.d(DEBUG_TAG, "prefSig: " + prefSig);
			
			if (prefSig == 0 ) {
				if (Utils.getSigHash(SettingsFragment.this) == YTD_SIG_HASH) {
					Log.d(DEBUG_TAG, "Found YTD signature: update check possile");
					up.setEnabled(true);
		    	} else {
		    		Log.d(DEBUG_TAG, "Found different signature: " + Utils.currentHashCode + " (F-Droid?). Update check cancelled.");
		    		up.setEnabled(false);
		    		up.setSummary(R.string.update_disabled_summary);
		    	}
				SharedPreferences.Editor editor = settings.edit();
		    	editor.putInt("APP_SIGNATURE", Utils.currentHashCode);
		    	if (editor.commit()) Log.d(DEBUG_TAG, "saving sig pref...");
			} else {
				if (prefSig == YTD_SIG_HASH) {
					Log.d(DEBUG_TAG, "YTD signature in prefs: update check possile");
					up.setEnabled(true);
				} else {
					Log.d(DEBUG_TAG, "diffrent YTD signature in prefs (F-Droid?). Update check cancelled.");
					up.setEnabled(false);
				}
			}			
		}

		public void initSuCpCheckbox() {
			rooted = settings.getBoolean("ROOTED", false);
            rootTestDone = settings.getString("ROOT_TEST_DONE", "");
            if (!rooted && !rootTestDone.isEmpty()) {
            	suCp.setChecked(false);
            	suCp.setEnabled(false);
            }
		}

		private boolean rootTestOk() {
			boolean BB = RootTools.isBusyboxAvailable();
			boolean SU = RootTools.isRootAvailable();
			if (BB && SU) {
				settings.edit().putBoolean("ROOTED", true).apply();
				rooted = true;
				settings.edit().putString("ROOT_TEST_DONE", "done").commit();
				//Toast.makeText(SettingsFragment.this.getActivity(), "Rooted device successfully detected", Toast.LENGTH_SHORT).show();
				return true;
			} else {
				settings.edit().putBoolean("ROOTED", false).apply();
				settings.edit().putString("ROOT_TEST_DONE", "done").commit();
				Toast.makeText(SettingsFragment.this.getActivity(), "Device NOT rooted", Toast.LENGTH_SHORT).show();
				return false;
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
        
		/*@Override
	    public void onStart() {
	        super.onStart();
	        Log.v(DEBUG_TAG, "_onStart");
	    }*/
	    
        @Override
        public void onResume(){
        	super.onResume();
        	// Set up a listener whenever a key changes            
        	getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
        	Log.v(DEBUG_TAG, "_onResume");
        }
       
        @Override
        public void onPause() {
        	super.onPause();
        	// Unregister the listener whenever a key changes            
        	getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
        	Log.v(DEBUG_TAG, "_onPause");
        }
        
        /*@Override
        public void onStop() {
            super.onStop();
        	Log.v(DEBUG_TAG, "_onStop");
        }*/
        
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        	updatePrefSummary(findPreference(key));
        	initSwapPreference();
        }
        	 
        private void initSummary(Preference p){
        	if (p instanceof PreferenceCategory){
        		PreferenceCategory pCat = (PreferenceCategory)p;
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
                	Log.d(DEBUG_TAG, "file-chooser selection: " + chooserSummary);
                	
                	switch (pathCheck(chooserFolder)) {
                		case 0:
                			// standard sdcard
                			setChooserPrefAndSummary();
	                		break;
                		case 1:
                			// ExtSdCard
                			handleExtSdCardPath();
                			setChooserPrefAndSummary();
                			break;
                		case 2:
                			// system path
                			standardDownloadFolderFallback();
                			break;
                		case 3:
                			// sdcard unmounted
                			Toast.makeText(SettingsFragment.this.getActivity(), "Sdcard NOT mounted", Toast.LENGTH_SHORT).show();
                	}
                }
                break;
            }
        }

		public void standardDownloadFolderFallback() {
			chooserSummary = ShareActivity.dir_Downloads.getAbsolutePath();
			setChooserPrefAndSummary();
			pathIsOnExtSdCard = false;
			//Toast.makeText(SettingsFragment.this.getActivity(), "Falling-back on standard Download folder", Toast.LENGTH_SHORT).show();
		}

		public void setChooserPrefAndSummary() {
			for(int i=0;i<getPreferenceScreen().getPreferenceCount();i++){
				initSummary(getPreferenceScreen().getPreference(i));
			}
			settings.edit().putString("CHOOSER_FOLDER", chooserSummary).apply();
		}
        
        private void handleExtSdCardPath() {
        	if (!suCp.isChecked()) {
				AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(SettingsFragment.this.getActivity());
				dialogBuilder.setIcon(android.R.drawable.ic_dialog_info);
				dialogBuilder.setTitle(getString(R.string.path_on_extsdcard_dialog_title));
				dialogBuilder.setMessage(getString(R.string.path_on_extsdcard_dialog_msg));
				
				dialogBuilder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						initRootTest();
						if (rooted) {
							suCp.setChecked(true);
							suCp.setEnabled(true);
							settings.edit().putBoolean("PATH_ON_EXTSDCARD", true);
						}
					}
				});
				
				dialogBuilder.setNegativeButton(getString(R.string.dialogs_negative), new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						standardDownloadFolderFallback();
					}
				});
				
				AlertDialog helpDialog = dialogBuilder.create();
				helpDialog.show();
			}
		}
        
        public void initRootTest() {
			if (rootTestDone.isEmpty()) {
				 Log.d(DEBUG_TAG, "Entering root test");
				 rootTestOk();
			 } else {
				 Log.d(DEBUG_TAG, "Root test already done: skipping");
			 }
		}

		/* Checks if external storage is available for read and write */
        public int pathCheck(File path) {
            String state = Environment.getExternalStorageState();
            if (Environment.MEDIA_MOUNTED.equals(state)) {
            	
            	if (path.canWrite()) {
					Pattern extPattern = Pattern.compile(SettingsFragment.EXT_CARD_NAMES);
					Matcher extMatcher = extPattern.matcher(path.toString());
					if (extMatcher.find()) {
						Log.d(DEBUG_TAG, "Path on ExtSdCard");
						return 1;
					} else {
						Log.d(DEBUG_TAG, "Path on standard sdcard");
						return 0;
					}
				} else {
					Log.d(DEBUG_TAG, "Path not writable");
					return 2;
				}
            } else {
            	Log.d(DEBUG_TAG, "Path not mounted");
            	return 3;
            }
        }
	}
}
