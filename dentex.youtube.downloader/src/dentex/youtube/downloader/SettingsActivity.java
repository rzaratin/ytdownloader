package dentex.youtube.downloader;

import group.pals.android.lib.ui.filechooser.FileChooserActivity;
import group.pals.android.lib.ui.filechooser.io.localfile.LocalFile;
import group.pals.android.lib.ui.filechooser.services.IFileProvider;

import java.io.File;
import java.util.List;
import java.util.Locale;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.res.Configuration;
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
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;
import dentex.youtube.downloader.service.AutoUpgradeApkService;
import dentex.youtube.downloader.utils.PopUps;
import dentex.youtube.downloader.utils.Utils;

public class SettingsActivity extends Activity {
	
	public static final String DEBUG_TAG = "SettingsActivity";
	private static final int _ReqChooseFile = 0;
	public static String chooserSummary;
    public static SharedPreferences settings = ShareActivity.settings;
	public final String PREFS_NAME = ShareActivity.PREFS_NAME;
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        this.setTitle(R.string.title_activity_settings);
        
    	settings = getSharedPreferences(PREFS_NAME, 0);
        
    	// Theme init
    	Utils.themeInit(this);
    	
        // Language init
        String lang  = settings.getString("lang", "default");
        if (!lang.equals("default")) {
	        Locale locale = new Locale(lang);
	        Locale.setDefault(locale);
	        Configuration config = new Configuration();
	        config.locale = locale;
	        getBaseContext().getResources().updateConfiguration(config, null);
        }
        
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
        	case R.id.menu_about:
        		startActivity(new Intent(this, AboutActivity.class));
        		return true;
        	case R.id.menu_dm:
        		startActivity(new Intent(android.app.DownloadManager.ACTION_VIEW_DOWNLOADS));
        		return true;
        	default:
        		return super.onOptionsItemSelected(item);
        }
    }

	public static class SettingsFragment extends PreferenceFragment implements OnSharedPreferenceChangeListener {
    	
		//private Preference dm;
		private Preference filechooser;
		private Preference quickStart;
		private Preference up;
		private CheckBoxPreference ownNot;
		private Preference th;

		//TODO fix for release
		public static final int YTD_SIG_HASH = -1892118308; // final string
		//public static final int YTD_SIG_HASH = -118685648; // dev test: desktop
		//public static final int YTD_SIG_HASH = 1922021506; // dev test: laptop
		
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            addPreferencesFromResource(R.xml.settings);

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
            
            quickStart = (Preference) getPreferenceScreen().findPreference("quick_start");
            quickStart.setOnPreferenceClickListener(new OnPreferenceClickListener() {
				
				public boolean onPreferenceClick(Preference preference) {
					PopUps.showPopUpInFragment(getString(R.string.quick_start_title), getString(R.string.quick_start_text), "info", SettingsFragment.this);
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
            
            th = (Preference) findPreference("choose_theme");
			th.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
				
				public boolean onPreferenceChange(Preference preference, Object newValue) {
					String theme = settings.getString("choose_theme", "D");
					Activity thisActivity = SettingsFragment.this.getActivity();
			    	if (theme.equals("D")) {
			    		thisActivity.setTheme(R.style.AppThemeDark);
			    	} else {
			    		thisActivity.setTheme(R.style.AppThemeLight);
			    	}
			    	thisActivity.finish();
		    		thisActivity.startActivity(new Intent(thisActivity, SettingsActivity.class));
					return true;
				}
			});
 
			updateInit();
		}

		public void updateInit() {
			int prefSig = settings.getInt("APP_SIGNATURE", 0);
			Log.d(DEBUG_TAG, "prefSig: " + prefSig);
			
			if (prefSig == 0 ) {
				if (Utils.getSigHash(SettingsFragment.this) == YTD_SIG_HASH) {
					Log.d(DEBUG_TAG, "Found YTD signature: update check possile");
					up.setEnabled(true);
					
					if (settings.getBoolean("autoupdate", false)) {
						Log.i(DEBUG_TAG, "autoupdate enabled");
						autoUpdate(getActivity());
					}
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
					Log.d(DEBUG_TAG, "YTD signature in PREFS: update check possile");
					up.setEnabled(true);
					
					if (settings.getBoolean("autoupdate", false)) {
						Log.i(DEBUG_TAG, "autoupdate enabled");
						autoUpdate(getActivity());
					}
				} else {
					Log.d(DEBUG_TAG, "diffrent YTD signature in prefs (F-Droid?). Update check cancelled.");
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
        	initSizePreference();
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
                			// Path on standard sdcard
                			setChooserPrefAndSummary();
	                		break;
                		case 1:
                			// Path not writable
                			chooserSummary = ShareActivity.dir_Downloads.getAbsolutePath();
                			setChooserPrefAndSummary();
                			PopUps.showPopUp(getString(R.string.system_warning_title), getString(R.string.system_warning_msg), "alert", SettingsFragment.this.getActivity());
                			//Toast.makeText(SettingsFragment.this.getActivity(), getString(R.string.system_warning), Toast.LENGTH_SHORT).show();
                			break;
                		case 2:
                			// Path not mounted
                			Toast.makeText(SettingsFragment.this.getActivity(), getString(R.string.sdcard_unmounted_warning), Toast.LENGTH_SHORT).show();
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
					Log.w(DEBUG_TAG, "Path not writable");
					return 1;
				}
            } else {
            	Log.w(DEBUG_TAG, "Path not mounted");
            	return 2;
            }
        }
        
        public static void autoUpdate(Context context) {
        	//TODO fix for release
	        long storedTime = settings.getLong("time", 0); // final string
	        //long storedTime = 10000; // dev test: forces auto update for testing purposes
	        
	        boolean shouldCheckForUpdate = !DateUtils.isToday(storedTime);
	        Log.i(DEBUG_TAG, "shouldCheckForUpdate: " + shouldCheckForUpdate);
	        if (shouldCheckForUpdate) {
	        	Intent intent = new Intent(context, AutoUpgradeApkService.class);
	        	context.startService(intent);
	        }
	        
	        long time = System.currentTimeMillis();
	        if (settings.edit().putLong("time", time).commit()) Log.i(DEBUG_TAG, "time written in prefs");
		}
	}
}